package uz.xasan.orvell.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import uz.xasan.orvell.BuildConfig
import uz.xasan.orvell.data.AppSettings
import uz.xasan.orvell.data.Sample
import uz.xasan.orvell.data.Store
import uz.xasan.orvell.data.T
import uz.xasan.orvell.data.WATCH_HISTORY_SOURCE
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Sinx xatolari: matni UI'da tanlangan tilda chiqariladi. */
sealed interface SyncError {
    /** Release'da http:// manzil (usesCleartextTraffic=false). */
    data object HttpsOnly : SyncError
    data class BadToken(val code: Int) : SyncError
    data class Http(val code: Int, val body: String) : SyncError
    data class Network(val detail: String) : SyncError

    /** Serverga (ack) yuboriladigan texnik matn. */
    fun technical(): String = when (this) {
        HttpsOnly -> "https required"
        is BadToken -> "unauthorized ($code)"
        is Http -> "server $code: ${body.take(120)}"
        is Network -> detail
    }
}

private class SyncFailure(val error: SyncError) : Exception(error.technical())

/** Serverning yaroqsiz yozuv javobi (HTTP 422): bitta yozuv rad etiladi, navbat davom etadi. */
internal const val HTTP_UNPROCESSABLE = 422

/** Serverdan 2xx va 422 dan boshqa javob: paket qabul qilinmadi, navbat shu yerda to'xtaydi. */
internal class BatchRefused(val code: Int, val body: String) : Exception("server $code")

/**
 * Paketni serverga yuboradi ([post]: HTTP kodi va javob matni). 2xx: hamma yozuv [accepted] ga beriladi.
 * 422: paket ikkiga bo'linib qayta yuboriladi, shunda yaroqsiz yozuv topiladi. Yakkalangan 422 yozuvi [rejected] ga
 * beriladi, qolgani yuboriladi. Boshqa javobda [BatchRefused]. Har natija darhol beriladi: keyingi xato uni yo'qotmaydi.
 */
internal fun <T> deliverBatch(
    items: List<Pair<Long, T>>,
    post: (List<T>) -> Pair<Int, String>,
    accepted: (List<Long>) -> Unit,
    rejected: (List<Long>) -> Unit,
) {
    if (items.isEmpty()) return
    val (code, body) = post(items.map { it.second })
    when {
        code in 200..299 -> accepted(items.map { it.first })
        code == HTTP_UNPROCESSABLE && items.size == 1 -> rejected(items.map { it.first })
        code == HTTP_UNPROCESSABLE -> {
            val mid = items.size / 2
            deliverBatch(items.subList(0, mid), post, accepted, rejected)
            deliverBatch(items.subList(mid, items.size), post, accepted, rejected)
        }
        else -> throw BatchRefused(code, body)
    }
}

/**
 * Xato matnidan tokenni olib tashlaydi. Matn UI'da ko'rinadi va serverga (ack) ketadi; istisno yoki server javobi
 * sarlavha qiymatini ("Bearer ...") o'z ichiga olishi mumkin.
 */
internal fun redactToken(text: String, token: String): String {
    val t = token.trim()
    return if (t.isEmpty()) text else text.replace(t, "***")
}

/**
 * Ixtiyoriy: telefondagi yozuvlarni foydalanuvchining o'z serveriga (Life-Tracker API mos) yuboradi.
 * Faqat server "qabul qildim" (2xx) degandan keyin yozuv yuborilgan deb belgilanadi — tarmoq uzilsa keyin qayta yuboriladi.
 */
object ServerSync {
    /** Manba nomi: boshqa ilovalar yozuvlari ustiga yozmaslik uchun alohida (server "fitcloud" so'zidan soat deb taniydi). */
    const val SOURCE = "orvell-fitcloud"

    /** [rejected]: server 422 bilan rad etgan yozuvlar. DB da qoladi, navbatga qaytmaydi. */
    data class Status(val running: Boolean = false, val lastOk: Long = 0, val lastError: SyncError? = null, val sentTotal: Int = 0, val pending: Long = 0, val rejected: Long = 0)

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status
    private val lock = Mutex()

    fun enabled(s: AppSettings) = s.serverEnabled && s.serverUrl.isNotBlank() && s.serverToken.isNotBlank()

    /** Release'da shifrlanmagan HTTP taqiqlangan (usesCleartextTraffic=false): tushunarsiz tarmoq xatosi o'rniga aniq xabar. */
    fun cleartextBlocked(url: String, debug: Boolean = BuildConfig.DEBUG) = !debug && url.trim().startsWith("http://", ignoreCase = true)

    /** Yuborilmagan yozuvlar soni. DB xatosi bo'lsa 0 (push ilovani yiqitmasin). */
    private fun pendingCount(store: Store): Long = runCatching { store.unsyncedCount() }.getOrDefault(0L)

    /** Server rad etgan yozuvlar soni. DB xatosi bo'lsa 0. */
    private fun rejectedCount(store: Store): Long = runCatching { store.rejectedCount() }.getOrDefault(0L)

