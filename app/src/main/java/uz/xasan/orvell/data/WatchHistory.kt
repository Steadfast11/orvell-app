package uz.xasan.orvell.data

import uz.xasan.orvell.protocol.wire.HistoryRecords
import uz.xasan.orvell.protocol.wire.WireFormatException
import java.time.LocalDate
import java.time.ZoneId

/** Soat tarixidan talqin qilingan yozuvlar manbasi (eski versiyaning "watch" yozuvlaridan ajralib turadi). */
const val WATCH_HISTORY_SOURCE = "watch_hist"

/**
 * Xom tarix yozuvi -> Sample'lar. Soat vaqti mahalliy (telefon soatga mahalliy vaqtni yuboradi).
 * Noma'lum hisoblagich yoki buzilgan yozuv: bo'sh ro'yxat (xom yozuv baribir saqlangan qoladi).
 */
fun watchSamples(counter: Int, payload: ByteArray, zone: ZoneId): List<Sample> = try {
    when (counter) {
        HistoryRecords.COUNTER_ACTIVITY -> HistoryRecords.activity(payload).flatMap { a ->
            val start = at(a.date, a.minute, zone)
            val end = at(a.date, a.minute + a.intervalMinutes, zone)
            buildList {
                add(Sample(T.STEPS_BUCKET, start, end, a.steps.toDouble(), a.distanceCm.toDouble(), WATCH_HISTORY_SOURCE))
                if (a.calories > 0) add(Sample(T.KCAL_BUCKET, start, end, a.calories / 1000.0, null, WATCH_HISTORY_SOURCE))
            }
        }
        HistoryRecords.COUNTER_HEART_RATE -> HistoryRecords.heartRate(payload).map { h ->
            Sample(T.HR, at(h.date, h.minute, zone), at(h.date, h.minute + h.intervalMinutes, zone), h.bpm.toDouble(), null, WATCH_HISTORY_SOURCE)
        }
        else -> emptyList()
    }
} catch (e: WireFormatException) {
    emptyList()
}

private fun at(date: LocalDate, minute: Int, zone: ZoneId): Long =
    date.atStartOfDay().plusMinutes(minute.toLong()).atZone(zone).toInstant().toEpochMilli()
