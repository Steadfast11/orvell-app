package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDateTime

class TimeCodecTest {
    // Spec v0.5 [T] vektorlari: qiymat (hex) -> mahalliy vaqt
    private val vectors = listOf(
        "6A971EEE" to LocalDateTime.of(2026, 10, 11, 17, 59, 46),
        "6A971EEF" to LocalDateTime.of(2026, 10, 11, 17, 59, 47),
        "6A9720B6" to LocalDateTime.of(2026, 10, 11, 18, 2, 54),
        "6A97214D" to LocalDateTime.of(2026, 10, 11, 18, 5, 13),
        "6A97214E" to LocalDateTime.of(2026, 10, 11, 18, 5, 14),
        "6A9733B5" to LocalDateTime.of(2026, 10, 11, 19, 14, 53),
        "6A9733B6" to LocalDateTime.of(2026, 10, 11, 19, 14, 54),
        "6A973420" to LocalDateTime.of(2026, 10, 11, 19, 16, 32),
        "6A9739C1" to LocalDateTime.of(2026, 10, 11, 19, 39, 1),
        "6A9739C5" to LocalDateTime.of(2026, 10, 11, 19, 39, 5),
        "6A96BB54" to LocalDateTime.of(2026, 10, 11, 11, 45, 20),
    )

    @Test
    fun decodeMatchesSpecVectors() {
        for ((value, expected) in vectors) {
            assertEquals("vektor $value", expected, TimeCodec.decode(hex(value)))
        }
    }

    @Test
    fun encodeMatchesSpecVectors() {
        for ((value, expected) in vectors) {
            assertArrayEquals("vektor $value", hex(value), TimeCodec.encode(expected))
        }
    }

    @Test
    fun setTimeWrapsValueInTlv() {
        // TYPE 02, ID 0001, VLEN 4, keyin 4 bayt qiymat
        assertArrayEquals(
            hex("02 00 01 00 04 6A 97 1E EE"),
            TimeMessages.setTime(LocalDateTime.of(2026, 10, 11, 17, 59, 46)),
        )
    }

    @Test
    fun encodeRejectsYearOutside2000To2063() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeCodec.encode(LocalDateTime.of(1999, 12, 31, 23, 59, 59))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TimeCodec.encode(LocalDateTime.of(2064, 1, 1, 0, 0, 0))
        }
    }

    @Test
    fun yearBoundsEncodeToKnownBytes() {
        assertArrayEquals(hex("00 42 00 00"), TimeCodec.encode(LocalDateTime.of(2000, 1, 1, 0, 0, 0)))
        assertArrayEquals(hex("FF 3F 7E FB"), TimeCodec.encode(LocalDateTime.of(2063, 12, 31, 23, 59, 59)))
    }

    @Test
    fun nanosecondsAreDropped() {
        assertArrayEquals(
            hex("6A 97 1E EE"),
            TimeCodec.encode(LocalDateTime.of(2026, 10, 11, 17, 59, 46, 999_000_000)),
        )
    }

    @Test
    fun decodeRejectsWrongSize() {
        assertThrows(WireFormatException::class.java) { TimeCodec.decode(hex("6A 97 1E")) }
        assertThrows(WireFormatException::class.java) { TimeCodec.decode(hex("6A 97 1E EE 00")) }
        assertThrows(WireFormatException::class.java) { TimeCodec.decode(ByteArray(0)) }
    }

    @Test
    fun decodeRejectsOutOfRangeFields() {
        // Oy 13 (6B571EEE): oy 1..12 oralig'ida emas
        assertThrows(WireFormatException::class.java) { TimeCodec.decode(hex("6B 57 1E EE")) }
        // Soniya 60 (6A971EFC): soniya 0..59 oralig'ida emas
        assertThrows(WireFormatException::class.java) { TimeCodec.decode(hex("6A 97 1E FC")) }
    }

    @Test
    fun roundTripOverDayEvery997Seconds() {
        var dt = LocalDateTime.of(2026, 10, 11, 0, 0, 0)
        val end = LocalDateTime.of(2026, 10, 11, 23, 59, 59)
        var count = 0
        while (!dt.isAfter(end)) {
            assertEquals(dt, TimeCodec.decode(TimeCodec.encode(dt)))
            dt = dt.plusSeconds(997)
            count++
        }
        // 00:00:00 dan 23:59:59 gacha, qadam 997 s: k = 0..86 (oxirgisi 23:49:02), jami 87 ta qiymat
        assertEquals(87, count)
    }
}
