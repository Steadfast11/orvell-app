package uz.xasan.orvell.watch

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import uz.xasan.orvell.data.SettingsRepo
import uz.xasan.orvell.R
import uz.xasan.orvell.data.Store
import uz.xasan.orvell.protocol.wire.ResponseStatus
import java.time.LocalDateTime

enum class Conn { UNPAIRED, BLUETOOTH_OFF, CONNECTING, CONFIRM_ON_WATCH, CONNECTED, DISCONNECTED }

data class WatchState(
    val conn: Conn = Conn.UNPAIRED,
    val battery: Int = -1,
    val charging: Boolean = false,
    /** Oxirgi muvaffaqiyatli tarix sinxroni (ms), 0 = hali bo'lmagan. */
    val historyAt: Long = 0,
    /** Oxirgi sinxronda yangi (avval saqlanmagan) yozuvlar soni. */
    val historyNew: Int = 0,
    val syncing: Boolean = false,
    val error: String? = null,
)

/** Tarix sinxroni natijasi: UI snackbar uchun bir martalik xabar. ok = false bo'lsa [error] to'ldiriladi. */
data class SyncResult(val ok: Boolean, val newRecords: Int, val error: String?, val at: Long)

/** Ulanish va bog'lash uchun kutish (ms). Soat tasdig'ini kutayotganda bu chegara qo'llanmaydi. */
private const val BIND_TIMEOUT_MS = 90_000L

/** Soatning ajratish javobini kutish (ms). */
private const val UNBIND_TIMEOUT_MS = 10_000L

/** Ulangandan keyin birinchi avtomatik sinxron (vaqt yuborilishi va bog'lash tekshiruvi tugasin). */
private const val AUTO_SYNC_FIRST_MS = 3_000L
private const val AUTO_SYNC_MS = 15 * 60_000L

/** Server "hozir yangila" so'rovida tarix sinxronini kutish (katta tarix ~10 s, zaxira bilan). */
private const val PULL_TIMEOUT_MS = 60_000L

/**
 * Soat bilan doimiy ulanish: ulanadi, bog'lanadi, uzilsa qayta ulanadi.
 * Tasdiqlangan protokol (clean-room wire API): ulanish, bog'lash, ajratish, vaqt, tarix sinxroni.
 * Batareya, jonli o'lchash va sozlamalar protokolda tasdiqlanmagan: ularga soatdan so'rov yuborilmaydi.
 */
@SuppressLint("MissingPermission")
object WatchManager {
    private const val TAG = "OrvellWatch"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(WatchState())
    val state: StateFlow<WatchState> = _state
    // Har tarix sinxronining natijasi bir marta chiqadi (holat o'zgarishini kuzatish o'rniga: tez sinxron o'tib ketmasin)
    private val _syncResult = MutableSharedFlow<SyncResult>(extraBufferCapacity = 4)
    val syncResult: SharedFlow<SyncResult> = _syncResult.asSharedFlow()

    private lateinit var appContext: Context
    // Tarix yozuvlari (watch_raw) soat ACK'idan oldin shu yerga yoziladi.
    private lateinit var store: Store
    private lateinit var settings: SettingsRepo
    private var loop: Job? = null
    @Volatile private var session: Session? = null

    fun init(context: Context, store: Store, settings: SettingsRepo) {
        appContext = context.applicationContext
        this.store = store
        this.settings = settings
        // Oxirgi sinxron vaqti saqlangan: ilova qayta ochilganda ko'rsatiladi
        _state.value = WatchState(historyAt = settings.value.watch.lastHistoryAt)
    }

    fun start() {
        if (loop?.isActive == true) return
        // Oldingi sikl to'liq tugagach boshlanadi: ikki seans bir vaqtda ochilib qolmaydi
        val previous = loop
        loop = scope.launch { previous?.join(); runForever() }
    }

    fun stop() {
        loop?.cancel(); loop = null
        session?.close(); session = null
        _state.update { it.copy(conn = if (settings.value.watch.paired) Conn.DISCONNECTED else Conn.UNPAIRED, syncing = false) }
    }

    fun restart() { stop(); start() }

