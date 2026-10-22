package uz.xasan.orvell.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

// Sun'iy qiymatlar: 2025-01-02 = 0x3222 (sana bitlari, spec messages.md v0.5)
class WatchHistoryTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private fun ms(h: Int, m: Int, day: Int = 2) = LocalDateTime.of(2025, 1, day, h, m).atZone(zone).toInstant().toEpochMilli()
    private fun hex(s: String) = s.filterNot { it.isWhitespace() }.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun activityBecomesStepAndCalorieBuckets() {
        val s = watchSamples(1, hex("0002 3222 0258 0005  0064 1B58 0BB8  0000 0000 0000"), zone)
        assertEquals(
            listOf(
                Sample(T.STEPS_BUCKET, ms(10, 0), ms(10, 5), 100.0, 7000.0, WATCH_HISTORY_SOURCE),
                Sample(T.KCAL_BUCKET, ms(10, 0), ms(10, 5), 3.0, null, WATCH_HISTORY_SOURCE),
                Sample(T.STEPS_BUCKET, ms(10, 5), ms(10, 10), 0.0, 0.0, WATCH_HISTORY_SOURCE),
            ),
            s,
        )
    }

    @Test
    fun lastBucketOfDayEndsAtMidnight() {
        val s = watchSamples(1, hex("0001 3222 059B 0005 000A 02BC 0000"), zone).single()
        assertEquals(ms(23, 55), s.start)
        assertEquals(ms(0, 0, day = 3), s.end)
    }

    @Test
    fun heartRateUsesBucketTime() {
        val s = watchSamples(3, hex("0002 3222 0258 0005 48 00"), zone)
        assertEquals(listOf(Sample(T.HR, ms(10, 0), ms(10, 5), 72.0, null, WATCH_HISTORY_SOURCE)), s)
    }

    @Test
    fun unknownCounterOrBrokenRecordGivesNothing() {
        assertTrue(watchSamples(5, hex("0001 3222 0258 0005 7448"), zone).isEmpty())
        assertTrue(watchSamples(1, hex("0002 3222 0258 0005 0064"), zone).isEmpty())
    }

    @Test
    fun dailyStepsAreTheSumOfBuckets() {
        val buckets = watchSamples(1, hex("0003 3222 0258 0005 0064 0000 0000 00C8 0000 0000 0001 0000 0000"), zone)
            .filter { it.type == T.STEPS_BUCKET }
        assertEquals(301, HealthMath.dailySteps(emptyList(), HealthMath.hourlySteps(buckets, zone)))
    }
}
