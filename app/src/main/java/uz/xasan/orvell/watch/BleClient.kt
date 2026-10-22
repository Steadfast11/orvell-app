package uz.xasan.orvell.watch

import uz.xasan.orvell.R
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import uz.xasan.orvell.protocol.wire.BindMessages
import uz.xasan.orvell.protocol.wire.Frame
import uz.xasan.orvell.protocol.wire.FrameCodec
import uz.xasan.orvell.protocol.wire.FaceMessages
import uz.xasan.orvell.protocol.wire.FrameReader
import uz.xasan.orvell.protocol.wire.HistoryMessages
import uz.xasan.orvell.protocol.wire.KIND_DATA
import uz.xasan.orvell.protocol.wire.ResponseStatus
import uz.xasan.orvell.protocol.wire.TimeMessages
import uz.xasan.orvell.protocol.wire.Tlv
import uz.xasan.orvell.protocol.wire.TlvCodec
import uz.xasan.orvell.protocol.wire.WireFormatException
import java.time.LocalDateTime
import java.util.UUID

/** Soat GATT xizmati (spec: open-questions.md §1). */
internal val WATCH_SERVICE_UUID: UUID = UUID.fromString("000001ff-3c17-d293-8e48-14fe2e4da212")

/** Yozish kanali (ATT handle 0x0012). */
private val WRITE_UUID: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")

/** Bildirishnoma kanali (ATT handle 0x0014). */
private val NOTIFY_UUID: UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")

/** Bildirishnoma yoqish deskriptori (CCCD, handle 0x0015). */
private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/** Siferblat fayl xizmati (spec watchface.md §1): ma'lumot (handle 0x002F) va boshqaruv (0x0031). */
private val FACE_SERVICE_UUID: UUID = UUID.fromString("00006287-3c17-d293-8e48-14fe2e4da212")
private val FACE_DATA_UUID: UUID = UUID.fromString("00006387-3c17-d293-8e48-14fe2e4da212")
private val FACE_CONTROL_UUID: UUID = UUID.fromString("00006487-3c17-d293-8e48-14fe2e4da212")

/** Siferblat uzatishida har bosqich javobini kutish (ms). */
private const val FACE_STEP_TIMEOUT_MS = 15_000L

/** 01/0002 tayyorlash javobini kutish (kuzatilgan: ~0,2 s). */
private const val FACE_PREPARE_TIMEOUT_MS = 3_000L

/** Stek yozuvni rad etsa (GATT band) qayta urinishlar soni va oralig'i. */
private const val WRITE_RETRIES = 50
private const val WRITE_RETRY_MS = 20L

/** Soat so'raydigan MTU (kuzatilgan: 247). ATT yozuvi MTU-3 = 244 baytgacha. */
private const val MTU_REQUEST = 247

/** Tekshiruv (0x0013) yoki bog'lash (0x0011) javobsiz qolganda keyingi urinish oralig'i (ms) [E]. */
private const val CONFIRM_RETRY_MS = 15_000L

/** Tarix sinxronining har bosqichida javob kutish (ms). Javob kelmasa sinxron to'xtaydi, ulanish qoladi. */
private const val HISTORY_STEP_TIMEOUT_MS = 10_000L

/**
 * Bog'lash oqimi. Sof mantiq: Android'siz, JVM testida tekshiriladi.
 * Avval 0x0013 tekshiruvi: soat hisobni tanisa 00 (tasdiqsiz). 01 bo'lsa soat ekranida tasdiq kutiladi:
 * shu paytda 0x0011 BIR MARTA yuboriladi va foydalanuvchi ✓ bossa 0x0012 = 00 keladi (yoki 0x0013 = 00).
 * Tasdiq kutilayotganda 0x0011 qayta yuborilmaydi: har yuborish soatda yangi ✓ so'roviga sabab bo'lishi mumkin.
 * Uning o'rniga 0x0013 bilan davriy tekshiriladi. Tekshiruv javobsiz qolsa, shundagina 0x0011 yuboriladi.
 *  IDLE --start()--> CHECK_WAIT (0x0013 yuboriladi)
 *  CHECK_WAIT --0x0014 = 00--> BOUND (tasdiqsiz)
 *  CHECK_WAIT --0x0014 = 01--> CONFIRM_ON_WATCH (0x0011 bir marta yuboriladi)
 *  CHECK_WAIT --0x0014 boshqa yoki tick--> LOGIN_WAIT (0x0011 yuboriladi)
 *  LOGIN_WAIT --tick--> CONFIRM_ON_WATCH (0x0013 yuboriladi) [E]
 *  LOGIN_WAIT, CONFIRM_ON_WATCH: 0x0012 = 00 -> BOUND; boshqa kod -> FAILED
 *  CONFIRM_ON_WATCH --0x0014 = 00--> BOUND; 0x0014 boshqa--> kutish davom etadi
 *  CONFIRM_ON_WATCH --tick--> oxirgi 0x0013 javobsiz bo'lsa 0x0011, aks holda 0x0013
 *  IDLE, BOUND, FAILED: javob va tick e'tiborsiz
 * Spec messages.md: 0x0013 -> 0x0014 (tasdiqlangan juftlik). 00 = tanilgan hisob [T], 01 = tasdiq kutilmoqda [E].
 */
