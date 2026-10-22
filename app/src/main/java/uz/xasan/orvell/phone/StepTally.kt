package uz.xasan.orvell.phone

import java.time.LocalDate

/**
 * Telefon qadam hisoblagichi (TYPE_STEP_COUNTER) dan kunlik jami. Hisoblagich qurilma yonganidan beri o'sadi,
 * bu yerda faqat o'sish yig'iladi. Yangi kunda hisob shu paytdan boshlanadi. Qurilma qayta yonsa (hisoblagich
 * kamayadi), yangi qiymat to'liq qo'shiladi. Sof mantiq: JVM testida tekshiriladi.
 */
data class StepTally(val date: LocalDate, val total: Int, val last: Long) {
    fun add(counter: Long, today: LocalDate): StepTally = when {
        date != today -> StepTally(today, 0, counter)
        counter < last -> copy(total = total + counter.toInt(), last = counter)
        else -> copy(total = total + (counter - last).toInt(), last = counter)
    }
}