    /**
     * Tarix sinxroni: soat yozuvlarini telefonga saqlaydi, har hisoblagich DB ga yozilgach soatga tasdiq yuboradi.
     * Faqat bog'langan holatda. Bir vaqtda bittasi.
     */
    fun syncNow() {
        val s = session?.takeIf { it.ready } ?: return
        if (_state.value.syncing || _face.value.sending) return
        s.freshRecords = 0
        // Sinxron kimligi: shu sinxron ichidagi takror yozuvlar faqat bitta qator bo'ladi (Store UNIQUE)
        s.syncId = System.currentTimeMillis()
        _state.update { it.copy(syncing = true, error = null) }
        if (!s.client.startHistory()) _state.update { it.copy(syncing = false) }
    }

    /** Siferblat uzatish holati. ok = null: hali natija yo'q. */
    data class FaceState(
        val sending: Boolean = false, val acked: Int = 0, val total: Int = 0,
        val ok: Boolean? = null, val error: String? = null, val finishedAt: Long = 0,
    )

    private val _face = MutableStateFlow(FaceState())
    val face: StateFlow<FaceState> = _face

    /** Siferblat xizmati shu soatda bormi (ulanish tayyor bo'lsa). */
    val faceSupported: Boolean get() = session?.takeIf { it.ready }?.client?.faceSupported == true

    /** Siferblat faylini yuboradi. False: soat ulanmagan, sinxron ketyapti yoki boshqa uzatish bor. */
    fun uploadFace(file: ByteArray): Boolean {
        val s = session?.takeIf { it.ready } ?: return false
        if (_state.value.syncing || _face.value.sending) return false
        _face.value = FaceState(sending = true, total = file.size)
        if (!s.client.startFace(file)) { _face.value = FaceState(); return false }
        return true
    }

    /**
     * Server so'rovi ("hozir yangila") uchun soat tarixini o'qiydi: sinxron ketayotgan bo'lsa uning natijasi kutiladi.
     * null: soat ulanmagan, siferblat yuborilmoqda yoki PULL_TIMEOUT_MS ichida natija kelmadi.
     */
    suspend fun pullNow(): SyncResult? {
        if (session?.ready != true || _face.value.sending) return null
        return withTimeoutOrNull(PULL_TIMEOUT_MS) {
            coroutineScope {
                // Natija oqimiga avval obuna bo'linadi: tez tugagan sinxron o'tkazib yuborilmasin
                val result = async(start = CoroutineStart.UNDISPATCHED) { syncResult.first() }
                if (!_state.value.syncing) syncNow()
                if (!_state.value.syncing) { result.cancel(); null } else result.await()
            }
        }
    }

    /**
     * Ajratish. Soat ulangan bo'lsa avval soatga ajratish so'rovi yuboriladi (0x0015 -> 0x0016 = 00).
     * Muvaffaqiyatli bo'lsa, yoki soat ulanmagan bo'lsa, telefondagi juftlik o'chadi (true).
     * Soat javob bermasa yoki rad etsa, juftlik qoladi va xato ko'rsatiladi (false).
     */
    suspend fun unpair(): Boolean {
        val s = session?.takeIf { it.ready }
        if (s != null) {
            val status = s.unbind()
            if (status != ResponseStatus.Success) {
                val err = if (status == null) appContext.getString(R.string.err_unbind_nores)
                else appContext.getString(R.string.err_unbind_rejected, status.raw)
                _state.update { it.copy(error = err) }
                return false
            }
        }
        stop()
        settings.update { it.copy(watch = it.watch.copy(address = null, name = null, firmware = null)) }
        _state.value = WatchState(conn = Conn.UNPAIRED, historyAt = _state.value.historyAt)
        return true
    }

    fun pair(address: String, name: String?) {
        stop()
        settings.update { it.copy(watch = it.watch.copy(address = address, name = name)) }
        _state.value = WatchState(conn = Conn.CONNECTING, historyAt = _state.value.historyAt)
        start()
    }

    private fun bluetoothOn(): Boolean =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true