internal class BindFlow {
    enum class Phase { IDLE, CHECK_WAIT, LOGIN_WAIT, CONFIRM_ON_WATCH, BOUND, FAILED }

    /** Javobdan yoki tickdan keyin bajariladigan harakat. */
    sealed class Next {
        /** Hech narsa qilinmaydi (kutilmagan javob yoki faza mos emas). */
        object None : Next()

        /** 0x0013 tekshiruv so'rovini yuborish (hisob satri bilan, BleClient'da). */
        object SendCheck : Next()

        /** 0x0011 bog'lash so'rovini yuborish (hisob satri bilan, BleClient'da). */
        object SendBind : Next()

        /** Bog'lash muvaffaqiyatli. Chaqiruvchi tickni bekor qiladi. */
        object Bound : Next()

        /** Noma'lum javob kodi: xom bayt, ma'nosi berilmaydi. Chaqiruvchi tickni bekor qiladi. */
        data class Failed(val raw: Int) : Next()
    }

    var phase: Phase = Phase.IDLE
        private set

    /** Oxirgi 0x0013 tekshiruviga javob kelmagan. Keyingi tick shunda 0x0011 yuboradi (tekshiruv ishlamayotgan bo'lsa). */
    private var checkPending = false

    /** Bog'lash boshlanadi: avval tekshiruv (0x0013). Faqat IDLE dan; takror chaqiruv e'tiborsiz. */
    fun start(): Next =
        if (phase == Phase.IDLE) {
            phase = Phase.CHECK_WAIT
            Next.SendCheck
        } else {
            Next.None
        }

    /**
     * 0x0014 tekshiruv javobi. CHECK_WAIT da bog'lash yo'nalishini hal qiladi: 00 tasdiqsiz bog'laydi, 01 tasdiqni kutadi.
     * CONFIRM_ON_WATCH da davriy tekshiruv javobi: 00 = foydalanuvchi ✓ bosgan, bog'lanadi; boshqasi kutishda qoladi.
     */
    fun onConfirmResponse(status: ResponseStatus): Next {
        // Har qanday javob keladi: tekshiruv ishlayapti, keyingi tick 0x0011 emas, 0x0013 yuboradi
        checkPending = false
        return when (phase) {
            Phase.CHECK_WAIT -> when {
                status == ResponseStatus.Success -> {
                    phase = Phase.BOUND
                    Next.Bound
                }
                status.raw == 0x01 -> {
                    // Soat ekranida tasdiq kutilmoqda: 0x0011 bir marta yuboriladi, keyin tekshiruv davom etadi
                    phase = Phase.CONFIRM_ON_WATCH
                    Next.SendBind
                }
                else -> {
                    phase = Phase.LOGIN_WAIT
                    Next.SendBind
                }
            }
            Phase.CONFIRM_ON_WATCH ->
                if (status == ResponseStatus.Success) {
                    phase = Phase.BOUND
                    Next.Bound
                } else {
                    Next.None
                }
            else -> Next.None
        }
    }

    /** 0x0012 javobi. LOGIN_WAIT, CONFIRM_ON_WATCH da hal qiladi; IDLE, BOUND, FAILED da e'tiborsiz. */
    fun onBindResponse(status: ResponseStatus): Next = when {
        phase != Phase.LOGIN_WAIT && phase != Phase.CONFIRM_ON_WATCH -> Next.None
        status == ResponseStatus.Success -> {
            phase = Phase.BOUND
            Next.Bound
        }
        else -> {
            phase = Phase.FAILED
            Next.Failed(status.raw)
        }
    }

    /**
     * CONFIRM_RETRY_MS (15 s) da, javob kelmasa, chaqiriladi.
     * CHECK_WAIT: tekshiruv javobsiz, 0x0011 ga o'tiladi. LOGIN_WAIT: CONFIRM_ON_WATCH ga o'tkaziladi, endi 0x0013 bilan tekshiriladi [E].
     * CONFIRM_ON_WATCH: oxirgi 0x0013 javobsiz qolgan bo'lsa (tekshiruv ishlamayapti) 0x0011 yuboriladi, aks holda 0x0013.
     */
    fun onTick(): Next = when (phase) {
        Phase.CHECK_WAIT -> {
            phase = Phase.LOGIN_WAIT
            Next.SendBind
        }
        Phase.LOGIN_WAIT -> {
            // [E] Soat ekranida tasdiq so'rovi chiqadi deb olinadi: 0x0011 qayta yuborilmaydi, tekshiruv davom etadi
            phase = Phase.CONFIRM_ON_WATCH
            checkPending = true
            Next.SendCheck
        }
        Phase.CONFIRM_ON_WATCH ->
            if (checkPending) {
                checkPending = false
                Next.SendBind
            } else {
                checkPending = true
                Next.SendCheck
            }
        else -> Next.None
    }

