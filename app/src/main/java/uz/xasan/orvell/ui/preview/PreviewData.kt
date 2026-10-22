package uz.xasan.orvell.ui.preview

import uz.xasan.orvell.data.AppSettings
import uz.xasan.orvell.data.DayStats
import uz.xasan.orvell.data.Goals
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.data.Profile
import uz.xasan.orvell.data.Sample
import uz.xasan.orvell.data.Sex
import uz.xasan.orvell.data.SleepNight
import uz.xasan.orvell.data.StageSpan
import uz.xasan.orvell.data.T
import uz.xasan.orvell.data.WatchCfg
import uz.xasan.orvell.watch.Conn
import uz.xasan.orvell.watch.WatchState
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sin

/** Previewlar uchun soxta, ammo real ko'rinishdagi ma'lumot (faqat Android Studio ko'rinishida ishlatiladi). */
object PreviewData {
    private val zone = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.of(2026, 10, 4)
    fun at(h: Int, m: Int = 0, d: LocalDate = today) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    val settings = AppSettings(
        onboarded = true,
        profile = Profile(name = "Aziz", sex = Sex.MALE, birthDate = LocalDate.of(1996, 5, 12), heightCm = 176, weightKg = 72f, avatar = 1),
        goals = Goals(steps = 8000, sleepMin = 480, kcal = 350, distanceM = 5000),
        watch = WatchCfg(address = "preview-device", name = "GL-SW26", bindId = "orvell-preview"),
    )

    val watch = WatchState(conn = Conn.CONNECTED, battery = 76, charging = false, historyAt = System.currentTimeMillis() - 4 * 60_000, historyNew = 12)

    private val hr: List<Sample> = (0 until 60).map { i ->
        val t = at(0) + i * 20 * 60_000L
        Sample(T.HR, t, value = 62.0 + 18 * sin(i / 6.0) + (if (i in 40..44) 35 else 0))
    }

    val night = SleepNight(
        bed = at(23, 10, today.minusDays(1)), wake = at(7, 5),
        spans = listOf(
            StageSpan(at(23, 10, today.minusDays(1)), at(23, 50, today.minusDays(1)), 4),
            StageSpan(at(23, 50, today.minusDays(1)), at(1, 0), 5),
            StageSpan(at(1, 0), at(1, 40), 4),
            StageSpan(at(1, 40), at(2, 15), 6),
            StageSpan(at(2, 15), at(2, 25), 1),
            StageSpan(at(2, 25), at(3, 40), 5),
            StageSpan(at(3, 40), at(5, 0), 4),
            StageSpan(at(5, 0), at(5, 50), 6),
            StageSpan(at(5, 50), at(7, 5), 4),
        ),
        deepMin = 145, lightMin = 230, remMin = 85, awakeMin = 10,
    )

    val day = DayStats(
        date = today, steps = 6240, distanceM = 4520, kcal = 268,
        hourlySteps = listOf(0, 0, 0, 0, 0, 0, 120, 640, 980, 410, 220, 300, 760, 540, 180, 260, 420, 880, 0, 0, 0, 0, 0, 0).let { h -> h.mapIndexed { i, v -> if (i > 17) 0 else v } },
        hr = hr, hrAvg = 68, hrMin = 51, hrMax = 118, restingHr = 56,
        spo2 = listOf(97.0, 96.0, 98.0, 95.0).mapIndexed { i, v -> Sample(T.SPO2, at(2 + i * 4), value = v) },
        bp = listOf(Sample(T.BP, at(9, 30), value = 118.0, extra = 76.0)),
        resp = listOf(Sample(T.RESP, at(9, 30), value = 15.0)),
        sleep = night,
    )

    val emptyDay = DayStats(today, 0, 0, 0, List(24) { 0 }, emptyList(), null, null, null, null, emptyList(), emptyList(), emptyList(), null)

    fun series(days: Int): List<Health.DayPoint> = (days - 1 downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        val k = (days - i)
        Health.DayPoint(
            d, steps = (5200 + 3400 * sin(k * 0.9)).toInt().coerceAtLeast(900), distanceM = 4000, kcal = 250,
            sleepMin = 400 + (40 * sin(k * 1.3)).toInt(), inBedMin = 450, hrAvg = 66 + (k % 4), restingHr = 55 + (k % 3), spo2Avg = 96, respAvg = 15,
        )
    }

    val week = series(7)
}
