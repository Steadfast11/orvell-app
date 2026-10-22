package uz.xasan.orvell.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uz.xasan.orvell.data.Units
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Fmt: tilga qarab sana, son, davomiylik va "N daqiqa oldin" (resurslardagi qiymatlar bilan bir xil). */
class FmtTest {
    private val zone = ZoneId.of("Asia/Tashkent")

    private fun uz() = FmtText(
        locale = Locale.forLanguageTag("uz"),
        months = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"),
        monthsShort = listOf("yan", "fev", "mar", "apr", "may", "iyn", "iyl", "avg", "sen", "okt", "noy", "dek"),
        weekdays = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba"),
        weekdaysShort = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya"),
        datePattern = "%1\$d-%2\$s", dateYearPattern = "%1\$d-%2\$s %3\$d", dateShortPattern = "%1\$d %2\$s", weekdayDatePattern = "%1\$s, %2\$s",
        decimalSep = ',', groupSep = ' ',
        today = "Bugun", yesterday = "Kecha",
        unitMin = "daq", unitHour = "soat", durMin = "%1\$d daq", durHours = "%1\$d soat", durHoursMin = "%1\$d s %2\$d daq",
        unitM = "m", unitKm = "km", unitMi = "mi", unitCm = "sm", unitKg = "kg", unitLb = "lb",
        never = "hech qachon", justNow = "hozirgina",
        minutesAgo = { "$it daqiqa oldin" }, hoursAgo = { "$it soat oldin" }, daysAgo = { "$it kun oldin" },
        greetMorning = "Xayrli tong", greetDay = "Xayrli kun", greetEvening = "Xayrli kech", greetNight = "Xayrli tun",
    )

