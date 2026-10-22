package uz.xasan.orvell.ui

import android.content.res.Resources
import uz.xasan.orvell.R

/** Fmt so'zlarini joriy til resurslaridan yig'adi (Activity/Service konteksti tilini oladi). */
fun fmtTextFrom(res: Resources): FmtText {
    fun s(id: Int) = res.getString(id)
    fun ch(id: Int) = res.getString(id).firstOrNull() ?: ' '
    return FmtText(
        locale = res.configuration.locales[0],
        months = res.getStringArray(R.array.fmt_months).toList(),
        monthsShort = res.getStringArray(R.array.fmt_months_short).toList(),
        weekdays = res.getStringArray(R.array.fmt_weekdays).toList(),
        weekdaysShort = res.getStringArray(R.array.fmt_weekdays_short).toList(),
        datePattern = s(R.string.fmt_date),
        dateYearPattern = s(R.string.fmt_date_year),
        dateShortPattern = s(R.string.fmt_date_short),
        weekdayDatePattern = s(R.string.fmt_weekday_date),
        decimalSep = ch(R.string.fmt_decimal_sep),
        groupSep = ch(R.string.fmt_group_sep),
        today = s(R.string.fmt_today),
        yesterday = s(R.string.fmt_yesterday),
        unitMin = s(R.string.fmt_unit_min),
        unitHour = s(R.string.fmt_unit_hour),
        durMin = s(R.string.fmt_dur_min),
        durHours = s(R.string.fmt_dur_hours),
        durHoursMin = s(R.string.fmt_dur_hm),
        unitM = s(R.string.fmt_unit_m),
        unitKm = s(R.string.fmt_unit_km),
        unitMi = s(R.string.fmt_unit_mi),
        unitCm = s(R.string.fmt_unit_cm),
        unitKg = s(R.string.fmt_unit_kg),
        unitLb = s(R.string.fmt_unit_lb),
        never = s(R.string.fmt_never),
        justNow = s(R.string.fmt_just_now),
        minutesAgo = { n -> res.getQuantityString(R.plurals.fmt_minutes_ago, n, n) },
        hoursAgo = { n -> res.getQuantityString(R.plurals.fmt_hours_ago, n, n) },
        daysAgo = { n -> res.getQuantityString(R.plurals.fmt_days_ago, n, n) },
        greetMorning = s(R.string.fmt_greet_morning),
        greetDay = s(R.string.fmt_greet_day),
        greetEvening = s(R.string.fmt_greet_evening),
        greetNight = s(R.string.fmt_greet_night),
    )
}
