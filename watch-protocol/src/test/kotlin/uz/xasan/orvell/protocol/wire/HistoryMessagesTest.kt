package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMessagesTest {
    @Test
    fun startIsEmptyTlv() {
        assertArrayEquals(hex("05 00 05 00 00"), HistoryMessages.startRequest())
    }

    @Test
    fun counterCarriesOneByte() {
        assertArrayEquals(hex("05 00 01 00 01 01"), HistoryMessages.counterRequest(0x01))
        assertArrayEquals(hex("05 00 01 00 01 83"), HistoryMessages.counterRequest(0x83))
    }

    @Test
    fun counterOutsideOneByteIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { HistoryMessages.counterRequest(0x100) }
    }

    @Test
    fun commitCarriesZero() {
        assertArrayEquals(hex("05 00 20 00 01 00"), HistoryMessages.commitRequest())
    }

    @Test
    fun endIsEmptyTlv() {
        assertArrayEquals(hex("05 00 21 00 00"), HistoryMessages.endRequest())
    }

    @Test
    fun countIsBigEndian() {
        // Spec v0.4: 0x0006 = 00000021 (33)
        assertEquals(33, HistoryMessages.decodeCount(TlvCodec.decode(hex("05 00 06 00 04 00 00 00 21"))))
    }

    @Test
    fun countRejectsWrongSize() {
        assertThrows(WireFormatException::class.java) {
            HistoryMessages.decodeCount(TlvCodec.decode(hex("05 00 07 00 02 00 0E")))
        }
    }

    @Test
    fun countersKeepObservedOrder() {
        assertEquals(listOf(0x01, 0x02, 0x03, 0x83, 0x04, 0x84, 0x05, 0x85, 0x10), HistoryMessages.COUNTERS)
    }

    @Test
    fun countersCannotBeModifiedThroughCast() {
        // Umumiy jadval: cast yoki Java orqali ham o'zgarmaydi
        val asMutable = HistoryMessages.COUNTERS as MutableList<Int>
        val result = runCatching { asMutable[0] = 0x99 }
        if (result.isSuccess) asMutable[0] = 0x01 // boshqa testlar buzilmasin
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
        assertEquals(0x01, HistoryMessages.COUNTERS[0])
    }
}
