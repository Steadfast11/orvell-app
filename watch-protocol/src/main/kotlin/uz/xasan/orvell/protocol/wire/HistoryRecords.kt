package uz.xasan.orvell.protocol.wire

import java.time.LocalDate

/**
 * Tarix yozuvi (0x0030) ichidagi bloklar. Spec messages.md, "05/0030 bloklari" (v0.5), 2026-10-12/13 o'z loglarimizdan.
 *
 *  blok: [soni:2 BE][sana:2][boshlanish daqiqasi:2 BE][oraliq daqiqa:2 BE] + soni × yozuv
 *  sana: bitlar yyyyyyy mmmm ddddd (yil - 2000, oy, kun)
 *  bitta 0x0030 ichida bir nechta blok ketma-ket keladi (masalan yarim tundan o'tganda ikki kun).
 *
 * Yozuv hajmi hisoblagichga bog'liq: 0x01 → 6 bayt (qadam, masofa sm, kaloriya), 0x03 → 1 bayt (puls).
 */
object HistoryRecords {
    const val COUNTER_ACTIVITY = 0x01
    const val COUNTER_HEART_RATE = 0x03

    private const val HEADER = 8

    /** Hajmi ma'lum hisoblagichlar. Qolganlari talqin qilinmaydi. */
    fun recordSize(counter: Int): Int? = when (counter) {
        COUNTER_ACTIVITY -> 6
        COUNTER_HEART_RATE -> 1
        else -> null
    }

    class Block(val date: LocalDate, val startMinute: Int, val intervalMinutes: Int, val records: List<ByteArray>)

    /** Faollik bo'lagi. distanceCm va calories soatning o'z birligida (sm, kaloriya = kkal × 1000). */
    data class Activity(val date: LocalDate, val minute: Int, val intervalMinutes: Int, val steps: Int, val distanceCm: Int, val calories: Int)

    data class HeartRate(val date: LocalDate, val minute: Int, val intervalMinutes: Int, val bpm: Int)

    /** Sana maydoni: yuqori 7 bit yil (2000 dan), 4 bit oy, 5 bit kun. Yaroqsiz bo'lsa WireFormatException. */
    fun decodeDate(raw: Int): LocalDate {
        val year = 2000 + (raw ushr 9 and 0x7F)
        val month = raw ushr 5 and 0x0F
        val day = raw and 0x1F
        return try {
            LocalDate.of(year, month, day)
        } catch (e: java.time.DateTimeException) {
            throw WireFormatException("Bad history date: 0x${raw.toString(16)}")
        }
    }

    /** Yozuvni bloklarga ajratadi. Hajm mos kelmasa (qoldiq bayt, qisqa blok) WireFormatException. */
    fun blocks(counter: Int, payload: ByteArray): List<Block> {
        val size = recordSize(counter) ?: throw WireFormatException("Unknown history counter: $counter")
        val out = ArrayList<Block>()
        var p = 0
        while (p < payload.size) {
            if (payload.size - p < HEADER) throw WireFormatException("Short history block at $p")
            val count = u16(payload, p)
            val date = decodeDate(u16(payload, p + 2))
            val minute = u16(payload, p + 4)
            val interval = u16(payload, p + 6)
            if (minute > 1439 || interval !in 1..1440) throw WireFormatException("Bad history block time at $p")
            p += HEADER
            if (payload.size - p < count * size) throw WireFormatException("History block overruns payload at $p")
            out += Block(date, minute, interval, List(count) { i -> payload.copyOfRange(p + i * size, p + (i + 1) * size) })
            p += count * size
        }
        return out
    }

    fun activity(payload: ByteArray): List<Activity> =
        blocks(COUNTER_ACTIVITY, payload).flatMap { b ->
            b.records.mapIndexed { i, r ->
                Activity(b.date, b.startMinute + i * b.intervalMinutes, b.intervalMinutes, u16(r, 0), u16(r, 2), u16(r, 4))
            }
        }

    /** 0 = o'lchov yo'q: chiqarilmaydi. */
    fun heartRate(payload: ByteArray): List<HeartRate> =
        blocks(COUNTER_HEART_RATE, payload).flatMap { b ->
            b.records.mapIndexedNotNull { i, r ->
                val bpm = r[0].toInt() and 0xFF
                if (bpm == 0) null else HeartRate(b.date, b.startMinute + i * b.intervalMinutes, b.intervalMinutes, bpm)
            }
        }

    private fun u16(b: ByteArray, at: Int) = ((b[at].toInt() and 0xFF) shl 8) or (b[at + 1].toInt() and 0xFF)
}