    /**
     * Hamma yuborilmagan yozuvlarni yuboradi. Qaytaradi: qabul qilingan yozuvlar soni (xato bo'lsa null).
     * Server 422 (yaroqsiz yozuv) bitta yozuvni to'xtatmaydi: u rad etilgan deb belgilanadi, navbat davom etadi.
     * Boshqa xato (tarmoq, 401, 5xx) navbatni to'xtatadi: yozuvlar yuborilmagan holda qoladi.
     */
    suspend fun push(store: Store, s: AppSettings): Int? = lock.withLock {
        if (!enabled(s)) return@withLock null
        withContext(Dispatchers.IO) {
            _status.update { it.copy(running = true, pending = pendingCount(store)) }
            var sent = 0
            try {
                if (cleartextBlocked(s.serverUrl)) throw SyncFailure(SyncError.HttpsOnly)
                while (true) {
                    val batch = store.unsynced(500)
                    if (batch.isEmpty()) break
                    try {
                        deliverBatch(
                            items = batch.map { (id, smp) -> id to toRecord(smp, s.watch.name) },
                            post = { records ->
                                request("POST", url(s.serverUrl, "/ingest"), s.serverToken, JSONObject().put("records", JSONArray(records)).toString())
                            },
                            // Har qism DB ga darhol yoziladi: keyingi xato oldin qabul qilingan qismni yo'qotmaydi
                            accepted = { ids -> store.markSynced(ids); sent += ids.size },
                            rejected = { ids -> store.markRejected(ids) },
                        )
                    } catch (e: BatchRefused) {
                        throw SyncFailure(httpError(e.code, e.body, s.serverToken))
                    }
                }
                _status.update { it.copy(running = false, lastOk = System.currentTimeMillis(), lastError = null, sentTotal = it.sentTotal + sent, pending = 0, rejected = rejectedCount(store)) }
                sent
            } catch (e: Exception) {
                val err = (e as? SyncFailure)?.error ?: SyncError.Network(redactToken(e.message ?: e.javaClass.simpleName, s.serverToken))
                _status.update { it.copy(running = false, lastError = err, pending = pendingCount(store), rejected = rejectedCount(store)) }
                null
            }
        }
    }

    /** Ulanishni tekshirish: null = hammasi joyida, aks holda xato. */
    suspend fun test(url: String, token: String): SyncError? = withContext(Dispatchers.IO) {
        if (cleartextBlocked(url)) return@withContext SyncError.HttpsOnly
        try {
            val (code, body) = request("GET", url(url, "/status"), token, null)
            if (code in 200..299) null else httpError(code, body, token)
        } catch (e: Exception) {
            SyncError.Network(redactToken(e.message ?: e.javaClass.simpleName, token))
        }
    }

    private fun httpError(code: Int, body: String, token: String): SyncError =
        if (code == 401 || code == 403) SyncError.BadToken(code) else SyncError.Http(code, redactToken(body, token).take(80))

    internal fun url(base: String, path: String) = base.trim().trimEnd('/') + path

    internal fun request(method: String, url: String, token: String, body: String?): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            // Yo'naltirish kuzatilmaydi: POST 301/302 bilan GET ga aylanib 2xx bersa, yozuvlar noto'g'ri "yuborilgan" bo'lib qolardi
            c.instanceFollowRedirects = false
            c.connectTimeout = 10_000
            c.readTimeout = 30_000
            c.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            return code to text
        } finally {
            c.disconnect()
        }
    }

    private fun iso(ms: Long) = Instant.ofEpochMilli(ms).toString()

    /** Orvell yozuvi -> server yozuvi (turlar va birliklar Life-Tracker API'siga mos). */
    fun toRecord(s: Sample, watchName: String?): JSONObject {
        val payload = JSONObject()
            .put("device", JSONObject().put("type", 1).put("manufacturer", "FitCloud").put("model", watchName ?: "watch"))
            .put("recording_method", 2)
            .put("app", "orvell")
        if (s.source == "watch_live") payload.put("measured", "realtime")
        val (type, unit) = when (s.type) {
            T.STEPS_TOTAL -> "steps" to "count"
            T.DIST_TOTAL -> "distance" to "m"
            T.KCAL_TOTAL -> "active_calories" to "kcal"
            // Soat tarixida kunlik jami yo'q: 5 daqiqalik bo'laklar "steps" bo'lib ketadi, server ularni manba bo'yicha qo'shadi
            T.STEPS_BUCKET -> { payload.put("distance_cm", s.extra ?: 0.0); (if (s.source == WATCH_HISTORY_SOURCE) "steps" else "steps_bucket") to "count" }
            T.KCAL_BUCKET -> "active_calories" to "kcal"
            T.HR -> "heart_rate" to "bpm"
            T.SPO2 -> "spo2" to "%"
            T.BP -> { payload.put("systolic_mmHg", s.value.toInt()).put("diastolic_mmHg", s.extra?.toInt()); "blood_pressure" to "mmHg" }
            T.RESP -> "respiratory_rate" to "bpm"
            T.BATTERY -> "watch_battery" to "%"
            T.SLEEP -> {
                payload.put("stages", JSONArray().put(JSONObject().put("stage", s.value.toInt()).put("start", iso(s.start)).put("end", iso(s.end))))
                "sleep" to null
            }
            else -> s.type to null
        }
        return JSONObject().put("type", type).put("start_time", iso(s.start)).put("end_time", iso(s.end))
            .apply { if (type != "sleep") put("value", s.value); if (unit != null) put("unit", unit) }
            .put("source", SOURCE).put("payload", payload)
    }
}
