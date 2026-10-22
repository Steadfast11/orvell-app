package uz.xasan.orvell.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerSyncTest {
    /** Sintetik token: haqiqiy sir emas. */
    private val token = "synthetic-token-0000"

    @Test
    fun tokenIsRemovedFromErrorText() {
        // istisno xabari sarlavha qiymatini o'z ichiga olgan holat (UI'da ko'rinadi)
        val out = redactToken("invalid header value: Bearer $token", token)
        assertFalse(out.contains(token))
        assertEquals("invalid header value: Bearer ***", out)
    }

    @Test
    fun storedTokenWithSpacesIsStillRemoved() {
        // so'rovda token trim() bilan yuboriladi: matnda ham trim qilingani qidiriladi
        assertEquals("Bearer ***", redactToken("Bearer $token", "  $token\n"))
    }

    @Test
    fun blankTokenLeavesTextUnchanged() {
        assertEquals("timeout", redactToken("timeout", ""))
        assertEquals("timeout", redactToken("timeout", "   "))
    }

    // ---------------------------------------------------------- 422: bitta yaroqsiz yozuv navbatni to'xtatmaydi

    /** Sintetik paket: id = yozuv raqami (serverga JSON emas, faqat id lar ketadi deb olinadi). */
    private fun batchOf(vararg ids: Long): List<Pair<Long, Long>> = ids.map { it to it }

    /** Server imitatsiyasi: [bad] yozuvi bor paketga 422, qolganiga 200. */
    private fun server(bad: Set<Long>): (List<Long>) -> Pair<Int, String> = { batch ->
        if (batch.any { it in bad }) 422 to "invalid record" else 200 to "ok"
    }

    @Test
    fun cleanBatchIsAcceptedInOneRequest() {
        var calls = 0
        val acc = mutableListOf<Long>()
        val rej = mutableListOf<Long>()
        deliverBatch(batchOf(1, 2, 3), post = { calls++; 200 to "ok" }, accepted = { acc += it }, rejected = { rej += it })
        assertEquals(1, calls)
        assertEquals(listOf(1L, 2L, 3L), acc)
        assertTrue(rej.isEmpty())
    }

    @Test
    fun oneBadRecordIsRejectedAndTheRestAreAccepted() {
        val acc = mutableListOf<Long>()
        val rej = mutableListOf<Long>()
        deliverBatch(batchOf(1, 2, 3, 4, 5, 6, 7, 8), post = server(setOf(5L)), accepted = { acc += it }, rejected = { rej += it })
        assertEquals(listOf(5L), rej)
        assertEquals(listOf(1L, 2L, 3L, 4L, 6L, 7L, 8L), acc.sorted())
    }

    @Test
    fun everyRecordBadIsRejectedAndNoneAccepted() {
        val acc = mutableListOf<Long>()
        val rej = mutableListOf<Long>()
        deliverBatch(batchOf(1, 2, 3), post = server(setOf(1L, 2L, 3L)), accepted = { acc += it }, rejected = { rej += it })
        assertTrue(acc.isEmpty())
        assertEquals(listOf(1L, 2L, 3L), rej.sorted())
    }

    @Test
    fun serverErrorStopsAfterEarlierPartsWereReported() {
        // Oldingi yarim (1, 2) qabul qilinadi va darhol xabar qilinadi; keyingi yarimda 500: navbat to'xtaydi
        val acc = mutableListOf<Long>()
        val rej = mutableListOf<Long>()
        val post: (List<Long>) -> Pair<Int, String> = { batch ->
            when {
                batch.size > 2 -> 422 to "bad"
                batch.first() == 1L -> 200 to "ok"
                else -> 500 to "down"
            }
        }
        val failure = runCatching {
            deliverBatch(batchOf(1, 2, 3, 4), post = post, accepted = { acc += it }, rejected = { rej += it })
        }.exceptionOrNull()
        assertEquals(500, (failure as BatchRefused).code)
        assertEquals(listOf(1L, 2L), acc)
        assertTrue(rej.isEmpty())
    }

    @Test
    fun unauthorizedStopsTheQueueWithNothingAccepted() {
        val acc = mutableListOf<Long>()
        val failure = runCatching {
            deliverBatch(batchOf(1, 2), post = { 401 to "no" }, accepted = { acc += it }, rejected = {})
        }.exceptionOrNull()
        assertEquals(401, (failure as BatchRefused).code)
        assertTrue(acc.isEmpty())
    }

    @Test
    fun emptyBatchSendsNothing() {
        var calls = 0
        deliverBatch(emptyList<Pair<Long, Long>>(), post = { calls++; 200 to "ok" }, accepted = {}, rejected = {})
        assertEquals(0, calls)
    }

    @Test
    fun releaseBuildBlocksPlainHttpOnly() {
        assertTrue(ServerSync.cleartextBlocked("http://synthetic-host", debug = false))
        assertTrue(ServerSync.cleartextBlocked("HTTP://synthetic-host", debug = false))
        assertFalse(ServerSync.cleartextBlocked("https://synthetic-host", debug = false))
        assertFalse(ServerSync.cleartextBlocked("http://synthetic-host", debug = true))
    }
}
