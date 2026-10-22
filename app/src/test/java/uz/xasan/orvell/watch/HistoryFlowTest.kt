package uz.xasan.orvell.watch

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.xasan.orvell.protocol.wire.HistoryMessages

class HistoryFlowTest {
    /** Sintetik yozuv: haqiqiy qiymat emas, faqat hajm va tartib. */
    private fun record(size: Int) = ByteArray(size) { (it + 1).toByte() }

    private fun sends(next: HistoryFlow.Next): List<ByteArray> {
        assertTrue("Send kutildi: $next", next is HistoryFlow.Next.Send)
        return (next as HistoryFlow.Next.Send).payloads
    }

    /** 05 00 01 00 01 <c> dan hisoblagich qiymatini oladi. */
    private fun counterOf(payload: ByteArray): Int = payload[5].toInt() and 0xFF

    /** Hisoblagich so'roviga 0x0007 va 0x0008 javobi (N, teng). */
    private fun answerLength(f: HistoryFlow, n: Int): HistoryFlow.Next {
        f.onCount(HistoryMessages.ID_LENGTH, n)
        return f.onCount(HistoryMessages.ID_LENGTH_COPY, n)
    }

    @Test
    fun startSendsStartOnce() {
        val f = HistoryFlow()
        assertArrayEquals(HistoryMessages.startRequest(), sends(f.start()).single())
        assertEquals(HistoryFlow.Phase.PENDING_WAIT, f.phase)
        assertTrue(f.start() is HistoryFlow.Next.None)
    }

    @Test
    fun emptySyncAsksEveryCounterInOrderThenEnds() {
        val f = HistoryFlow()
        f.start()
        val requested = mutableListOf(counterOf(sends(f.onCount(HistoryMessages.ID_PENDING, 12)).single()))
        val last = HistoryMessages.COUNTERS.size - 1
        for (i in HistoryMessages.COUNTERS.indices) {
            val next = answerLength(f, 0)
            val s = sends(next)
            assertArrayEquals(HistoryMessages.commitRequest(), s[0])
            if (i < last) {
                requested += counterOf(s[1])
            } else {
                assertArrayEquals(HistoryMessages.endRequest(), s[1])
                assertTrue((next as HistoryFlow.Next.Send).done)
            }
        }
        assertEquals(HistoryMessages.COUNTERS, requested)
        assertEquals(HistoryFlow.Phase.DONE, f.phase)
    }

    @Test
    fun zeroPendingEndsWithoutCounterRequests() {
        val f = HistoryFlow()
        f.start()
        val next = f.onCount(HistoryMessages.ID_PENDING, 0)
        assertArrayEquals(HistoryMessages.endRequest(), sends(next).single())
        assertTrue((next as HistoryFlow.Next.Send).done)
        assertEquals(HistoryFlow.Phase.DONE, f.phase)
    }

