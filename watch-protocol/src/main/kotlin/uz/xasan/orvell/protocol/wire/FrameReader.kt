package uz.xasan.orvell.protocol.wire

/** Bo'lingan payload yig'iladigan eng katta LEN. Undan kattasi bo'laklarga bo'linsa rad etiladi (xotira chegarasi). */
const val MAX_REASSEMBLED_PAYLOAD = 4096

/**
 * Soatdan kelgan notify'larni frame'ga yig'adi.
 * Sarlavha bitta notify'da, payload keyingi notify'da keladi. LEN bitta notify'ga sig'masa payload
 * to'liq hajmli bo'laklarga bo'linib keladi (frame.md: LEN 261 = 244 + 17, birlashtirilganda CRC mos [T]).
 */
class FrameReader {
    /** To'liq notify hajmi (ATT_MTU - 3). Faqat shu hajmdagi bo'lakdan keyin davom kutiladi. */
    var chunkSize: Int = 244

    /** Payload kutilayotgan sarlavha. null bo'lsa reader bo'sh holatda. */
    private var pending: FrameHeader? = null
    private var buffer = ByteArray(0)
    private var filled = 0

    /** Payload notify'ini kutayotgan bo'lsa true. */
    val isAwaitingPayload: Boolean
        get() = pending != null

    /**
     * Bitta notify'ni qayta ishlaydi.
     * Frame tayyor bo'lsa uni qaytaradi, aks holda null (sarlavha yoki payload bo'lagi saqlanadi).
     * Qoida buzilsa WireFormatException; reader har holatda bo'sh holatga qaytadi.
     */
    fun feed(notification: ByteArray): Frame? {
        val header = pending
        if (header == null) {
            val parsed = FrameCodec.parseHeader(notification)
            if (parsed.kind == KIND_ACK) return Frame(parsed, ByteArray(0))
            // parseHeader KIND 0x00 uchun LEN > 0 ni kafolatlaydi
            pending = parsed
            filled = 0
            return null
        }

        val remaining = header.length - filled
        if (notification.size < remaining) {
            // Bo'lak: faqat to'liq hajmli notify va chegaradagi LEN. Aks holda kesilgan payload: rad
            if (notification.size != chunkSize || header.length > MAX_REASSEMBLED_PAYLOAD) {
                reset()
                throw WireFormatException("Payload size ${filled + notification.size} does not match LEN ${header.length}")
            }
            if (filled == 0) buffer = ByteArray(header.length)
            notification.copyInto(buffer, filled)
            filled += notification.size
            return null
        }

        // Payload notify'i sarlavha sifatida o'qilmaydi; natijadan qat'i nazar bo'sh holatga qaytiladi
        val split = filled > 0
        val partial = buffer
        val have = filled
        reset()
        if (notification.size != remaining) {
            throw WireFormatException("Payload size ${have + notification.size} does not match LEN ${header.length}")
        }
        val payload = if (split) partial.also { notification.copyInto(it, have) } else notification.copyOf()
        if (Crc16Arc.compute(payload) != header.crc) {
            throw WireFormatException("Payload CRC does not match header CRC")
        }
        return Frame(header, payload)
    }

    /** Kutilayotgan sarlavha va bo'laklarni tashlaydi. Bog'lanish uzilganda chaqiriladi (ilova qoidasi). */
    fun reset() {
        pending = null
        buffer = ByteArray(0)
        filled = 0
    }
}
