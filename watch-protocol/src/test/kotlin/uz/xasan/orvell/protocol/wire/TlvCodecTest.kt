package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TlvCodecTest {
    @Test
    fun decodeUnbindRequestPayload() {
        val tlv = TlvCodec.decode(hex("03 00 15 00 00"))
        assertEquals(0x03, tlv.type)
        assertEquals(0x0015, tlv.id)
        assertEquals(0, tlv.value.size)
    }

    @Test
    fun decodeWatchResponsePayloads() {
        val unbind = TlvCodec.decode(hex("03 00 16 00 01 00"))
        assertEquals(0x0016, unbind.id)
        assertArrayEquals(byteArrayOf(0x00), unbind.value)

        val bind = TlvCodec.decode(hex("03 00 12 00 01 00"))
        assertEquals(0x0012, bind.id)
        assertArrayEquals(byteArrayOf(0x00), bind.value)
    }

    @Test
    fun encodeMatchesWireLayout() {
        assertArrayEquals(hex("03 00 11 00 03 01 02 03"), TlvCodec.encode(0x03, 0x0011, hex("01 02 03")))
    }

    @Test
    fun encodeThenDecodeRoundTrip() {
        val tlv = TlvCodec.decode(TlvCodec.encode(0x02, 0xABCD, hex("ff 00 7f")))
        assertEquals(0x02, tlv.type)
        assertEquals(0xABCD, tlv.id)
        assertArrayEquals(hex("ff 00 7f"), tlv.value)
    }

    @Test
    fun decodeRejectsVlenMismatch() {
        // VLEN 2, lekin qiymat 1 bayt
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(hex("03 00 16 00 02 00")) }
        // VLEN 0, lekin ortiqcha bayt bor
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(hex("03 00 16 00 00 00")) }
    }

    @Test
    fun decodeRejectsShortPayload() {
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(hex("03 00 16 00")) }
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(ByteArray(0)) }
    }

    @Test
    fun encodeRejectsFieldOverflow() {
        assertThrows(IllegalArgumentException::class.java) { TlvCodec.encode(0x100, 0x0011, ByteArray(0)) }
        assertThrows(IllegalArgumentException::class.java) { TlvCodec.encode(0x03, 0x10000, ByteArray(0)) }
        assertThrows(IllegalArgumentException::class.java) { TlvCodec.encode(0x03, 0x0011, ByteArray(0x10000)) }
    }
}
