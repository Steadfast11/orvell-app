package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertEquals
import org.junit.Test

class Crc16ArcTest {
    @Test
    fun standardCheckValue() {
        // V7: CRC-16/ARC standart tekshiruv qiymati
        assertEquals(0xBB3D, Crc16Arc.compute("123456789".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun emptyInputGivesZero() {
        assertEquals(0x0000, Crc16Arc.compute(ByteArray(0)))
    }

    @Test
    fun specPayloadCrcValues() {
        // V1, V4, V5, V6 payload'larining CRC'i sarlavhadagi qiymatlarga teng
        assertEquals(0xC455, Crc16Arc.compute(hex("03 00 15 00 00")))
        assertEquals(0xEB05, Crc16Arc.compute(hex("03 00 16 00 01 00")))
        assertEquals(0x93C5, Crc16Arc.compute(hex("03 00 14 00 01 01")))
        assertEquals(0xDB04, Crc16Arc.compute(hex("03 00 12 00 01 00")))
    }
}