    /** Ulanish uzilganda yoki yopilganda: keyingi ulanish oqim boshidan boshlanadi. */
    fun reset() {
        phase = Phase.IDLE
        checkPending = false
    }
}

/**
 * Soat bilan BLE aloqa: GATT ulanish, kanal sozlash, frame o'qish, ACK, bog'lash va ajratish.
 * Protokol: clean-room wire API (protocol.wire). Tasdiqlanmagan funksiyalar bu yerda yo'q.
 * Bir vaqtda faqat bitta ulanish: FitCloudPro ishlayotgan bo'lsa, soat band bo'lishi mumkin.
 */
@SuppressLint("MissingPermission")
class BleClient(private val context: Context, private val address: String, private val account: String) {
    interface Listener {
        fun onState(state: String)

        /** Soat ekranida tasdiq kutilmoqda [E]: foydalanuvchi ✓ bosishi kerak. Holat 0x0013 bilan davriy tekshiriladi. */
        fun onConfirmOnWatch()

        /** Bog'lash muvaffaqiyatli: ulanish tayyor. */
        fun onBound()

        /** Ajratish javobi (0x0016). */
        fun onUnbindResult(status: ResponseStatus)

        /** Xato: foydalanuvchiga ko'rinadi. Ulanishni yopish chaqiruvchining ishi. */
        fun onError(message: String)

        /** Aloqa uzildi. */
        fun onDisconnected(reason: String)

        /**
         * Tarix yozuvi bo'lagi (TYPE 0x05, 0x0030): DB ga yoziladi. part: hisoblagich ichidagi tartib,
         * complete: hisoblagich yozuvi to'ldi. True: ACK yuboriladi. False: ACK yo'q.
         */
        fun onHistoryRecord(counter: Int, payload: ByteArray, part: Int, complete: Boolean): Boolean

        /** Tarix sinxroni tugadi. ok = true: hamma yozuv saqlangan, soatga 0x0020 va 0x0021 yuborilgan. */
        fun onHistoryDone(ok: Boolean, message: String?)

        /** Siferblat uzatish: soat tasdiqlagan bayt soni. */
        fun onFaceProgress(acked: Int, total: Int) {}

        /** Siferblat uzatish tugadi. ok = true: soat faylni tekshirib qabul qildi. */
        fun onFaceDone(ok: Boolean, message: String?) {}
    }

    /** Yozish navbatidagi GATT amali (Android bir vaqtda bitta amalga ruxsat beradi). */
    private sealed class Op {
        class Write(val ch: BluetoothGattCharacteristic, val bytes: ByteArray, val type: Int) : Op()
        class EnableNotify(val ch: BluetoothGattCharacteristic) : Op()
    }

    /** Tushunilgan xabar (ACK allaqachon navbatda). 0x0014 bu yerda yo'q: e'tiborsiz, ACK yuborilmaydi. */
    private sealed class Reply {
        /** 0x0012: bog'lash javobi, holatni BindFlow o'zgartiradi. */
        class Bind(val status: ResponseStatus) : Reply()

        /** 0x0016: ajratish javobi. */
        class Unbind(val status: ResponseStatus) : Reply()

        /** 0x0014: tekshiruv javobi (0x0013). 00 = tanilgan hisob, 01 = soat ekranida tasdiq kutilmoqda. */
        class Confirm(val status: ResponseStatus) : Reply()

        /** 0x0006, 0x0007, 0x0008: tarix soni (ACK allaqachon navbatda, flow holatni o'zgartiradi). */
        class HistoryCount(val id: Int, val count: Int) : Reply()

        /** 0x0030: tarix yozuvi. ACK faqat DB ga yozilgandan keyin (receiveHistoryRecord). */
        class HistoryRecord(val payload: ByteArray) : Reply()

        /** 01/0002: siferblat uzatishiga tayyor (00 00). */
        class FacePrepared(val ok: Boolean) : Reply()

        /** 02/0053: joriy siferblat ID'si. */
        class CurrentDial(val id: Int) : Reply()
    }

    private val tag = "OrvellWatch"
    private val main = Handler(Looper.getMainLooper())
    private val reader = FrameReader()
    private val bindFlow = BindFlow()
    private val history = HistoryFlow()
    private val writeQueue = ArrayDeque<Op>()
    private var face: uz.xasan.orvell.protocol.wire.FaceUpload? = null
    @Volatile private var faceData: BluetoothGattCharacteristic? = null
    @Volatile private var faceControl: BluetoothGattCharacteristic? = null
    private var faceNotifyOn = false
    private var rejects = 0
    private val faceTimeout = Runnable { synchronized(this) { if (!closed) face?.let { runFace(it.onTimeout()) } } }
    private var facePreparing = false
    private val facePrepareTimeout = Runnable { synchronized(this) { if (!closed) facePrepared(null) } }
    private val retryPump = Runnable { synchronized(this) { if (!closed) pump() } }
    /** CONFIRM_RETRY_MS dan keyin: BindFlow tick (javob bo'lmasa 0x0011 qayta yuboriladi). */
    private val retryBind = Runnable { synchronized(this) { if (!closed) execute(bindFlow.onTick()) } }
    /** HISTORY_STEP_TIMEOUT_MS dan keyin: tarix bosqichi javob bermadi. */
    private val historyTimeout = Runnable { synchronized(this) { if (!closed) runHistory(history.onTimeout()) } }
    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var writeChar: BluetoothGattCharacteristic? = null
    @Volatile private var notifyChar: BluetoothGattCharacteristic? = null
    @Volatile private var mtu = 23
    /** Bu seansda bildirishnoma (CCCD) yozilganmi. Seans har ulanishda yangi BleClient bilan boshlanadi. */
    private var notificationsRequested = false
    @Volatile private var closed = false
    private var writing = false
    private var seq = 0
    var listener: Listener? = null

