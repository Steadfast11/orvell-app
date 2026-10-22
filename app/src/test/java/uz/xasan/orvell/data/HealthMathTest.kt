package uz.xasan.orvell.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HealthMathTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private fun at(d: Int, h: Int, m: Int = 0, month: Int = 10, zoneId: ZoneId = zone) =
        LocalDateTime.of(2026, month, d, h, m).atZone(zoneId).toInstant().toEpochMilli()

    private fun total(type: String, ms: Long, v: Int) = Sample(type, ms, ms, v.toDouble())
    private fun bucket(ms: Long, steps: Int, cm: Double? = null) = Sample(T.STEPS_BUCKET, ms, ms + 600_000, steps.toDouble(), cm)
    private fun stage(from: Long, to: Long, stage: Int) = Sample(T.SLEEP, from, to, stage.toDouble())

    // ---------------------------------------------------------------- kunlik jamlar

    @Test fun dailyTotalNeverDecreasesAfterWatchCounterReset() {
        // soat qayta bog'landi: hisoblagich 8000 dan 120 ga tushdi, keyin 900 gacha o'sdi
        val totals = listOf(total(T.STEPS_TOTAL, at(4, 12), 8000), total(T.STEPS_TOTAL, at(4, 13), 120), total(T.STEPS_TOTAL, at(4, 15), 900))
        assertEquals(8000, HealthMath.dailySteps(totals, List(24) { 0 }))
        assertEquals(8000, HealthMath.dailyMax(totals))
    }

    @Test fun dailyStepsFallBackToBucketsWhenTheyAreHigher() {
        val buckets = listOf(bucket(at(4, 9), 3000), bucket(at(4, 10), 2500))
        val hourly = HealthMath.hourlySteps(buckets, zone)
        assertEquals(5500, HealthMath.dailySteps(listOf(total(T.STEPS_TOTAL, at(4, 9), 3000)), hourly))
        assertEquals(5500, HealthMath.dailySteps(emptyList(), hourly))
        assertEquals(0, HealthMath.dailySteps(emptyList(), List(24) { 0 }))
    }

    @Test fun distanceUsesTotalOrBucketCentimetres() {
        val buckets = listOf(bucket(at(4, 9), 100, cm = 7000.0), bucket(at(4, 10), 100, cm = 8050.0))
        assertEquals(150, HealthMath.dailyDistance(emptyList(), buckets))
        assertEquals(4200, HealthMath.dailyDistance(listOf(total(T.DIST_TOTAL, at(4, 9), 4200), total(T.DIST_TOTAL, at(4, 10), 300)), buckets))
        assertEquals(0, HealthMath.dailyDistance(emptyList(), emptyList()))
    }

    @Test fun distanceNeverDropsBelowBucketsAfterCounterReset() {
        // bo'laklar 3000 m bergan; soat qayta bog'landi va nollangan jami 100 m keldi — kun masofasi kamaymaydi
        val buckets = listOf(bucket(at(4, 9), 100, cm = 150_000.0), bucket(at(4, 10), 100, cm = 150_000.0))
        assertEquals(3000, HealthMath.dailyDistance(emptyList(), buckets))
        assertEquals(3000, HealthMath.dailyDistance(listOf(total(T.DIST_TOTAL, at(4, 11), 100)), buckets))
    }

    @Test fun negativeTotalsAreIgnored() {
        assertEquals(0, HealthMath.dailyMax(listOf(total(T.KCAL_TOTAL, at(4, 9), -5))))
        assertEquals(0, HealthMath.dailyDistance(listOf(total(T.DIST_TOTAL, at(4, 9), -5)), emptyList()))
    }

    @Test fun hourlyBucketsUseLocalTime() {
        val hourly = HealthMath.hourlySteps(listOf(bucket(at(4, 0, 5), 10), bucket(at(4, 23, 50), 20), bucket(at(4, 23, 0), 5)), zone)
        assertEquals(24, hourly.size)
        assertEquals(10, hourly[0])
        assertEquals(25, hourly[23])
    }

    @Test fun hourlyBucketsSurviveDstFallBack() {
        // Berlin, 2026-10-25: 03:00 -> 02:00, soat 2 ikki marta keladi — 25 soatlik kun ham 24 ustunga tushadi
        val berlin = ZoneId.of("Europe/Berlin")
        val firstTwo = LocalDateTime.of(2026, 10, 25, 2, 30).atZone(berlin).withEarlierOffsetAtOverlap().toInstant().toEpochMilli()
        val secondTwo = LocalDateTime.of(2026, 10, 25, 2, 30).atZone(berlin).withLaterOffsetAtOverlap().toInstant().toEpochMilli()
        val hourly = HealthMath.hourlySteps(listOf(bucket(firstTwo, 100), bucket(secondTwo, 50)), berlin)
        assertEquals(150, hourly[2])
        assertEquals(150, hourly.sum())
    }

    // ---------------------------------------------------------------- uyqu

    @Test fun sleepPiecesWithShortGapsMergeIntoOneNight() {
        val rows = listOf(
            stage(at(3, 23), at(4, 1), HealthMath.STAGE_LIGHT),
            stage(at(4, 1), at(4, 2), HealthMath.STAGE_DEEP),
            // 1,5 soatlik bo'shliq: soat yozmagan, lekin o'sha tun
            stage(at(4, 3, 30), at(4, 5), HealthMath.STAGE_REM),
            stage(at(4, 5), at(4, 5, 20), HealthMath.STAGE_AWAKE),
        )
        val n = HealthMath.sleepNight(rows.shuffled())!!
        assertEquals(at(3, 23), n.bed)
        assertEquals(at(4, 5, 20), n.wake)
        assertEquals(120, n.lightMin)
        assertEquals(60, n.deepMin)
        assertEquals(90, n.remMin)
        assertEquals(20, n.awakeMin)
        assertEquals(270, n.asleepMin)
        assertEquals(380, n.inBedMin)
        assertEquals(4, n.spans.size)
    }

    @Test fun longestGroupWinsOverNap() {
        val rows = listOf(
            stage(at(3, 15), at(3, 15, 40), HealthMath.STAGE_LIGHT),   // kunduzgi mizg'ish (3 soatdan ko'p bo'shliq)
            stage(at(3, 23), at(4, 6), HealthMath.STAGE_LIGHT),
        )
        val n = HealthMath.sleepNight(rows)!!
        assertEquals(at(3, 23), n.bed)
        assertEquals(420, n.lightMin)
    }

    @Test fun gapIsMeasuredFromTheLatestEndInGroup() {
        // ikkinchi bo'lak birinchisining ichida tugaydi: bo'shliq eng kech tugashdan hisoblanadi
        val rows = listOf(
            stage(at(3, 22), at(4, 4), HealthMath.STAGE_LIGHT),
            stage(at(3, 23), at(4, 0), HealthMath.STAGE_DEEP),
            stage(at(4, 5, 30), at(4, 6), HealthMath.STAGE_REM),    // 4:00 dan 1,5 soat — o'sha tun
            stage(at(4, 9), at(4, 9, 30), HealthMath.STAGE_LIGHT),  // 6:00 dan 3 soat — boshqa guruh
        )
        val n = HealthMath.sleepNight(rows)!!
        assertEquals(at(3, 22), n.bed)
        assertEquals(at(4, 6), n.wake)
    }

    @Test fun noSleepRowsMeansNoNight() {
        assertNull(HealthMath.sleepNight(emptyList()))
    }

    @Test fun sleepWindowIsSixPmToSixPm() {
        val (from, to) = HealthMath.sleepWindow(LocalDate.of(2026, 10, 4), zone)
        assertEquals(at(3, 18), from)
        assertEquals(at(4, 18), to)
    }

    @Test fun restingHeartRateIsNightMedian() {
        val night = HealthMath.sleepNight(listOf(stage(at(3, 23), at(4, 6), HealthMath.STAGE_LIGHT)))!!
        val hr = listOf(80, 52, 55, 60, 120).mapIndexed { i, v -> Sample(T.HR, at(4, 1 + i), value = v.toDouble()) } +
            Sample(T.HR, at(4, 12), value = 140.0)  // tundan keyin — hisobga olinmaydi
        assertEquals(60, HealthMath.restingHeartRate(hr, night))
        assertNull(HealthMath.restingHeartRate(hr.take(2), night))
        assertNull(HealthMath.restingHeartRate(hr, null))
    }

    @Test fun invalidReadingsAreFiltered() {
        val hr = listOf(0.0, 24.0, 25.0, 180.0, 251.0).map { Sample(T.HR, 0, value = it) }
        assertEquals(listOf(25.0, 180.0), HealthMath.validHeartRate(hr).map { it.value })
        val spo2 = listOf(0.0, 69.0, 95.0, 100.0, 101.0).map { Sample(T.SPO2, 0, value = it) }
        assertEquals(listOf(95.0, 100.0), HealthMath.validSpo2(spo2).map { it.value })
    }
}
