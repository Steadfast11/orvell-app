package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Qiymatlar sun'iy: tuzilish o'z loglarimizdan (spec messages.md v0.5), shaxsiy son yozilmaydi.
class HistoryRecordsTest {
    // 2025-01-02: (25 shl 9) or (1 shl 5) or 2 = 0x3222; 2025-01-03 = 0x3223
    private val jan2 = LocalDate.of(2025, 1, 2)

    @Test
    fun dateIsBitPacked() {
        assertEquals(jan2, HistoryRecords.decodeDate(0x3222))
        assertEquals(LocalDate.of(2026, 12, 31), HistoryRecords.decodeDate((26 shl 9) or (12 shl 5) or 31))
    }

    @Test
    fun invalidDateIsRejected() {
        assertThrows(WireFormatException::class.java) { HistoryRecords.decodeDate((25 shl 9) or (13 shl 5) or 1) }
        assertThrows(WireFormatException::class.java) { HistoryRecords.decodeDate((25 shl 9) or (2 shl 5) or 30) }
    }

    @Test
    fun activityBlockGivesStepsDistanceCaloriesPerInterval() {
        // 2 yozuv, 10:00 (600 = 0x0258) dan, 5 daqiqa oraliq
        val p = hex("0002 3222 0258 0005  0064 1B58 0BB8  0000 0000 0000")
        val a = HistoryRecords.activity(p)
        assertEquals(
            listOf(
                HistoryRecords.Activity(jan2, 600, 5, steps = 100, distanceCm = 7000, calories = 3000),
                HistoryRecords.Activity(jan2, 605, 5, steps = 0, distanceCm = 0, calories = 0),
            ),
            a,
        )
    }

    @Test
    fun severalBlocksAcrossMidnight() {
        // 23:55 (1435 = 0x059B) da bitta yozuv, keyin ertasi kun 00:00 dan bitta yozuv
        val p = hex("0001 3222 059B 0005  000A 02BC 012C  0001 3223 0000 0005  0014 0578 0258")
        val a = HistoryRecords.activity(p)
        assertEquals(2, a.size)
        assertEquals(jan2 to 1435, a[0].date to a[0].minute)
        assertEquals(LocalDate.of(2025, 1, 3) to 0, a[1].date to a[1].minute)
        assertEquals(30, a.sumOf { it.steps })
    }

    @Test
    fun emptyBlockIsAllowed() {
        assertTrue(HistoryRecords.activity(hex("0000 3222 0258 0005")).isEmpty())
    }

    @Test
    fun heartRateSkipsZero() {
        val p = hex("0003 3222 0258 0005 48 00 50")
        assertEquals(
            listOf(HistoryRecords.HeartRate(jan2, 600, 5, 72), HistoryRecords.HeartRate(jan2, 610, 5, 80)),
            HistoryRecords.heartRate(p),
        )
    }

    @Test
    fun truncatedOrTrailingBytesAreRejected() {
        assertThrows(WireFormatException::class.java) { HistoryRecords.activity(hex("0002 3222 0258 0005 0064 1B58 0BB8")) }
        assertThrows(WireFormatException::class.java) { HistoryRecords.activity(hex("0001 3222 0258 0005 0064 1B58 0BB8 00")) }
        assertThrows(WireFormatException::class.java) { HistoryRecords.heartRate(hex("0001 3222")) }
    }

    @Test
    fun badTimeIsRejected() {
        assertThrows(WireFormatException::class.java) { HistoryRecords.heartRate(hex("0001 3222 05A0 0005 48")) }
        assertThrows(WireFormatException::class.java) { HistoryRecords.heartRate(hex("0001 3222 0258 0000 48")) }
    }

    @Test
    fun unknownCounterIsNotInterpreted() {
        assertEquals(null, HistoryRecords.recordSize(0x05))
        assertThrows(WireFormatException::class.java) { HistoryRecords.blocks(0x05, hex("0001 3222 0258 0005 7448")) }
    }
}
