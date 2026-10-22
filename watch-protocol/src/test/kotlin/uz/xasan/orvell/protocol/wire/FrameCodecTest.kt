package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameCodecTest {
    @Test
    fun encodeUnbindFrameMatchesV1() {
        val frame = FrameCodec.encodeData(0x006A, BindMessages.unbindRequest())
        assertArrayEquals(hex("ab 00 00 05 c4 55 00 6a 03 00 15 00 00"), frame)
    }

    @Test
    fun encodeAckMatchesV2() {
        assertArrayEquals(hex("ab 10 00 00 00 00 1b 9a"), FrameCodec.encodeAck(0x1B9A))
    }

    @Test
    fun encodeAckAcceptsMaxSeq() {
        assertArrayEquals(hex("ab 10 00 00 00 00 ff ff"), FrameCodec.encodeAck(0xFFFF))
    }

    @Test
    fun parseWatchAckMatchesV3() {
        val header = FrameCodec.parseHeader(hex("ab 10 00 00 00 00 00 66"))
        assertEquals(FrameHeader(KIND_ACK, 0, 0x0000, 0x0066), header)
    }

    @Test
    fun parseHeaderRejectsWrongSize() {
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("ab 10 00 00 00 00 00")) }
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("ab 10 00 00 00 00 00 66 00")) }
    }

    @Test
    fun parseHeaderRejectsBadMagic() {
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("aa 10 00 00 00 00 00 66")) }
    }

    @Test
    fun parseHeaderRejectsUnknownKind() {
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("ab 20 00 00 00 00 00 01")) }
    }

    @Test
    fun parseHeaderRejectsAckWithLength() {
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("ab 10 00 05 00 00 00 01")) }
    }

    @Test
    fun parseHeaderRejectsZeroLengthWithCrc() {
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("ab 10 00 00 00 01 00 01")) }
    }

    @Test
    fun parseHeaderRejectsDataWithZeroLength() {
        assertThrows(WireFormatException::class.java) { FrameCodec.parseHeader(hex("ab 00 00 00 00 00 00 01")) }
    }

    @Test
    fun encodeDataRejectsEmptyPayload() {
        assertThrows(IllegalArgumentException::class.java) { FrameCodec.encodeData(1, ByteArray(0)) }
    }

    @Test
    fun encodeDataRejectsPayloadLargerThanLenField() {
        assertThrows(IllegalArgumentException::class.java) { FrameCodec.encodeData(1, ByteArray(0x10000)) }
    }

    @Test
    fun seqMustFitTwoBytes() {
        assertThrows(IllegalArgumentException::class.java) { FrameCodec.encodeData(-1, byteArrayOf(1)) }
        assertThrows(IllegalArgumentException::class.java) { FrameCodec.encodeData(0x10000, byteArrayOf(1)) }
        assertThrows(IllegalArgumentException::class.java) { FrameCodec.encodeAck(-1) }
        assertThrows(IllegalArgumentException::class.java) { FrameCodec.encodeAck(0x10000) }
    }

    @Test
    fun encodeParseAndValidateRoundTrip() {
        val payload = hex("03 00 15 00 00")
        val bytes = FrameCodec.encodeData(0x006A, payload)
        val header = FrameCodec.parseHeader(bytes.copyOfRange(0, FRAME_HEADER_SIZE))
        assertEquals(KIND_DATA, header.kind)
        assertEquals(payload.size, header.length)
        assertEquals(0x006A, header.seq)
        assertTrue(FrameCodec.isPayloadValid(header, bytes.copyOfRange(FRAME_HEADER_SIZE, bytes.size)))
    }

    @Test
    fun isPayloadValidDetectsWrongCrcAndLength() {
        val header = FrameHeader(KIND_DATA, 5, 0xC455, 0x006A)
        assertTrue(FrameCodec.isPayloadValid(header, hex("03 00 15 00 00")))
        assertFalse(FrameCodec.isPayloadValid(header, hex("03 00 15 00 01")))
        assertFalse(FrameCodec.isPayloadValid(header, hex("03 00 15 00")))
    }
}
