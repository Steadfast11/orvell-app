package uz.xasan.orvell.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import uz.xasan.orvell.OrvellApp
import uz.xasan.orvell.watch.WatchManager
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * Serverga doimiy ulanish (SSE): server (masalan AI'ning "hozir yangila" so'rovi) "sync" buyrug'ini yuborsa,
 * Orvell darhol soatdan o'qiydi, serverga yuboradi va natijani qaytaradi. Faqat server sinxroni yoqilganda ishlaydi.
 */
object DeviceStream {
    fun run(scope: CoroutineScope, app: OrvellApp): Job = scope.launch(Dispatchers.IO) {
        var backoff = 3_000L
        while (isActive) {
            val s = app.settings.value
            if (!ServerSync.enabled(s) || ServerSync.cleartextBlocked(s.serverUrl)) {
                app.settings.flow.first { it != s }; continue
            }
            try {
                val c = URL(ServerSync.url(s.serverUrl, "/device/stream?client=orvell")).openConnection() as HttpURLConnection
                c.setRequestProperty("Authorization", "Bearer ${s.serverToken.trim()}")
                c.connectTimeout = 10_000
                c.readTimeout = 50_000  // server har 15 soniyada ping yuboradi
                try {
                    if (c.responseCode !in 200..299) throw IOException("stream ${c.responseCode}")
                    c.inputStream.bufferedReader().use { r ->
                        while (isActive) {
                            val line = r.readLine() ?: break
                            // Oqim haqiqatan ishladi (ping keldi): qabul qilib darhol yopadigan serverga 3 s da urilmaslik uchun shundan keyin
                            backoff = 3_000L
                            val now = app.settings.value
                            // manzil/token o'zgarsa yoki o'chirilsa — qayta ulanamiz
                            if (now.serverUrl != s.serverUrl || now.serverToken != s.serverToken || !now.serverEnabled) break
                            if (line.startsWith("data:")) {
                                val msg = runCatching { JSONObject(line.removePrefix("data:").trim()) }.getOrNull() ?: continue
                                if (msg.optString("cmd") == "sync") launch { handle(app, msg.optString("id")) }
                            }
                        }
                    }
                } finally { c.disconnect() }
            } catch (_: Exception) {
                // tarmoq yoki server yo'q: keyin qayta uriniladi
            }
            // Tasodifiy qo'shimcha: server qayta ishga tushganda barcha qurilmalar bir vaqtda urilmasin
            delay(backoff + Random.nextLong(backoff / 3 + 1))
            backoff = minOf(backoff * 2, 30_000L)
        }
    }

    private suspend fun handle(app: OrvellApp, id: String) {
        val errors = JSONArray()
        // Server javobni ~25 s kutadi: soat sinxroni uzoqroq cho'zilsa u fonda davom etadi, javob kechikmaydi
        val pulled = runCatching { withTimeoutOrNull(PULL_WAIT_MS) { WatchManager.pullNow() } }.getOrNull()
        val watchOk = pulled?.ok == true
        // Soat ulanmagan yoki o'qish xato bo'lsa ham oxirgi saqlangan ma'lumot yuboriladi
        if (pulled == null) errors.put("soat ulanmagan, band yoki sinxron hali tugamadi — oxirgi saqlangan ma'lumot yuborildi")
        else if (!pulled.ok) errors.put("soatdan o'qish to'xtadi: ${pulled.error ?: "-"} — oxirgi saqlangan ma'lumot yuborildi")
        val sent = ServerSync.push(app.store, app.settings.value)
        if (sent == null) errors.put(ServerSync.status.value.lastError?.technical() ?: "yuborib bo'lmadi")
        val ack = JSONObject().put("id", id).put("ok", watchOk && sent != null).put("read", pulled?.newRecords ?: 0)
            .put("added", sent ?: 0).put("errors", errors)
        val s = app.settings.value
        // Javob yo'qolsa server "sinxron bo'lmadi" deb hisoblaydi: qisqa oraliq bilan qayta yuboriladi
        for (attempt in 0 until ACK_ATTEMPTS) {
            val code = runCatching { ServerSync.request("POST", ServerSync.url(s.serverUrl, "/device/ack"), s.serverToken, ack.toString()).first }.getOrNull()
            if (code != null && code in 200..499) return
            delay(1_000L shl attempt)
        }
    }

    private const val PULL_WAIT_MS = 18_000L
    private const val ACK_ATTEMPTS = 3
}
