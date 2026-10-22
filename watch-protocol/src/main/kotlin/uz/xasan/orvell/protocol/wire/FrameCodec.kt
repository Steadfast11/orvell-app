package uz.xasan.orvell.protocol.wire

/** Frame sarlavhasini yaratadi va spec qoidalari bo'yicha tekshiradi. */
object FrameCodec {
    private const val MAX_U16 = 0xFFFF

    /**
     * Ma'lumot frame'i (telefon -> soat): sarlavha va payload bitta massivda,
     * bitta ATT yozuvida yuboriladi. Bo'sh payload rad etiladi.
     */
    fun encodeData(seq: Int, payload: ByteArray): ByteArray {
        require(payload.isNotEmpty()) { "Payload must not be empty (LEN > 0)" }
        require(payload.size <= MAX_U16) { "Payload too large for 2-byte LEN: ${payload.size}" }
        requireSeq(seq)
        val out = ByteArray(FRAME_HEADER_SIZE + payload.size)
        out[0] = FRAME_MAGIC.toByte()
        out[1] = KIND_DATA.toByte()
        out.writeU16Be(2, payload.size)
        out.writeU16Be(4, Crc16Arc.compute(payload))
        out.writeU16Be(6, seq)
        payload.copyInto(out, FRAME_HEADER_SIZE)
        return out
    }

    /** ACK frame'i (8 bayt): KIND 0x10, LEN 0, CRC 0x0000. SEQ tasdiqlanayotgan frame'niki. */
    fun encodeAck(seq: Int): ByteArray {
        requireSeq(seq)
        val out = ByteArray(FRAME_HEADER_SIZE)
        out[0] = FRAME_MAGIC.toByte()
        out[1] = KIND_ACK.toByte()
        out.writeU16Be(6, seq)
        return out
    }

    /**
     * 8 baytli sarlavhani o'qiydi va spec qoidalarini tekshiradi.
     * Qoida buzilsa WireFormatException.
     */
    fun parseHeader(bytes: ByteArray): FrameHeader {
        if (bytes.size != FRAME_HEADER_SIZE) {
            throw WireFormatException("Header must be $FRAME_HEADER_SIZE bytes, got ${bytes.size}")
        }
        val magic = bytes[0].toInt() and 0xFF
        if (magic != FRAME_MAGIC) {
            throw WireFormatException("Bad MAGIC: 0x%02X".format(magic))
        }
        val kind = bytes[1].toInt() and 0xFF
        // Noma'lum KIND: spec [N], xavfsiz rad
        if (kind != KIND_DATA && kind != KIND_ACK) {
            throw WireFormatException("Unknown KIND: 0x%02X".format(kind))
        }
        val length = bytes.readU16Be(2)
        val crc = bytes.readU16Be(4)
        val seq = bytes.readU16Be(6)
        if (kind == KIND_ACK && length != 0) {
            throw WireFormatException("ACK must have LEN 0, got $length")
        }
        if (length == 0 && crc != 0) {
            throw WireFormatException("LEN 0 requires CRC 0x0000, got 0x%04X".format(crc))
        }
        // KIND 0x00 va LEN 0: spec [N], rad etiladi
        if (kind == KIND_DATA && length == 0) {
            throw WireFormatException("Data frame with LEN 0 is rejected")
        }
        return FrameHeader(kind, length, crc, seq)
    }

    /** Payload uzunligi LEN ga va CRC sarlavhadagi CRC ga mos bo'lsa true. */
    fun isPayloadValid(header: FrameHeader, payload: ByteArray): Boolean =
        payload.size == header.length && Crc16Arc.compute(payload) == header.crc

    private fun requireSeq(seq: Int) {
        require(seq in 0..MAX_U16) { "SEQ must be in 0..65535: $seq" }
    }
}