    private suspend fun runForever() {
        var backoff = 5_000L
        while (scope.isActive) {
            val cfg = settings.value.watch
            if (!cfg.paired) {
                _state.value = WatchState(conn = Conn.UNPAIRED, historyAt = _state.value.historyAt)
                settings.flow.first { it.watch.paired }
                continue
            }
            if (!bluetoothOn()) { _state.update { it.copy(conn = Conn.BLUETOOTH_OFF) }; delay(5_000); continue }
            val s = Session(appContext, cfg.address!!, cfg.bindId)
            session = s
            try {
                _state.update { it.copy(conn = Conn.CONNECTING, error = null) }
                s.open()
                if (awaitBound(s)) {
                    backoff = 5_000L
                    _state.update { it.copy(conn = Conn.CONNECTED, error = null) }
                    // Ulangach va keyin har AUTO_SYNC_MS da tarix o'zi o'qiladi (tugmadagi bilan bir xil sinxron)
                    val auto = scope.launch {
                        delay(AUTO_SYNC_FIRST_MS)
                        while (isActive && !s.closed) { syncNow(); delay(AUTO_SYNC_MS) }
                    }
                    try { s.waitClosed() } finally { auto.cancel() }
                } else if (s.failure != null) {
                    _state.update { it.copy(error = s.failure) }
                }
            } finally {
                // Sikl bekor qilinsa ham (stop/restart) GATT yopiladi; yangi seansni o'chirib yubormaydi
                s.close()
                if (session === s) session = null
            }
            _state.update { it.copy(conn = Conn.DISCONNECTED, syncing = false) }
            delay(backoff)
            // Qayta urinish oralig'i 15 s dan oshmaydi: soat qaytib yaqinlashganda ulanish uzoq kutmasin
            backoff = minOf(backoff * 2, 15_000L)
        }
    }

    /**
     * Bog'lanishni kutadi. Odatda BIND_TIMEOUT_MS ichida tugamasa uziladi. Soat tasdig'ini kutayotgan bo'lsa
     * (foydalanuvchi soat ekranida tasdiqlashi kerak) vaqt chegarasi qo'llanmaydi: tasdiq yoki uzilishgacha kutiladi.
     */
    private suspend fun awaitBound(s: Session): Boolean {
        var ok = s.awaitReady(BIND_TIMEOUT_MS)
        while (!ok && s.confirming && s.failure == null && !s.closed) ok = s.awaitReady(BIND_TIMEOUT_MS)
        return ok
    }

    // ---------------------------------------------------------------- seans

    private class Session(ctx: Context, address: String, account: String) : BleClient.Listener {
        val client = BleClient(ctx, address, account)
        @Volatile var ready = false
        @Volatile var confirming = false
        @Volatile var failure: String? = null
        /** Joriy tarix sinxronida yangi (avval saqlanmagan) yozuvlar soni. */
        @Volatile var freshRecords = 0
        /** Joriy tarix sinxronining kimligi (boshlangan vaqt, ms). */
        @Volatile var syncId = 0L
        private val readyD = CompletableDeferred<Boolean>()
        private val closedD = CompletableDeferred<Unit>()
        @Volatile private var unbindD: CompletableDeferred<ResponseStatus>? = null

        val closed: Boolean get() = closedD.isCompleted

        fun open() { client.listener = this; client.connect() }
        suspend fun awaitReady(ms: Long): Boolean = withTimeoutOrNull(ms) { readyD.await() } ?: false
        suspend fun waitClosed() = closedD.await()

        fun close() {
            ready = false
            confirming = false
            client.close()
            readyD.complete(false)
            closedD.complete(Unit)
        }

        /** Soatga ajratish so'raydi. Javob kodini qaytaradi; javob kelmasa yoki yuborilmasa null. */
        suspend fun unbind(): ResponseStatus? {
            val d = CompletableDeferred<ResponseStatus>()
            unbindD = d
            if (!client.sendUnbind()) return null
            return withTimeoutOrNull(UNBIND_TIMEOUT_MS) { d.await() }
        }

        override fun onState(state: String) { Log.i(TAG, state) }

        override fun onConfirmOnWatch() {
            confirming = true
            _state.update { it.copy(conn = Conn.CONFIRM_ON_WATCH) }
        }

