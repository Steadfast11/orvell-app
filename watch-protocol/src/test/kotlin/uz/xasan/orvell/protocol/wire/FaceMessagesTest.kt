package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Tuzilish o'z logimizdan (spec watchface.md §4).
class FaceMessagesTest {
    @Test
    fun prepareRoundTrip() {
        assertArrayEquals(hex("01 0001 0000"), FaceMessages.prepareRequest())
        assertTrue(FaceMessages.decodePrepared(TlvCodec.decode(hex("01 0002 0002 0000"))))
        assertFalse(FaceMessages.decodePrepared(TlvCodec.decode(hex("01 0002 0002 0001"))))
        assertThrows(WireFormatException::class.java) { FaceMessages.decodePrepared(TlvCodec.decode(hex("01 0002 0001 00"))) }
    }

    @Test
    fun currentDialIsReadFromOffsetFour() {
        assertArrayEquals(hex("02 0052 0000"), FaceMessages.dialListRequest())
        val custom = TlvCodec.decode(hex("02 0053 000A 0e75 0001 c520 0105 1601"))
        assertEquals(FaceMessages.CUSTOM_DIAL_ID, FaceMessages.decodeCurrentDial(custom))
        val other = TlvCodec.decode(hex("02 0053 0008 0e75 0001 bd56 0105"))
        assertEquals(0xBD56, FaceMessages.decodeCurrentDial(other))
        assertThrows(WireFormatException::class.java) { FaceMessages.decodeCurrentDial(TlvCodec.decode(hex("02 0053 0004 0e75 0001"))) }
    }

    @Test
    fun customConfigMatchesCapture() {
        assertArrayEquals(hex("02 0070 0003 05 01 00"), FaceMessages.customConfig())
    }
}
