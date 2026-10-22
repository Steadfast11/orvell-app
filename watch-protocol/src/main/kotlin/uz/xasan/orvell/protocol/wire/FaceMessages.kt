package uz.xasan.orvell.protocol.wire

/**
 * Siferblat bilan bog'liq asosiy kanal xabarlari (spec watchface.md §4, o'z logimiz 2026-10-13).
 *
 *  uzatishdan oldin:  P 01/0001 (bo'sh) → W 01/0002 (2 bayt, 00 00 = tayyor) [T] (2 / 2)
 *  har ulanishda:     P 02/0052 (bo'sh) → W 02/0053: siferblatlar ro'yxati, joriy siferblat ID u16 BE offset 4 [T] (5 / 5)
 *  joriy = 0xC520:    P 02/0070 = 05 01 00 (rasmli siferblat sozlamasi) [T] (2 / 2), ma'nosi [N]
 */
object FaceMessages {
    const val TYPE_PREPARE = 0x01
    const val ID_PREPARE = 0x0001
    const val ID_PREPARE_RESPONSE = 0x0002

    const val TYPE_DIAL = 0x02
    const val ID_DIAL_LIST = 0x0052
    const val ID_DIAL_LIST_RESPONSE = 0x0053
    const val ID_CUSTOM_CONFIG = 0x0070

    /** Rasmli siferblat ID'si (TBUI sarlavhasi 0x08 ning quyi 16 biti, 02/0053 dagi joriy ID). */
    const val CUSTOM_DIAL_ID = 0xC520

    fun prepareRequest(): ByteArray = TlvCodec.encode(TYPE_PREPARE, ID_PREPARE, ByteArray(0))

    /** 01/0002: true — soat tayyor (00 00). Hajm mos kelmasa WireFormatException. */
    fun decodePrepared(tlv: Tlv): Boolean {
        if (tlv.type != TYPE_PREPARE || tlv.id != ID_PREPARE_RESPONSE || tlv.value.size != 2) {
            throw WireFormatException("Unexpected prepare response: id=${tlv.id}, vlen=${tlv.value.size}")
        }
        return tlv.value[0].toInt() == 0 && tlv.value[1].toInt() == 0
    }

    fun dialListRequest(): ByteArray = TlvCodec.encode(TYPE_DIAL, ID_DIAL_LIST, ByteArray(0))

    /** 02/0053 dan joriy siferblat ID'si. */
    fun decodeCurrentDial(tlv: Tlv): Int {
        if (tlv.type != TYPE_DIAL || tlv.id != ID_DIAL_LIST_RESPONSE || tlv.value.size < 6) {
            throw WireFormatException("Unexpected dial list: id=${tlv.id}, vlen=${tlv.value.size}")
        }
        return ((tlv.value[4].toInt() and 0xFF) shl 8) or (tlv.value[5].toInt() and 0xFF)
    }

    /** Rasmli siferblat joriy bo'lganda FitCloudPro yuboradigan sozlama (kuzatilgan qiymat). */
    fun customConfig(): ByteArray = TlvCodec.encode(TYPE_DIAL, ID_CUSTOM_CONFIG, byteArrayOf(0x05, 0x01, 0x00))
}