        override fun onBound() {
            confirming = false
            ready = true
            // Telefon vaqti har bog'lanishda bir marta yuboriladi. Soat javob bermaydi, kutilmaydi.
            if (client.sendTime(LocalDateTime.now())) Log.i(TAG, "vaqt yuborildi") else Log.w(TAG, "vaqt yuborilmadi")
            // Joriy siferblat: rasmli siferblat bo'lsa uning sozlamasi yuboriladi (FitCloudPro shunday qiladi)
            client.queryDial()
            readyD.complete(true)
        }

        override fun onUnbindResult(status: ResponseStatus) { unbindD?.complete(status) }

        override fun onError(message: String) {
            Log.w(TAG, message)
            failure = message
            confirming = false
            readyD.complete(false)
        }

        override fun onHistoryRecord(counter: Int, payload: ByteArray, part: Int, complete: Boolean): Boolean {
            // Xom bo'lak va (yozuv to'lganda) talqin qilingan qadam/puls bitta tranzaksiyada. Xato bo'lsa False: ACK yuborilmaydi, soat yozuvni o'zida saqlaydi.
            return try {
                if (store.insertWatchRecord(counter, payload, System.currentTimeMillis(), syncId, part, complete)) freshRecords++
                true
            } catch (e: Exception) {
                Log.w(TAG, "tarix yozuvi saqlanmadi: ${e.javaClass.simpleName}")
                false
            }
        }

        override fun onHistoryDone(ok: Boolean, message: String?) {
            Log.i(TAG, if (ok) "tarix: yangi yozuv $freshRecords" else "tarix to'xtadi: ${message ?: "-"}")
            // Xato bo'lsa ham saqlangan qismning soni ko'rinsin
            store.notifyChanged()
            if (ok) {
                // Oxirgi muvaffaqiyatli sinxron vaqti saqlanadi: ilova qayta ochilganda "sinxron qilinmagan" chiqmasin
                val now = System.currentTimeMillis()
                settings.update { it.copy(watch = it.watch.copy(lastHistoryAt = now)) }
                _state.update { it.copy(syncing = false, historyAt = now, historyNew = freshRecords, error = null) }
            } else {
                _state.update { it.copy(syncing = false, error = message) }
            }
            _syncResult.tryEmit(SyncResult(ok, freshRecords, message, System.currentTimeMillis()))
        }

        override fun onFaceProgress(acked: Int, total: Int) {
            _face.update { it.copy(sending = acked < total, acked = acked, total = total) }
        }

        override fun onFaceDone(ok: Boolean, message: String?) {
            Log.i(TAG, if (ok) "siferblat qabul qilindi" else "siferblat yuborilmadi: ${message ?: "-"}")
            _face.update { it.copy(sending = false, ok = ok, error = if (ok) null else message, finishedAt = System.currentTimeMillis()) }
        }

        override fun onDisconnected(reason: String) {
            Log.i(TAG, "uzildi: $reason")
            ready = false
            confirming = false
            readyD.complete(false)
            closedD.complete(Unit)
        }
    }
}

// ================================================================ qidiruv

data class FoundWatch(val address: String, val name: String, val rssi: Int, val fitCloud: Boolean)

/** Yaqin atrofdagi BLE qurilmalarini qidiradi. Soat xizmatini e'lon qilganlar ro'yxat boshida. */
@SuppressLint("MissingPermission")
class WatchScanner(private val context: Context) {
    private val _found = MutableStateFlow<List<FoundWatch>>(emptyList())
    val found: StateFlow<List<FoundWatch>> = _found
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning
    private val map = LinkedHashMap<String, FoundWatch>()

    private val cb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull() ?: return
            if (name.isBlank()) return
            val fc = result.scanRecord?.serviceUuids?.any { it.uuid == WATCH_SERVICE_UUID } == true
            map[result.device.address] = FoundWatch(result.device.address, name, result.rssi, fc)
            _found.value = map.values.sortedWith(compareByDescending<FoundWatch> { it.fitCloud }.thenByDescending { it.rssi })
        }
        override fun onScanFailed(errorCode: Int) { _scanning.value = false }
    }

    fun start(): Boolean {
        val scanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bluetoothLeScanner ?: return false
        map.clear(); _found.value = emptyList()
        return runCatching {
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
            _scanning.value = true
        }.isSuccess
    }

    fun stop() {
        runCatching { (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bluetoothLeScanner?.stopScan(cb) }
        _scanning.value = false
    }
}