    fun connect() {
        // Hisob satri noto'g'ri bo'lsa (ASCII emas yoki 32 bayt'dan uzun) ulanmaymiz
        if (runCatching { BindMessages.bindRequest(account) }.isFailure) {
            listener?.onError(context.getString(R.string.err_account))
            return
        }
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter = mgr.adapter ?: run { listener?.onDisconnected(context.getString(R.string.err_no_bluetooth)); return }
        listener?.onState("ulanmoqda")
        // Android 12+ da BLUETOOTH_CONNECT ruxsati yo'q (SecurityException) yoki manzil noto'g'ri bo'lsa ilova yiqilmasin:
        // seans uziladi, WatchManager keyinroq qayta urinadi. Xabarda manzil yo'q (faqat istisno turi).
        val g = try {
            adapter.getRemoteDevice(address).connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            Log.w(tag, "ulanish boshlanmadi: ${e.javaClass.simpleName}")
            listener?.onDisconnected(context.getString(R.string.err_connect_failed))
            return
        }
        // connectGatt null qaytarsa 90 soniya kutmasdan darhol qayta urinish boshlanadi
        if (g == null) { listener?.onDisconnected(context.getString(R.string.err_connect_failed)); return }
        // close() shu orada chaqirilgan bo'lsa yangi GATT ochiq qolmasin
        synchronized(this) { if (!closed) { gatt = g; return } }
        try { g?.close() } catch (_: Exception) {}
    }

    fun close() {
        // gatt qulf ostida olinadi: connect() bilan bir vaqtda chaqirilsa ham ulanish yopilmay qolmaydi
        val g = synchronized(this) {
            closed = true
            resetLink()
            gatt.also { gatt = null }
        }
        try { g?.disconnect(); g?.close() } catch (_: Exception) {}
    }

    /** Ajratish so'rovi (0x0015): faqat bog'langan holatda. Javob onUnbindResult orqali keladi. */
    @Synchronized fun sendUnbind(): Boolean {
        if (closed || bindFlow.phase != BindFlow.Phase.BOUND) return false
        return sendData(BindMessages.unbindRequest())
    }

    /**
     * Vaqt yozuvi (0x0001): telefon mahalliy vaqti. Faqat bog'langan holatda; soat javob bermaydi, kutilmaydi.
     * False: ulanish yo'q, yil 2000..2063 dan tashqari yoki frame sig'madi. Chaqiruvchi log yozadi, ilova davom etadi.
     */
    @Synchronized fun sendTime(dt: LocalDateTime): Boolean {
        if (closed || bindFlow.phase != BindFlow.Phase.BOUND) return false
        val payload = runCatching { TimeMessages.setTime(dt) }.getOrNull() ?: return false
        return sendData(payload)
    }

    private val cb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (closed) return
            Log.i(tag, "holat status=$status yangi=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                listener?.onState("ulandi, xizmatlar qidirilmoqda")
                // Qidiruv boshlanmasa (GATT band yoki uzilgan): seans uziladi, WatchManager qayta urinadi
                if (!g.discoverServices()) listener?.onDisconnected(context.getString(R.string.err_discover))
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                resetLink()
                listener?.onDisconnected("uzildi (status=$status)")
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (closed) return
            if (status != BluetoothGatt.GATT_SUCCESS) { listener?.onError(context.getString(R.string.err_services_status, status)); return }
            // GATT xaritasi (handle = instanceId): spec uchun, shaxsiy ma'lumot yo'q
            for (s in g.services) for (c in s.characteristics) {
                Log.i(tag, "gatt svc=%s chr=%s handle=0x%04X prop=0x%02X".format(s.uuid, c.uuid, c.instanceId, c.properties))
            }
            val svc = g.getService(WATCH_SERVICE_UUID)
            if (svc == null) { listener?.onError(context.getString(R.string.err_service_missing)); return }
            val w = svc.getCharacteristic(WRITE_UUID)
            val n = svc.getCharacteristic(NOTIFY_UUID)
            if (w == null || n == null) { listener?.onError(context.getString(R.string.err_channels_missing)); return }
            writeChar = w
            notifyChar = n
            // Siferblat xizmati bo'lmasa ham asosiy aloqa ishlayveradi (faqat siferblat yuborilmaydi)
            g.getService(FACE_SERVICE_UUID)?.let { fs ->
                faceData = fs.getCharacteristic(FACE_DATA_UUID)
                faceControl = fs.getCharacteristic(FACE_CONTROL_UUID)
            }
            // MTU so'raladi (so'rov boshlanmasa qayta uriniladi)
            requestMtuOrEnable(g, 0)
        }

