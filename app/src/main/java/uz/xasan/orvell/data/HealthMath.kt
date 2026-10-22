package uz.xasan.orvell.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Sof Kotlin: Android'ga bog'liq emas, JVM testlarida tekshiriladi (app/src/test).

/** Ma'lumot turlari (bazadagi `type`). */
object T {
    const val STEPS_TOTAL = "steps_total"       // soatning kunlik jami (start = kun boshi)
    const val DIST_TOTAL = "distance_total"     // metr
    const val KCAL_TOTAL = "kcal_total"         // kkal
    const val STEPS_BUCKET = "steps_bucket"     // qadam bo'lagi (soat tarixi: 5 daqiqa); extra = masofa (sm)
    const val KCAL_BUCKET = "kcal_bucket"       // kaloriya bo'lagi, kkal
    const val HR = "heart_rate"
    const val SPO2 = "spo2"
    const val BP = "blood_pressure"             // value = sistolik, extra = diastolik
    const val RESP = "respiratory_rate"
    const val SLEEP = "sleep_stage"             // value = bosqich (1 uyg'oq, 4 yengil, 5 chuqur, 6 REM)
    const val BATTERY = "battery"
}

data class Sample(
    val type: String,
    val start: Long,
    val end: Long = start,
    val value: Double,
    val extra: Double? = null,
    val source: String = "watch",
)

/** Uyqu bosqichi bo'lagi (gipnogramma uchun). */
data class StageSpan(val start: Long, val end: Long, val stage: Int)

data class SleepNight(
    val bed: Long,
    val wake: Long,
    val spans: List<StageSpan>,
    val deepMin: Int,
    val lightMin: Int,
    val remMin: Int,
    val awakeMin: Int,
) {
    val inBedMin get() = ((wake - bed) / 60_000).toInt()
    val asleepMin get() = deepMin + lightMin + remMin
}

data class DayStats(
    val date: LocalDate,
    val steps: Int,
    val distanceM: Int,
    val kcal: Int,
    val hourlySteps: List<Int>,
    val hr: List<Sample>,
    val hrAvg: Int?,
    val hrMin: Int?,
    val hrMax: Int?,
    val restingHr: Int?,
    val spo2: List<Sample>,
    val bp: List<Sample>,
    val resp: List<Sample>,
    val sleep: SleepNight?,
    /** Shu kun uchun qadam yozuvi bormi. False bo'lsa qadam/masofa/kkal "0" emas, "—" deb ko'rsatiladi. */
    val hasSteps: Boolean = true,
    /** Qadam telefon sensoridan olingan (soat emas). Masofa va kkal bu holatda o'lchanmagan: "—". */
    val stepsFromPhone: Boolean = false,
)

object HealthMath {
    /** Bir tun bo'laklari orasidagi ruxsat etilgan eng katta bo'shliq. */
    const val SLEEP_GAP_MS = 2 * 3_600_000L

    const val STAGE_AWAKE = 1
    const val STAGE_LIGHT = 4
    const val STAGE_DEEP = 5
    const val STAGE_REM = 6

    /** 10 daqiqalik qadam bo'laklari -> 24 soatlik ustunlar (mahalliy vaqt bo'yicha). */
    fun hourlySteps(buckets: List<Sample>, zone: ZoneId): List<Int> {
        val hourly = IntArray(24)
        for (s in buckets) hourly[Instant.ofEpochMilli(s.start).atZone(zone).hour] += s.value.toInt()
        return hourly.toList()
    }

    /**
     * Kunlik qadam jami hech qachon kamaymaydi: soat qayta bog'lansa uning hisoblagichi nollanadi va
     * kichikroq "jami" yuboradi — kun davomidagi eng katta qiymat olinadi. Bo'laklar yig'indisi
     * kattaroq bo'lsa (masalan jami hali kelmagan), o'sha olinadi.
     */
    fun dailySteps(totals: List<Sample>, hourly: List<Int>): Int = maxOf(dailyMax(totals), hourly.sum())

    /**
     * Masofa (m) ham qadam kabi kamaymaydi: soat jamining eng kattasi yoki bo'laklardagi masofa (sm) yig'indisi,
     * qaysi biri katta bo'lsa. Qayta bog'langandan keyingi kichik jami bo'laklardan olingan masofani kamaytirmaydi.
     */
    fun dailyDistance(totals: List<Sample>, buckets: List<Sample>): Int =
        maxOf(dailyMax(totals), (buckets.sumOf { it.extra ?: 0.0 } / 100).toInt())

    /** Kun ichidagi jamlardan eng kattasi (kamaymaydigan hisoblagich). */
    fun dailyMax(totals: List<Sample>): Int = totals.maxOfOrNull { it.value.toInt() }?.coerceAtLeast(0) ?: 0

    fun validHeartRate(hr: List<Sample>) = hr.filter { it.value in 25.0..250.0 }

    fun validSpo2(v: List<Sample>) = v.filter { it.value in 70.0..100.0 }

    /** Shu kun ertalab tugagan tun oynasi: kechagi 18:00 dan bugungi 18:00 gacha (mahalliy vaqt). */
    fun sleepWindow(d: LocalDate, zone: ZoneId): Pair<Long, Long> =
        d.minusDays(1).atTime(18, 0).atZone(zone).toInstant().toEpochMilli() to d.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()

    /**
     * Soat tunni bo'laklab yozadi: 2 soatgacha bo'shliqli bo'laklar bitta tunga birlashtiriladi,
     * bir nechta guruh bo'lsa eng uzuni olinadi (masalan kunduzgi mizg'ish tun hisoblanmaydi).
     */
    fun sleepNight(rows: List<Sample>): SleepNight? {
        val sorted = rows.sortedBy { it.start }
        if (sorted.isEmpty()) return null
        val groups = mutableListOf(mutableListOf(sorted.first()))
        var groupEnd = sorted.first().end
        for (r in sorted.drop(1)) {
            if (r.start - groupEnd <= SLEEP_GAP_MS) groups.last().add(r) else groups.add(mutableListOf(r))
            groupEnd = if (groups.last().size == 1) r.end else maxOf(groupEnd, r.end)
        }
        val g = groups.maxBy { grp -> grp.maxOf { it.end } - grp.minOf { it.start } }
        fun mins(stage: Int) = (g.filter { it.value.toInt() == stage }.sumOf { it.end - it.start } / 60_000).toInt()
        return SleepNight(
            bed = g.minOf { it.start }, wake = g.maxOf { it.end },
            spans = g.map { StageSpan(it.start, it.end, it.value.toInt()) },
            deepMin = mins(STAGE_DEEP), lightMin = mins(STAGE_LIGHT), remMin = mins(STAGE_REM), awakeMin = mins(STAGE_AWAKE),
        )
    }

    /** Uyqudagi yurak urishi: tun davomidagi o'lchovlar medianasi (kamida 3 ta o'lchov). */
    fun restingHeartRate(hr: List<Sample>, night: SleepNight?): Int? {
        if (night == null) return null
        val v = hr.filter { it.start in night.bed..night.wake }.map { it.value }.sorted()
        return if (v.size >= 3) v[v.size / 2].toInt() else null
    }
}