    /** Ruscha ko'plik qoidasi (one/few/many) — resurslardagi plurals'ga mos. */
    private fun ruPlural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10; val m100 = n % 100
        val form = when { m10 == 1 && m100 != 11 -> one; m10 in 2..4 && m100 !in 12..14 -> few; else -> many }
        return form.format(n)
    }

    private fun ru() = FmtText(
        locale = Locale.forLanguageTag("ru"),
        months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"),
        monthsShort = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"),
        weekdays = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"),
        weekdaysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
        datePattern = "%1\$d %2\$s", dateYearPattern = "%1\$d %2\$s %3\$d", dateShortPattern = "%1\$d %2\$s", weekdayDatePattern = "%1\$s, %2\$s",
        decimalSep = ',', groupSep = ' ',
        today = "Сегодня", yesterday = "Вчера",
        unitMin = "мин", unitHour = "ч", durMin = "%1\$d мин", durHours = "%1\$d ч", durHoursMin = "%1\$d ч %2\$d мин",
        unitM = "м", unitKm = "км", unitMi = "миль", unitCm = "см", unitKg = "кг", unitLb = "фнт",
        never = "никогда", justNow = "только что",
        minutesAgo = { ruPlural(it, "%d минуту назад", "%d минуты назад", "%d минут назад") },
        hoursAgo = { ruPlural(it, "%d час назад", "%d часа назад", "%d часов назад") },
        daysAgo = { ruPlural(it, "%d день назад", "%d дня назад", "%d дней назад") },
        greetMorning = "Доброе утро", greetDay = "Добрый день", greetEvening = "Добрый вечер", greetNight = "Доброй ночи",
    )

    private val en get() = FmtText(
        locale = Locale.ENGLISH,
        months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
        monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
        weekdaysShort = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"),
        datePattern = "%2\$s %1\$d", dateYearPattern = "%2\$s %1\$d, %3\$d", dateShortPattern = "%2\$s %1\$d", weekdayDatePattern = "%1\$s, %2\$s",
        decimalSep = '.', groupSep = ',',
        today = "Today", yesterday = "Yesterday",
        unitMin = "min", unitHour = "h", durMin = "%1\$d min", durHours = "%1\$d h", durHoursMin = "%1\$d h %2\$d min",
        unitM = "m", unitKm = "km", unitMi = "mi", unitCm = "cm", unitKg = "kg", unitLb = "lb",
        never = "never", justNow = "just now",
        minutesAgo = { if (it == 1) "1 minute ago" else "$it minutes ago" }, hoursAgo = { if (it == 1) "1 hour ago" else "$it hours ago" },
        daysAgo = { if (it == 1) "1 day ago" else "$it days ago" },
        greetMorning = "Good morning", greetDay = "Good afternoon", greetEvening = "Good evening", greetNight = "Good night",
    )

    @Before fun setUp() { Fmt.text = uz(); Fmt.zone = { zone } }
    @After fun tearDown() { Fmt.zone = { ZoneId.systemDefault() } }

    @Test fun groupsThousandsPerLanguage() {
        assertEquals("8 412", Fmt.int(8412))
        assertEquals("1 234 567", Fmt.int(1_234_567))
        assertEquals("999", Fmt.int(999))
        assertEquals("-12 000", Fmt.int(-12_000))
        assertEquals("0", Fmt.int(0))
        Fmt.text = en
        assertEquals("8,412", Fmt.int(8412))
        assertEquals("-2,147,483,648", Fmt.int(Int.MIN_VALUE))  // abs(MIN_VALUE) ham to'g'ri
    }

    @Test fun durations() {
        assertEquals("0 daq", Fmt.duration(0))
        assertEquals("0 daq", Fmt.duration(-5))
        assertEquals("45 daq", Fmt.duration(45))
        assertEquals("8 soat", Fmt.duration(480))
        assertEquals("5 s 31 daq", Fmt.duration(331))
        assertEquals("45" to "daq", Fmt.durationShort(45))
        assertEquals("7:05" to "soat", Fmt.durationShort(425))
        assertEquals("0" to "daq", Fmt.durationShort(-3))
        Fmt.text = ru()
        assertEquals("7 ч 30 мин", Fmt.duration(450))
    }

    @Test fun datesFollowLanguage() {
        val d = LocalDate.of(2026, 10, 4)
        assertEquals("4-oktabr", Fmt.date(d))
        assertEquals("4-oktabr 2026", Fmt.date(d, withYear = true))
        assertEquals("4 okt", Fmt.dateShort(d))
        Fmt.text = ru()
        assertEquals("4 октября", Fmt.date(d))
        assertEquals("4 окт", Fmt.dateShort(d))
        Fmt.text = en
        assertEquals("October 4, 2026", Fmt.date(d, withYear = true))
        assertEquals("Sunday, October 4", Fmt.fullDate(d, today = d))
    }

    @Test fun dayTitleUsesTodayYesterdayAndYear() {
        val today = LocalDate.of(2026, 10, 4)  // yakshanba
        assertEquals("Bugun", Fmt.dayTitle(today, today))
        assertEquals("Kecha", Fmt.dayTitle(today.minusDays(1), today))
        assertEquals("Payshanba, 1-oktabr", Fmt.dayTitle(LocalDate.of(2026, 10, 1), today))
        assertEquals("Chorshanba, 31-dekabr 2025", Fmt.dayTitle(LocalDate.of(2025, 12, 31), today))
        // yangi yil kechasi: kecha boshqa yilda bo'lsa ham "Kecha"
        assertEquals("Kecha", Fmt.dayTitle(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)))
        assertEquals("Ya", Fmt.weekdayShort(today))
    }

    @Test fun agoUsesPluralsAndClampsFuture() {
        val now = 1_800_000_000_000L
        assertEquals("hech qachon", Fmt.ago(0, now))
        assertEquals("hozirgina", Fmt.ago(now - 30_000, now))
        assertEquals("hozirgina", Fmt.ago(now + 120_000, now))  // soat vaqti oldinda bo'lsa manfiy chiqmasin
        assertEquals("5 daqiqa oldin", Fmt.ago(now - 5 * 60_000, now))
        assertEquals("3 soat oldin", Fmt.ago(now - 3 * 3_600_000, now))
        assertEquals("2 kun oldin", Fmt.ago(now - 2 * 86_400_000L, now))
        Fmt.text = ru()
        assertEquals("1 минуту назад", Fmt.ago(now - 60_000, now))
        assertEquals("3 минуты назад", Fmt.ago(now - 3 * 60_000, now))
        assertEquals("11 минут назад", Fmt.ago(now - 11 * 60_000, now))
        assertEquals("21 минуту назад", Fmt.ago(now - 21 * 60_000, now))
        assertEquals("22 часа назад", Fmt.ago(now - 22 * 3_600_000L, now))
    }

    @Test fun unitsAndDecimals() {
        assertEquals("850" to "m", Fmt.distance(850, Units.METRIC))
        assertEquals("5,25" to "km", Fmt.distance(5250, Units.METRIC))
        assertEquals("3,11" to "mi", Fmt.distance(5000, Units.IMPERIAL))
        assertEquals("65 kg", Fmt.weight(65f, Units.METRIC))
        assertEquals("65,5 kg", Fmt.weight(65.5f, Units.METRIC))
        assertEquals("143 lb", Fmt.weight(65f, Units.IMPERIAL))
        assertEquals("170 sm", Fmt.height(170, Units.METRIC))
        assertEquals("5′7″", Fmt.height(170, Units.IMPERIAL))
        Fmt.text = en
        assertEquals("5.25" to "km", Fmt.distance(5250, Units.METRIC))
        assertEquals("999" to "m", Fmt.distance(999, Units.METRIC))
    }

    @Test fun ratioAndPercentNeverNaN() {
        assertEquals(0.5f, Fmt.ratio(4000, 8000), 0.0001f)
        assertEquals(0f, Fmt.ratio(4000, 0), 0f)       // maqsad 0 bo'lsa ham halqa NaN bo'lmaydi
        assertEquals(0f, Fmt.ratio(4000, -10), 0f)
        assertEquals(0f, Fmt.ratio(-5, 100), 0f)
        assertEquals(1.25f, Fmt.ratio(10_000, 8000), 0.0001f)
        assertEquals("0%", Fmt.pct(Float.NaN))
        assertEquals("0%", Fmt.pct(Float.POSITIVE_INFINITY))
        assertEquals("42%", Fmt.pct(0.42f))
    }

    @Test fun timeUsesZoneAndClockSetting() {
        val ms = LocalDateTime.of(2026, 10, 4, 21, 5).atZone(zone).toInstant().toEpochMilli()
        assertEquals("21:05", Fmt.time(ms, clock24 = true))
        Fmt.text = en
        assertEquals("9:05 PM", Fmt.time(ms, clock24 = false))
    }

    @Test fun greetingByHour() {
        assertEquals("Xayrli tong", Fmt.greeting(LocalTime.of(7, 0)))
        assertEquals("Xayrli kun", Fmt.greeting(LocalTime.of(13, 0)))
        assertEquals("Xayrli kech", Fmt.greeting(LocalTime.of(19, 0)))
        assertEquals("Xayrli tun", Fmt.greeting(LocalTime.of(2, 0)))
    }
}
