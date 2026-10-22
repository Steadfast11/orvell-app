package uz.xasan.orvell.protocol.wire

import java.util.Collections

/**
 * Tarix sinxroni xabarlari (TYPE 0x05). Spec messages.md: "Tarix sinxroni (TYPE 0x05)" (v0.3, v0.4).
 * Ketma-ketlik FitCloudPro sinxron loglarida [T] kuzatilgan. Hisoblagich va yozuv ma'nosi [N]:
 * kod ularni talqin qilmaydi, yozuv baytlari xom holda saqlanadi.
 *
 *  P 0x0005 → W 0x0006 (kutilayotgan jami bayt, 4 bayt BE)
 *  har hisoblagich c uchun: P 0x0001 (c) → W 0x0007 va 0x0008 (N, teng); N > 0 bo'lsa W 0x0030 (N bayt)
 *  W 0x0030 tartibi qat'iy emas: u 0x0008 dan oldin ham kelishi kuzatilgan (ilova qatlami buni hisobga oladi)
 *  keyingi c dan oldin va 0x0021 dan oldin: P 0x0020 (00)
 *  yakun: P 0x0021
 */
object HistoryMessages {
    const val TYPE = 0x05
    const val ID_START = 0x0005
    const val ID_COUNTER = 0x0001
    const val ID_PENDING = 0x0006
    const val ID_LENGTH = 0x0007
    const val ID_LENGTH_COPY = 0x0008
    const val ID_RECORD = 0x0030
    const val ID_COMMIT = 0x0020
    const val ID_END = 0x0021

    /** Hisoblagich qiymatlari, kuzatilgan tartibda [T]. Ma'nosi [N]. Ro'yxat o'zgarmas (oddiy listOf cast orqali o'zgarar edi). */
    val COUNTERS: List<Int> =
        Collections.unmodifiableList(listOf(0x01, 0x02, 0x03, 0x83, 0x04, 0x84, 0x05, 0x85, 0x10))

    fun startRequest(): ByteArray = TlvCodec.encode(TYPE, ID_START, ByteArray(0))

    fun counterRequest(counter: Int): ByteArray {
        require(counter in 0..0xFF) { "Counter must fit in 1 byte: $counter" }
        return TlvCodec.encode(TYPE, ID_COUNTER, byteArrayOf(counter.toByte()))
    }

    /** Oldingi hisoblagich yozuvlari qabul qilindi (00). Soat shundan keyin tarixni o'chirishi mumkin [N]. */
    fun commitRequest(): ByteArray = TlvCodec.encode(TYPE, ID_COMMIT, byteArrayOf(0))

    fun endRequest(): ByteArray = TlvCodec.encode(TYPE, ID_END, ByteArray(0))

    /** Son (0x0006, 0x0007, 0x0008): 4 baytli BE. Tur yoki hajm mos kelmasa WireFormatException. */
    fun decodeCount(tlv: Tlv): Int {
        if (tlv.type != TYPE || tlv.value.size != 4) {
            throw WireFormatException("Unexpected history count: id=${tlv.id}, vlen=${tlv.value.size}")
        }
        val v = tlv.value
        return ((v[0].toInt() and 0xFF) shl 24) or ((v[1].toInt() and 0xFF) shl 16) or
            ((v[2].toInt() and 0xFF) shl 8) or (v[3].toInt() and 0xFF)
    }
}