        override fun onMtuChanged(g: BluetoothGatt, newMtu: Int, status: Int) {
            if (closed) return
            mtu = newMtu
            // Katta frame payload'i shu hajmdagi bo'laklarga bo'linib keladi
            if (status == BluetoothGatt.GATT_SUCCESS) synchronized(this@BleClient) { reader.chunkSize = newMtu - 3 }
            Log.i(tag, "MTU=$newMtu status=$status")
            enableNotifications(g)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (closed) return
            if (d.characteristic.uuid == FACE_CONTROL_UUID) { faceNotifyDone(status); return }
            if (status != BluetoothGatt.GATT_SUCCESS) { listener?.onError(context.getString(R.string.err_notify_status, status)); return }
            listener?.onState("kanal tayyor, bog'lash so'ralmoqda")
            startBind()
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (closed) return
            writeDone(status)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            if (c.uuid == FACE_CONTROL_UUID) onFaceControl(value) else onNotify(value)
        }

        @Deprecated("API < 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= 33) return
            @Suppress("DEPRECATION") val v = c.value ?: return
            if (c.uuid == FACE_CONTROL_UUID) onFaceControl(v) else onNotify(v)
        }
    }

    /**
     * MTU so'rovi boshlanmasa 300 ms dan keyin qayta uriniladi (3 marta). Hammasi rad etilsa ham bildirishnoma
     * yoqiladi: aks holda bog'lash so'rovi (MTU 23 da sig'maydi) qayta-qayta uziladi.
     */
    private fun requestMtuOrEnable(g: BluetoothGatt, attempt: Int) {
        if (g.requestMtu(MTU_REQUEST)) return
        if (attempt < 3) {
            main.postDelayed({ synchronized(this) { if (!closed) requestMtuOrEnable(g, attempt + 1) } }, 300L)
        } else {
            enableNotifications(g)
        }
    }

    /** CCCD = 0x0100 (bildirishnoma yoqish). Bog'lash so'rovi shu yozuv tugagach (onDescriptorWrite) yuboriladi. */
    private fun enableNotifications(g: BluetoothGatt) {
        // Bir seansda bir marta: MTU callback'i ikkinchi marta kelsa CCCD qayta yozilmaydi (sog'lom ulanish uzilmaydi)
        if (notificationsRequested) return
        notificationsRequested = true
        val n = notifyChar ?: return
        g.setCharacteristicNotification(n, true)
        val d = n.getDescriptor(CCCD_UUID)
        if (d == null) { listener?.onError(context.getString(R.string.err_descriptor_missing)); return }
        val ok = if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION") run { d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; g.writeDescriptor(d) }
        }
        if (!ok) listener?.onError(context.getString(R.string.err_notify_write))
    }

    /** Soatdan bildirishnoma: frame yig'iladi. Tushunilgan ma'lumotga ACK darhol, keyin holat o'zgaradi. */
    @Synchronized private fun onNotify(value: ByteArray) {
        if (closed) return
        val frame: Frame = try {
            reader.feed(value) ?: return // sarlavha saqlandi: payload keyingi bildirishnomada
        } catch (e: WireFormatException) {
            Log.w(tag, "RX rad: ${e.message}") // CRC yoki uzunlik xato: ACK yo'q
            return
        }
        if (frame.header.kind != KIND_DATA) return // soatning ACK'i: e'tiborsiz
        val reply = classify(frame.payload) ?: return // tanish bo'lmagan ma'lumot: ACK yo'q, hech narsa saqlanmaydi
        // Tarix yozuvi: avval DB ga yoziladi, ACK faqat saqlangandan keyin (receiveHistoryRecord)
        if (reply is Reply.HistoryRecord) { receiveHistoryRecord(frame.header.seq, reply.payload); return }
        enqueue(FrameCodec.encodeAck(frame.header.seq))
        dispatch(reply)
    }

    /** Ma'lumot payload'ini TLV sifatida tushunadi. Tanish bo'lmasa yoki noto'g'ri bo'lsa null. */
    private fun classify(payload: ByteArray): Reply? {
        val tlv: Tlv = try {
            TlvCodec.decode(payload)
        } catch (e: WireFormatException) {
            Log.w(tag, "TLV rad: ${e.message}")
            return null
        }
        return try {
            when (tlv.type) {
                BindMessages.TYPE -> when (tlv.id) {
                    BindMessages.ID_BIND_RESPONSE -> Reply.Bind(BindMessages.decodeBindResponse(tlv))
                    BindMessages.ID_UNBIND_RESPONSE -> Reply.Unbind(BindMessages.decodeUnbindResponse(tlv))
                    BindMessages.ID_CONFIRM_RESPONSE -> Reply.Confirm(BindMessages.decodeConfirmResponse(tlv))
                    // noma'lum ID lar: e'tiborsiz, ACK yuborilmaydi
                    else -> null
                }
                HistoryMessages.TYPE -> when (tlv.id) {
                    HistoryMessages.ID_PENDING, HistoryMessages.ID_LENGTH, HistoryMessages.ID_LENGTH_COPY ->
                        Reply.HistoryCount(tlv.id, HistoryMessages.decodeCount(tlv))
                    HistoryMessages.ID_RECORD -> Reply.HistoryRecord(tlv.value)
                    // 0x0022 va boshqa ID lar: noma'lum, ACK yuborilmaydi (spec v0.4)
                    else -> null
                }
                FaceMessages.TYPE_PREPARE -> if (tlv.id == FaceMessages.ID_PREPARE_RESPONSE) Reply.FacePrepared(FaceMessages.decodePrepared(tlv)) else null
                FaceMessages.TYPE_DIAL -> if (tlv.id == FaceMessages.ID_DIAL_LIST_RESPONSE) Reply.CurrentDial(FaceMessages.decodeCurrentDial(tlv)) else null
                else -> null
            }
        } catch (e: WireFormatException) {
            Log.w(tag, "javob rad: ${e.message}")
            null
        }
    }

    /** Tushunilgan javob: ACK allaqachon navbatda, endi holat o'zgaradi. */
    private fun dispatch(reply: Reply) {
        when (reply) {
            is Reply.Bind -> {
                Log.i(tag, "0x0012 kod=${reply.status.raw}")
                execute(bindFlow.onBindResponse(reply.status))
            }
            is Reply.Unbind -> listener?.onUnbindResult(reply.status)
            is Reply.Confirm -> {
                Log.i(tag, "0x0014 kod=${reply.status.raw}")
                execute(bindFlow.onConfirmResponse(reply.status))
            }
            is Reply.HistoryCount -> {
                Log.i(tag, "tarix son: id=0x%04X n=%d".format(reply.id, reply.count))
                runHistory(history.onCount(reply.id, reply.count))
            }
            is Reply.HistoryRecord -> Unit // onNotify da alohida ishlanadi
            is Reply.FacePrepared -> {
                Log.i(tag, "siferblat tayyorlash javobi: ok=${reply.ok}")
                facePrepared(reply.ok)
            }
            is Reply.CurrentDial -> {
                Log.i(tag, "joriy siferblat 0x%04X".format(reply.id))
                // Rasmli siferblat joriy bo'lsa FitCloudPro shu sozlamani har ulanishda yuboradi (spec watchface.md §4)
                if (reply.id == FaceMessages.CUSTOM_DIAL_ID) sendData(FaceMessages.customConfig())
            }
        }
    }

    /** Tarix sinxronini boshlaydi: faqat bog'langan holatda, bir vaqtda bittasi. True: boshlandi. */
    @Synchronized fun startHistory(): Boolean {
        if (closed || bindFlow.phase != BindFlow.Phase.BOUND || face != null) return false
        val next = history.start()
        if (next is HistoryFlow.Next.None) return false
        runHistory(next)
        return true
    }

    /**
     * Tarix yozuvi (0x0030). Avval DB ga yoziladi; saqlangandan keyin ACK yuboriladi.
     * Saqlanmasa ACK ham, 0x0020 ham yo'q: soat yozuvni o'zida saqlab qoladi.
     */
    @Synchronized private fun receiveHistoryRecord(seq: Int, payload: ByteArray) {
        Log.i(tag, "tarix yozuvi: ${payload.size} bayt, boshi ${payload.take(8).joinToString("") { "%02X".format(it) }}")
        val next = history.onRecord(payload, seq)
        // Takroriy bo'lak: allaqachon DB da, faqat ACK (oldingi ACK yo'qolgan bo'lishi mumkin)
        if (next is HistoryFlow.Next.Ack) { enqueue(FrameCodec.encodeAck(seq)); return }
        if (next !is HistoryFlow.Next.Store) { runHistory(next); return }
        val stored = listener?.onHistoryRecord(next.counter, next.payload, next.part, next.complete) ?: false
        if (stored) enqueue(FrameCodec.encodeAck(seq))
        runHistory(history.onStored(stored))
    }

    /** Tarix oqimi harakatini bajaradi. Har yuborishdan keyin bosqich vaqt chegarasi qayta qo'yiladi. */
    @Synchronized private fun runHistory(next: HistoryFlow.Next) {
        when (next) {
            is HistoryFlow.Next.None, is HistoryFlow.Next.Store, is HistoryFlow.Next.Ack -> {}
            is HistoryFlow.Next.Send -> {
                for (p in next.payloads) {
                    if (!sendData(p)) { historyFailed(context.getString(R.string.err_write_mtu, mtu)); return }
                }
                main.removeCallbacks(historyTimeout)
                if (next.done) listener?.onHistoryDone(true, null)
                else main.postDelayed(historyTimeout, HISTORY_STEP_TIMEOUT_MS)
            }
            is HistoryFlow.Next.Fail -> historyFailed(context.getString(next.messageRes))
        }
    }

    private fun historyFailed(message: String) {
        history.abort()
        main.removeCallbacks(historyTimeout)
        listener?.onHistoryDone(false, message)
    }

    @Synchronized private fun startBind() {
        if (closed) return
        execute(bindFlow.start())
    }

    /** Bog'lash oqimi harakatini bajaradi. Har 0x0011 yuborilganda keyingi tick qayta rejalashtiriladi. */
    @Synchronized private fun execute(next: BindFlow.Next) {
        when (next) {
            BindFlow.Next.None -> {}
            BindFlow.Next.SendCheck -> {
                if (!sendData(BindMessages.confirmRequest(account))) { fail(context.getString(R.string.err_write_mtu_bind, mtu)); return }
                Log.i(tag, "0x0013 yuborildi, faza=${bindFlow.phase}")
                // Tasdiq kutilayotganda davriy tekshiruv: soat ✓ bosilganini 0x0014 = 00 bilan bildiradi
                if (bindFlow.phase == BindFlow.Phase.CONFIRM_ON_WATCH) listener?.onConfirmOnWatch()
                main.removeCallbacks(retryBind)
                main.postDelayed(retryBind, CONFIRM_RETRY_MS)
            }
            BindFlow.Next.SendBind -> {
                if (!sendData(BindMessages.bindRequest(account))) { fail(context.getString(R.string.err_write_mtu_bind, mtu)); return }
                Log.i(tag, "0x0011 yuborildi, faza=${bindFlow.phase}")
                // [E] Soat ekranida tasdiq kutilmoqda: foydalanuvchi ✓ bossa 0x0012 = 00 keladi.
                if (bindFlow.phase == BindFlow.Phase.CONFIRM_ON_WATCH) listener?.onConfirmOnWatch()
                main.removeCallbacks(retryBind)
                main.postDelayed(retryBind, CONFIRM_RETRY_MS)
            }
            BindFlow.Next.Bound -> {
                main.removeCallbacks(retryBind)
                listener?.onBound()
            }
            is BindFlow.Next.Failed -> fail(context.getString(R.string.err_bind_rejected, next.raw))
        }
    }

    private fun fail(message: String) {
        main.removeCallbacks(retryBind)
        listener?.onError(message)
    }

    /** Ma'lumot frame'i. SEQ 0x0001 dan boshlanadi, har frame'da +1 (spec frame.md). */
    @Synchronized private fun sendData(payload: ByteArray): Boolean {
        seq = (seq + 1) and 0xFFFF
        return enqueue(FrameCodec.encodeData(seq, payload))
    }

    /** Asosiy kanal yozish navbatiga qo'yadi. ATT yozuvi MTU-3 dan katta bo'lsa yubormaydi. */
    @Synchronized private fun enqueue(frame: ByteArray): Boolean {
        val ch = writeChar ?: return false
        val type = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0)
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return enqueue(ch, frame, type)
    }

    @Synchronized private fun enqueue(ch: BluetoothGattCharacteristic, bytes: ByteArray, type: Int): Boolean {
        if (bytes.size > mtu - 3) {
            Log.w(tag, "yozuv ${bytes.size} bayt, sig'adigan hajm ${mtu - 3}: yuborilmadi")
            return false
        }
        writeQueue.addLast(Op.Write(ch, bytes, type))
        pump()
        return true
    }

    @Synchronized private fun pump() {
        if (writing) return
        val g = gatt ?: return
        val next = writeQueue.firstOrNull() ?: return
        val ok = when (next) {
            is Op.Write -> if (Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(next.ch, next.bytes, next.type) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION") run { next.ch.writeType = next.type; next.ch.value = next.bytes; g.writeCharacteristic(next.ch) }
            }
            is Op.EnableNotify -> {
                val d = next.ch.getDescriptor(CCCD_UUID)
                d != null && g.setCharacteristicNotification(next.ch, true) && if (Build.VERSION.SDK_INT >= 33) {
                    g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION") run { d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; g.writeDescriptor(d) }
                }
            }
        }
        if (ok) {
            writeQueue.removeFirst()
            writing = true
            rejects = 0
            return
        }
        // Stek band (masalan boshqa ilova shu soat bilan ishlayapti): amal navbatda qoladi, qisqa kutib qayta uriniladi
        if (++rejects <= WRITE_RETRIES) { main.postDelayed(retryPump, WRITE_RETRY_MS); return }
        rejects = 0
        Log.w(tag, "yozish rad etildi")
        listener?.onError(context.getString(R.string.err_write))
    }

    @Synchronized private fun writeDone(status: Int) {
        writing = false
        if (status != BluetoothGatt.GATT_SUCCESS) Log.w(tag, "yozish xatosi status=$status")
        pump()
    }

    // ---------------------------------------------------------------- siferblat

    /** Siferblat xizmati bormi (ulanish tayyor bo'lgach). */
    val faceSupported: Boolean get() = faceData != null && faceControl != null

    /**
     * Siferblat faylini yuborishni boshlaydi (spec watchface.md). Faqat bog'langan holatda, tarix sinxroni va
     * boshqa uzatish bo'lmaganda. Natija onFaceProgress / onFaceDone orqali keladi.
     */
    @Synchronized fun startFace(file: ByteArray): Boolean {
        if (closed || bindFlow.phase != BindFlow.Phase.BOUND || history.active || face != null) return false
        val ctrl = faceControl ?: return false
        if (faceData == null || file.isEmpty()) return false
        face = uz.xasan.orvell.protocol.wire.FaceUpload(file, chunk = minOf(uz.xasan.orvell.protocol.wire.FaceUpload.CHUNK, mtu - 3))
        // FitCloudPro avval 01/0001 yuboradi va 01/0002 = 00 00 ni kutadi (spec watchface.md §4).
        // Javob kelmasa ham uzatish boshlanadi: soat tayyorlashsiz ham faylni qabul qilgan (2026-10-13 sinovi).
        facePreparing = true
        sendData(FaceMessages.prepareRequest())
        main.postDelayed(facePrepareTimeout, FACE_PREPARE_TIMEOUT_MS)
        return true
    }

    /** Tayyorlash javobi yoki vaqt chegarasi: uzatish boshlanadi (soat rad etsa — to'xtaydi). */
    @Synchronized private fun facePrepared(ok: Boolean?) {
        if (!facePreparing) return
        facePreparing = false
        main.removeCallbacks(facePrepareTimeout)
        val upload = face ?: return
        if (ok == false) {
            face = null
            listener?.onFaceDone(false, context.getString(R.string.err_face_rejected))
            return
        }
        if (ok == null) Log.w(tag, "siferblat tayyorlash javobi kelmadi, uzatish boshlanadi")
        val ctrl = faceControl ?: return
        if (!faceNotifyOn) { writeQueue.addLast(Op.EnableNotify(ctrl)); pump() } else runFace(upload.start())
    }

    /** Joriy siferblatni so'raydi (02/0052); rasmli siferblat bo'lsa uning sozlamasi yuboriladi. Faqat bog'langan holatda. */
    @Synchronized fun queryDial(): Boolean {
        if (closed || bindFlow.phase != BindFlow.Phase.BOUND) return false
        return sendData(FaceMessages.dialListRequest())
    }

    @Synchronized private fun faceNotifyDone(status: Int) {
        writing = false
        val upload = face
        if (status != BluetoothGatt.GATT_SUCCESS) {
            if (upload != null) { face = null; listener?.onFaceDone(false, context.getString(R.string.err_notify_status, status)) }
        } else {
            faceNotifyOn = true
            if (upload != null) runFace(upload.start())
        }
        pump()
    }

    @Synchronized private fun onFaceControl(value: ByteArray) {
        if (closed) return
        val upload = face ?: return
        runFace(upload.onControl(value))
    }

    @Synchronized private fun runFace(next: uz.xasan.orvell.protocol.wire.FaceUpload.Next) {
        val upload = face ?: return
        val ctrl = faceControl ?: return
        val data = faceData ?: return
        val withResponse = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val noResponse = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        when (next) {
            is uz.xasan.orvell.protocol.wire.FaceUpload.Next.None -> return
            is uz.xasan.orvell.protocol.wire.FaceUpload.Next.Control -> enqueue(ctrl, next.bytes, withResponse)
            is uz.xasan.orvell.protocol.wire.FaceUpload.Next.Block -> {
                next.before?.let { enqueue(ctrl, it, withResponse) }
                for (c in next.chunks) enqueue(data, c, noResponse)
                enqueue(ctrl, next.after, withResponse)
                listener?.onFaceProgress(upload.acked, upload.size)
            }
            is uz.xasan.orvell.protocol.wire.FaceUpload.Next.Done -> {
                enqueue(ctrl, next.bytes, withResponse)
                main.removeCallbacks(faceTimeout)
                face = null
                Log.i(tag, "siferblat yuborildi: ${upload.size} bayt")
                listener?.onFaceProgress(upload.size, upload.size)
                listener?.onFaceDone(true, null)
                return
            }
            is uz.xasan.orvell.protocol.wire.FaceUpload.Next.Fail -> {
                main.removeCallbacks(faceTimeout)
                face = null
                Log.w(tag, "siferblat to'xtadi: ${next.reason}")
                listener?.onFaceDone(false, context.getString(R.string.err_face_rejected))
                return
            }
        }
        main.removeCallbacks(faceTimeout)
        main.postDelayed(faceTimeout, FACE_STEP_TIMEOUT_MS)
    }

    /** Ulanish uzilganda: kutilayotgan sarlavha, navbat va bog'lash holati tashlanadi (ilova qoidasi). */
    @Synchronized private fun resetLink() {
        reader.reset()
        writeQueue.clear()
        writing = false
        rejects = 0
        bindFlow.reset()
        history.reset()
        main.removeCallbacks(retryBind)
        main.removeCallbacks(historyTimeout)
        main.removeCallbacks(faceTimeout)
        main.removeCallbacks(facePrepareTimeout)
        facePreparing = false
        main.removeCallbacks(retryPump)
        faceNotifyOn = false
        face?.let { face = null; listener?.onFaceDone(false, context.getString(R.string.err_face_disconnected)) }
    }
}
