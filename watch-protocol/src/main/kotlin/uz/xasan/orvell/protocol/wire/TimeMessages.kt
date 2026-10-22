package uz.xasan.orvell.protocol.wire

import java.time.DateTimeException
import java.time.LocalDateTime

/**
 * Vaqt qiymati (TYPE 0x02, ID 0x0001, VLEN 4, BE). Spec v0.5 [T]: mahalliy devor vaqti, maydonlar MSB dan:
 * yil-2000 (6 bit) | oy (4) | kun (5) | soat (5) | daqiqa (6) | soniya (6).
 */
object TimeCodec {
    private const val VALUE_SIZE = 4
    private const val FIRST_YEAR = 2000
    private const val LAST_YEAR = 2063

    /** Vaqtni 4 baytga kodlaydi. Yil 2000..2063 dan tashqari bo'lsa IllegalArgumentException. Nanosekundlar tashlanadi. */
    fun encode(dt: LocalDateTime): ByteArray {
        require(dt.year in FIRST_YEAR..LAST_YEAR) { "Year must be in $FIRST_YEAR..$LAST_YEAR: ${dt.year}" }
        val packed = ((dt.year - FIRST_YEAR) shl 26) or
            (dt.monthValue shl 22) or
            (dt.dayOfMonth shl 17) or
            (dt.hour shl 12) or
            (dt.minute shl 6) or
            dt.second
        return byteArrayOf(
            (packed ushr 24).toByte(),
            (packed ushr 16).toByte(),
            (packed ushr 8).toByte(),
            packed.toByte(),
        )
    }

    /** 4 baytni vaqtga dekodlaydi. Hajm 4 emas, yoki maydon oralig'dan tashqari (masalan oy 13) bo'lsa WireFormatException. */
    fun decode(value: ByteArray): LocalDateTime {
        if (value.size != VALUE_SIZE) {
            throw WireFormatException("Time value must be $VALUE_SIZE bytes, got ${value.size}")
        }
        val packed = ((value[0].toInt() and 0xFF) shl 24) or
            ((value[1].toInt() and 0xFF) shl 16) or
            ((value[2].toInt() and 0xFF) shl 8) or
            (value[3].toInt() and 0xFF)
        return try {
            LocalDateTime.of(
                FIRST_YEAR + ((packed ushr 26) and 0x3F),
                (packed ushr 22) and 0x0F,
                (packed ushr 17) and 0x1F,
                (packed ushr 12) and 0x1F,
                (packed ushr 6) and 0x3F,
                packed and 0x3F,
            )
        } catch (e: DateTimeException) {
            throw WireFormatException("Time fields out of range: ${e.message}")
        }
    }
}

/** Vaqt xabarlari (TYPE 0x02). Faqat telefon -> soat. Soat vaqt yozuviga ma'lumot javobi yubormaydi (faqat ACK) [T]. */
object TimeMessages {
    const val TYPE = 0x02
    const val ID_TIME = 0x0001

    /** Vaqt yozuvi TLV: 02 00 01 00 04 + 4 bayt. Javob kutilmaydi. */
    fun setTime(dt: LocalDateTime): ByteArray = TlvCodec.encode(TYPE, ID_TIME, TimeCodec.encode(dt))
}
