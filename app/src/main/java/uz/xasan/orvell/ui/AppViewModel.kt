package uz.xasan.orvell.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.xasan.orvell.AppLocale
import uz.xasan.orvell.OrvellApp
import uz.xasan.orvell.R
import uz.xasan.orvell.WatchService
import uz.xasan.orvell.data.AppSettings
import uz.xasan.orvell.data.DayStats
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.phone.PhoneSteps
import uz.xasan.orvell.sync.ServerSync
import uz.xasan.orvell.sync.SyncError
import uz.xasan.orvell.watch.WatchManager
import uz.xasan.orvell.watch.WatchScanner
import uz.xasan.orvell.watch.WatchState
import java.io.File
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val orvell = app as OrvellApp
    val settings: StateFlow<AppSettings> = orvell.settings.flow
    val watch: StateFlow<WatchState> = WatchManager.state
    /** Tarix sinxroni natijasi (bir martalik): snackbar uchun. */
    val syncResult = WatchManager.syncResult
    val scanner = WatchScanner(app)

    /** Daqiqada bir marta "tiq" — kun almashganda va "N daqiqa oldin" yozuvlari yangilanishi uchun. */
    private val minuteTick = flow { while (true) { emit(System.currentTimeMillis() / 60_000); delay(30_000) } }

    // baza xatosi (masalan xotira to'la) ekranni yiqitmasin: oxirgi qiymat qoladi, keyingi "tiq"da qayta urinadi
    val today: StateFlow<DayStats?> = combine(orvell.store.changes, minuteTick, PhoneSteps.today) { _, _, _ ->
        // Soat qadami yo'q yoki oxirgi sinxrondan beri telefon ko'proq sanagan bo'lsa telefon soni ko'rsatiladi
        // (manbasi belgilanadi): kun ichidagi son kamaymaydi. Faqat bugungi son.
        val phone = PhoneSteps.valueFor(LocalDate.now())
        runCatching { orvell.health.day(LocalDate.now()) }.getOrNull()?.let { d ->
            if (phone != null && (!d.hasSteps || phone > d.steps)) d.copy(steps = phone, hasSteps = true, stepsFromPhone = true) else d
        }
    }
        .filterNotNull().flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val week: StateFlow<List<Health.DayPoint>> = combine(orvell.store.changes, minuteTick) { _, _ -> runCatching { orvell.health.series(LocalDate.now(), 7) }.getOrNull() }
        .filterNotNull().flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saqlangan xom tarix yozuvlari soni: sinxron tugaganda va bazada o'zgarganda qayta hisoblanadi. */
    val historyTotal: StateFlow<Long> = combine(orvell.store.changes, WatchManager.state.map { it.historyAt }.distinctUntilChanged()) { _, _ ->
        runCatching { orvell.store.watchRawCount() }.getOrDefault(0L)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Eski versiya namunalari soni (talqini tasdiqlanmagan): ekranlar ogohlantirish uchun shuni ishlatadi. */
    val legacyCount: StateFlow<Long> = orvell.store.changes.map { runCatching { orvell.store.legacySampleCount() }.getOrDefault(0L) }
        .flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Bugungi eski versiya namunalari: "Bugun" ekranidagi ogohlantirish faqat bugungi qiymatlarga tegishli. */
    val legacyToday: StateFlow<Long> = combine(orvell.store.changes, minuteTick) { _, _ ->
        val zone = java.time.ZoneId.systemDefault()
        val d = LocalDate.now()
        runCatching {
            orvell.store.legacySampleCount(d.atStartOfDay(zone).toInstant().toEpochMilli(), d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        }.getOrDefault(0L)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    suspend fun day(d: LocalDate): DayStats = withContext(Dispatchers.IO) { orvell.health.day(d) }
    suspend fun series(end: LocalDate, days: Int) = withContext(Dispatchers.IO) { orvell.health.series(end, days) }
    suspend fun stats(): Pair<Long, Long?> = withContext(Dispatchers.IO) { orvell.store.count() to orvell.store.firstTime() }

    fun update(f: (AppSettings) -> AppSettings) = orvell.settings.update(f)

    /** Tarix sinxroni: soat bog'langan bo'lsa boshlanadi, aks holda (yoki sinxron ketayotgan bo'lsa) hech narsa qilmaydi. */
    fun syncNow() = WatchManager.syncNow()


    fun pair(address: String, name: String?) {
        WatchManager.pair(address, name)
        WatchService.start(getApplication())
    }

    /** Ajratish: soat tasdiqlasa (yoki ulanmagan bo'lsa) xizmat to'xtaydi. Soat rad etsa juftlik qoladi. */
    fun unpair() {
        viewModelScope.launch {
            if (WatchManager.unpair()) WatchService.stop(getApplication())
        }
    }

    fun reconnect() = WatchManager.restart()

    fun startService() = WatchService.start(getApplication())

    fun pushNow() { viewModelScope.launch { ServerSync.push(orvell.store, orvell.settings.value) } }
    suspend fun testServer(url: String, token: String): SyncError? = ServerSync.test(url, token)
    suspend fun pendingUploads(): Long = withContext(Dispatchers.IO) { orvell.store.unsyncedCount() }

    /** Server 422 bilan rad etgan yozuvlar soni (DB da qoladi). */
    suspend fun rejectedUploads(): Long = withContext(Dispatchers.IO) { orvell.store.rejectedCount() }

    /** Ma'lumotni CSV qilib, ulashish oynasini ochadi. */
    suspend fun exportIntent(): Intent = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val dir = File(ctx.cacheDir, "export").apply { mkdirs() }
        val f = File(dir, "orvell-${LocalDate.now()}.csv")
        f.bufferedWriter().use { orvell.store.exportCsv(it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, AppLocale.localized(ctx).getString(R.string.settings_export_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Xom soat tarixini (diagnostika) CSV qilib, ulashish oynasini ochadi. */
    suspend fun exportWatchRawIntent(): Intent = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val dir = File(ctx.cacheDir, "export").apply { mkdirs() }
        val f = File(dir, "orvell-raw-${LocalDate.now()}.csv")
        f.bufferedWriter().use { orvell.store.exportWatchRawCsv(it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, AppLocale.localized(ctx).getString(R.string.settings_export_raw_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Ma'lumotni o'chiradi. DB xatosi bo'lsa ilova yiqilmaydi. */
    fun clearData() { viewModelScope.launch(Dispatchers.IO) { runCatching { orvell.store.clearAll() } } }

    /** Ilovani boshlang'ich holatga qaytaradi: soat uziladi, ma'lumot va sozlamalar o'chadi. */
    fun resetApp() {
        viewModelScope.launch {
            // avval soatdan ajratishga urinadi, so'ng xizmat to'xtaydi (ulanish uziladi) va ma'lumot o'chadi
            WatchManager.unpair()
            WatchService.stop(getApplication())
            // Qadamlar alohida: ma'lumot o'chmasa ham sozlamalar tiklanadi va aksincha
            withContext(Dispatchers.IO) {
                runCatching { orvell.store.clearAll() }
                runCatching { orvell.settings.reset() }
            }
        }
    }
}
