package uz.xasan.orvell.ui

import uz.xasan.orvell.data.Units
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fmt uchun tilga bog'liq so'zlar. Ilovada string resurslardan yig'iladi (FmtRes.kt),
 * testlarda qo'lda beriladi — shuning uchun bu fayl Android'ga bog'liq emas.
 */
class FmtText(
    val locale: Locale,
    val months: List<String>,
    val monthsShort: List<String>,
    val weekdays: List<String>,
    val weekdaysShort: List<String>,
    /** %1$d = kun, %2$s = oy (%3$d = yil) */
    val datePattern: String,
    val dateYearPattern: String,
    val dateShortPattern: String,
    /** %1$s = hafta kuni, %2$s = sana */
    val weekdayDatePattern: String,
    val decimalSep: Char,
    val groupSep: Char,
    val today: String,
    val yesterday: String,
    val unitMin: String,
    val unitHour: String,
    val durMin: String,
    val durHours: String,
    val durHoursMin: String,
    val unitM: String,
    val unitKm: String,
    val unitMi: String,
    val unitCm: String,
    val unitKg: String,
    val unitLb: String,
    val never: String,
    val justNow: String,
    val minutesAgo: (Int) -> String,
    val hoursAgo: (Int) -> String,
    val daysAgo: (Int) -> String,
    val greetMorning: String,
    val greetDay: String,
    val greetEvening: String,
    val greetNight: String,
) {
    init {
        require(months.size == 12 && monthsShort.size == 12) { "12 ta oy kerak" }
        require(weekdays.size == 7 && weekdaysShort.size == 7) { "7 ta hafta kuni kerak" }
    }
}

/** Raqam, birlik va vaqt ko'rinishlari (bir joyda — butun ilovada bir xil, tanlangan tilda). */
object Fmt {
    /** Joriy til so'zlari: ilova ochilganda va til almashganda o'rnatiladi. */
    @Volatile lateinit var text: FmtText

    /** Previewlar uchun: so'zlar hali o'rnatilmaganmi. */
    val isReady: Boolean get() = ::text.isInitialized

    /** Vaqt mintaqasi (testlar uchun almashtiriladi). */
    @Volatile var zone: () -> ZoneId = { ZoneId.systemDefault() }

    private val t get() = text

    val weekdays: List<String> get() = t.weekdays
    val weekdaysShort: List<String> get() = t.weekdaysShort

    fun weekdayShort(d: LocalDate) = t.weekdaysShort[d.dayOfWeek.value - 1]

    /** 8412 -> "8 412" (ajratuvchi tilga qarab) */
    fun int(v: Int): String {
        val s = kotlin.math.abs(v.toLong()).toString()
        val out = StringBuilder()
        s.forEachIndexed { i, ch -> if (i > 0 && (s.length - i) % 3 == 0) out.append(t.groupSep); out.append(ch) }
        return (if (v < 0) "-" else "") + out
    }

    /** Kasr son: "%.2f" lekin o'nlik ajratuvchi tilga qarab. */
    fun dec(v: Double, digits: Int): String = String.format(Locale.ROOT, "%.${digits}f", v).replace('.', t.decimalSep)

    fun distance(m: Int, units: Units): Pair<String, String> = when (units) {
        Units.METRIC -> if (m < 1000) int(m) to t.unitM else dec(m / 1000.0, 2) to t.unitKm
        Units.IMPERIAL -> dec(m / 1609.344, 2) to t.unitMi
    }

    fun height(cm: Int, units: Units) = when (units) {
        Units.METRIC -> "$cm ${t.unitCm}"
        Units.IMPERIAL -> { val inch = Math.round(cm / 2.54f); "${inch / 12}′${inch % 12}″" }
    }

    fun weight(kg: Float, units: Units) = when (units) {
        Units.METRIC -> {
            val tenths = Math.round(kg * 10)
            (if (tenths % 10 == 0) "${tenths / 10}" else dec(tenths / 10.0, 1)) + " " + t.unitKg
        }
        Units.IMPERIAL -> "${Math.round(kg * 2.20462f)} ${t.unitLb}"
    }

    /** 331 -> "5 s 31 daq" */
    fun duration(min: Int): String = when {
        min <= 0 -> t.durMin.format(0)
        min < 60 -> t.durMin.format(min)
        min % 60 == 0 -> t.durHours.format(min / 60)
        else -> t.durHoursMin.format(min / 60, min % 60)
    }

    /** Katta raqam + birlik: 45 -> ("45", "daq"), 331 -> ("5:31", "soat") */
    fun durationShort(min: Int): Pair<String, String> {
        val m = min.coerceAtLeast(0)
        return if (m < 60) "$m" to t.unitMin else "${m / 60}:${"%02d".format(Locale.ROOT, m % 60)}" to t.unitHour
    }

    fun time(ms: Long, clock24: Boolean = true): String {
        val z = Instant.ofEpochMilli(ms).atZone(zone())
        return z.format(DateTimeFormatter.ofPattern(if (clock24) "HH:mm" else "h:mm a", t.locale))
    }

    fun date(d: LocalDate, withYear: Boolean = false): String =
        if (withYear) t.dateYearPattern.format(d.dayOfMonth, t.months[d.monthValue - 1], d.year)
        else t.datePattern.format(d.dayOfMonth, t.months[d.monthValue - 1])

    fun dateShort(d: LocalDate) = t.dateShortPattern.format(d.dayOfMonth, t.monthsShort[d.monthValue - 1])

    /** "Payshanba, 4-oktabr" (boshqa yil bo'lsa yil bilan). */
    fun fullDate(d: LocalDate, today: LocalDate = LocalDate.now(zone())): String =
        t.weekdayDatePattern.format(t.weekdays[d.dayOfWeek.value - 1], date(d, d.year != today.year))

    fun dayTitle(d: LocalDate, today: LocalDate = LocalDate.now(zone())): String = when (d) {
        today -> t.today
        today.minusDays(1) -> t.yesterday
        else -> fullDate(d, today)
    }

    fun ago(ms: Long, now: Long = System.currentTimeMillis()): String {
        if (ms <= 0) return t.never
        val m = ((now - ms) / 60_000).coerceAtLeast(0)
        return when {
            m < 1 -> t.justNow
            m < 60 -> t.minutesAgo(m.toInt())
            m < 24 * 60 -> t.hoursAgo((m / 60).toInt())
            else -> t.daysAgo((m / (24 * 60)).toInt())
        }
    }

    fun greeting(now: LocalTime = LocalTime.now(zone())): String = when (now.hour) {
        in 5..10 -> t.greetMorning
        in 11..16 -> t.greetDay
        in 17..21 -> t.greetEvening
        else -> t.greetNight
    }

    /** 0.42 -> "42%"; NaN/cheksiz -> "0%" */
    fun pct(v: Float) = "${if (v.isFinite()) Math.round(v * 100) else 0}%"

    /** Maqsadga nisbat: maqsad 0 yoki manfiy bo'lsa 0 (NaN/cheksizlik chizmaga tushmasin). */
    fun ratio(value: Number, goal: Number): Float {
        val g = goal.toDouble()
        val r = if (g > 0) value.toDouble() / g else 0.0
        return if (r.isFinite()) r.toFloat().coerceAtLeast(0f) else 0f
    }
}