    @Test
    fun recordLengthAboveReassemblyLimitFailsImmediately() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH, MAX_RECORD_BYTES + 1) is HistoryFlow.Next.Fail)
    }

    private fun storePiece(f: HistoryFlow, payload: ByteArray, seq: Int): HistoryFlow.Next.Store {
        val s = f.onRecord(payload, seq) as HistoryFlow.Next.Store
        f.onStored(true)
        return s
    }

    @Test
    fun recordArrivesInPiecesUntilAnnouncedLength() {
        // Kuzatilgan: 0x0007 = 1158, bo'laklar 4 × 256 + 134
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1158)
        f.onCount(HistoryMessages.ID_LENGTH, 1158)
        val parts = (0 until 4).map { storePiece(f, record(256), seq = 10 + it) }
        assertEquals(listOf(0, 1, 2, 3), parts.map { it.part })
        assertTrue(parts.none { it.complete })
        val last = f.onRecord(record(134), 14) as HistoryFlow.Next.Store
        assertTrue(last.complete)
        assertEquals(4, last.part)
        assertTrue(f.onStored(true) is HistoryFlow.Next.None)
        val s = sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 1158))
        assertArrayEquals(HistoryMessages.commitRequest(), s[0])
        assertEquals(HistoryMessages.COUNTERS[1], counterOf(s[1]))
    }

    @Test
    fun identicalPiecesWithNewSeqAreStoredRetransmitIsOnlyAcked() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 768)
        f.onCount(HistoryMessages.ID_LENGTH, 768)
        val zeros = ByteArray(256)
        storePiece(f, zeros, seq = 1)
        assertTrue(f.onRecord(zeros.copyOf(), 1) is HistoryFlow.Next.Ack)
        assertEquals(1, storePiece(f, zeros.copyOf(), seq = 2).part)
        assertEquals(2, storePiece(f, zeros.copyOf(), seq = 3).part)
    }

    @Test
    fun middlePieceOfOtherSizeOrOverflowStopsSync() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 600)
        f.onCount(HistoryMessages.ID_LENGTH, 600)
        assertTrue(f.onRecord(record(200), 1) is HistoryFlow.Next.Fail)

        val g = HistoryFlow()
        g.start()
        g.onCount(HistoryMessages.ID_PENDING, 300)
        g.onCount(HistoryMessages.ID_LENGTH, 300)
        storePiece(g, record(256), seq = 1)
        assertTrue(g.onRecord(record(256), 2) is HistoryFlow.Next.Fail)
    }

    @Test
    fun splitSizedRecordIsStored() {
        // Kuzatilgan: 256 bayt qiymat (261 bayt payload, 244 + 17 notify)
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 256)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH, 256) !is HistoryFlow.Next.Fail)
        assertTrue(f.onRecord(ByteArray(256)) is HistoryFlow.Next.Store)
    }

    @Test
    fun recordIsStoredThenCommitAndNextCounter() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 33)
        assertTrue(answerLength(f, 14) is HistoryFlow.Next.None)
        assertEquals(HistoryFlow.Phase.RECORD_WAIT, f.phase)

        val payload = record(14)
        val store = f.onRecord(payload) as HistoryFlow.Next.Store
        assertEquals(HistoryMessages.COUNTERS[0], store.counter)
        assertArrayEquals(payload, store.payload)
        assertEquals(HistoryFlow.Phase.STORING, f.phase)

        val s = sends(f.onStored(true))
        assertArrayEquals(HistoryMessages.commitRequest(), s[0])
        assertEquals(HistoryMessages.COUNTERS[1], counterOf(s[1]))
    }

    @Test
    fun recordBeforeSecondLengthIsAcceptedAndCommitWaitsForCopy() {
        // Qurilma tartibi: 0x0007 (N), 0x0030 (N bayt), keyin 0x0008 (N)
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 14)
        f.onCount(HistoryMessages.ID_LENGTH, 14)
        val payload = record(14)
        val store = f.onRecord(payload) as HistoryFlow.Next.Store
        assertArrayEquals(payload, store.payload)
        assertTrue(f.onStored(true) is HistoryFlow.Next.None)
        val s = sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 14))
        assertArrayEquals(HistoryMessages.commitRequest(), s[0])
        assertEquals(HistoryMessages.COUNTERS[1], counterOf(s[1]))
    }

    @Test
    fun duplicateRecordBeforeSecondLengthIsAckedWithoutStoring() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 9)
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        val payload = record(9)
        assertTrue(f.onRecord(payload) is HistoryFlow.Next.Store)
        f.onStored(true)
        // qayta yuborilgan yozuv yangi massiv bo'lib keladi: baytlar bir xil bo'lgani uchun Ack
        assertTrue(f.onRecord(payload.copyOf()) is HistoryFlow.Next.Ack)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.Send)
    }

    @Test
    fun differentSecondRecordForSameCounterIsNotAckedAndStopsSync() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 9)
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        assertTrue(f.onRecord(record(9)) is HistoryFlow.Next.Store)
        f.onStored(true)
        // hajmi bir xil, baytlari boshqa: bu yozuv saqlanmagan, ACK bo'lmasligi kerak
        val other = record(9).also { it[0] = 0x7F.toByte() }
        assertTrue(f.onRecord(other) is HistoryFlow.Next.Fail)
        assertEquals(HistoryFlow.Phase.FAILED, f.phase)
        // 0x0020 ham yuborilmaydi: soat yozuvni o'zida saqlab qoladi
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.None)
    }

    @Test
    fun secondRecordOfOtherSizeStopsSyncWithoutCommit() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 9)
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        f.onRecord(record(9))
        f.onStored(true)
        assertTrue(f.onRecord(record(8)) is HistoryFlow.Next.Fail)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.None)
    }

    @Test
    fun recordBeforeFirstLengthIsIgnored() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 9)
        assertTrue(f.onRecord(record(9)) is HistoryFlow.Next.None)
        assertEquals(HistoryFlow.Phase.COUNTER_WAIT, f.phase)
    }

    @Test
    fun unsavedRecordStopsWithoutCommit() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 9)
        answerLength(f, 9)
        f.onRecord(record(9))
        assertTrue(f.onStored(false) is HistoryFlow.Next.Fail)
        assertEquals(HistoryFlow.Phase.FAILED, f.phase)
    }

    @Test
    fun recordOfWrongSizeFailsWithoutStore() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 9)
        answerLength(f, 10)
        assertTrue(f.onRecord(record(9)) is HistoryFlow.Next.Fail)
        assertEquals(HistoryFlow.Phase.FAILED, f.phase)
    }

    @Test
    fun lengthsThatDisagreeAreRequestedOnceAgainThenFail() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        f.onCount(HistoryMessages.ID_LENGTH, 14)
        // birinchi mos kelmaslik: shu hisoblagich qayta so'raladi (ACK ham, 0x0020 ham yo'q)
        assertArrayEquals(HistoryMessages.counterRequest(HistoryMessages.COUNTERS[0]), sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9)).single())
        assertEquals(HistoryFlow.Phase.COUNTER_WAIT, f.phase)
        // qayta so'rovda ham mos kelmasa: to'xtaydi
        f.onCount(HistoryMessages.ID_LENGTH, 14)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.Fail)
        assertEquals(HistoryFlow.Phase.FAILED, f.phase)
    }

    @Test
    fun zeroLengthWithDifferentCopyIsRetriedThenFails() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        f.onCount(HistoryMessages.ID_LENGTH, 0)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.Send)
        f.onCount(HistoryMessages.ID_LENGTH, 0)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.Fail)
    }

    @Test
    fun mismatchThatAgreesOnRetryContinuesAndCommits() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 14))
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH, 9) is HistoryFlow.Next.None)
        assertTrue(f.onRecord(record(9)) is HistoryFlow.Next.Store)
        assertTrue(f.onStored(true) is HistoryFlow.Next.None)
        val s = sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9))
        assertArrayEquals(HistoryMessages.commitRequest(), s[0])
        assertEquals(HistoryMessages.COUNTERS[1], counterOf(s[1]))
    }

    @Test
    fun storedRecordIsOnlyAckedWhenRepeatedAfterRetry() {
        // Saqlangan yozuv retry'dan keyin takror kelsa: faqat ACK, qayta saqlanmaydi
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        val payload = record(9)
        assertTrue(f.onRecord(payload) is HistoryFlow.Next.Store)
        f.onStored(true)
        sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 14))
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        assertTrue(f.onRecord(payload.copyOf()) is HistoryFlow.Next.Ack)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH_COPY, 9) is HistoryFlow.Next.Send)
    }

    @Test
    fun differentRecordAfterRetryStopsWithoutAck() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        assertTrue(f.onRecord(record(9)) is HistoryFlow.Next.Store)
        f.onStored(true)
        sends(f.onCount(HistoryMessages.ID_LENGTH_COPY, 14))
        f.onCount(HistoryMessages.ID_LENGTH, 9)
        assertTrue(f.onRecord(ByteArray(9) { 0x7F }) is HistoryFlow.Next.Fail)
        assertEquals(HistoryFlow.Phase.FAILED, f.phase)
    }

    @Test
    fun oversizedRecordLengthFails() {
        val f = HistoryFlow()
        f.start()
        f.onCount(HistoryMessages.ID_PENDING, 1)
        assertTrue(f.onCount(HistoryMessages.ID_LENGTH, MAX_RECORD_BYTES + 1) is HistoryFlow.Next.Fail)
    }

    @Test
    fun recordWithoutRequestIsIgnored() {
        val f = HistoryFlow()
        assertTrue(f.onRecord(record(14)) is HistoryFlow.Next.None)
        f.start()
        assertTrue(f.onRecord(record(14)) is HistoryFlow.Next.None)
        assertEquals(HistoryFlow.Phase.PENDING_WAIT, f.phase)
    }

    @Test
    fun timeoutFailsOnlyWhileActive() {
        val f = HistoryFlow()
        assertTrue(f.onTimeout() is HistoryFlow.Next.None)
        f.start()
        assertTrue(f.onTimeout() is HistoryFlow.Next.Fail)
        assertEquals(HistoryFlow.Phase.FAILED, f.phase)
    }

    @Test
    fun resetAllowsRestart() {
        val f = HistoryFlow()
        f.start()
        f.reset()
        assertEquals(HistoryFlow.Phase.IDLE, f.phase)
        assertTrue(f.start() is HistoryFlow.Next.Send)
    }
}
