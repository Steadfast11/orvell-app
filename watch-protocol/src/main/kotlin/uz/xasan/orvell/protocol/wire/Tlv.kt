package uz.xasan.orvell.protocol.wire

/** TLV yozuvi: TYPE (oila), ID va VALUE. */
class Tlv(val type: Int, val id: Int, val value: ByteArray)

/** TLV kodeki. Format: TYPE (1 bayt) | ID (2 bayt, BE) | VLEN (2 bayt, BE) | VALUE. */
object TlvCodec {
    private const val HEADER_SIZE = 5
    private const val MAX_U16 = 0xFFFF

    /**
     * TLV baytlarini yaratadi, VLEN = value.size.
     * Maydon hajmiga sig'maydigan qiymat IllegalArgumentException beradi.
     */
    fun encode(type: Int, id: Int, value: ByteArray): ByteArray {
        require(type in 0..0xFF) { "TYPE must fit in 1 byte: $type" }
        require(id in 0..MAX_U16) { "ID must fit in 2 bytes: $id" }
        require(value.size <= MAX_U16) { "VLEN must fit in 2 bytes: ${value.size}" }
        val out = ByteArray(HEADER_SIZE + value.size)
        out[0] = type.toByte()
        out.writeU16Be(1, id)
        out.writeU16Be(3, value.size)
        value.copyInto(out, HEADER_SIZE)
        return out
    }

    /** Payload'ni TLV sifatida o'qiydi. Hajm 5 + VLEN ga teng bo'lishi shart, aks holda WireFormatException. */
    fun decode(payload: ByteArray): Tlv {
        if (payload.size < HEADER_SIZE) {
            throw WireFormatException("TLV must be at least $HEADER_SIZE bytes, got ${payload.size}")
        }
        val vlen = payload.readU16Be(3)
        if (payload.size != HEADER_SIZE + vlen) {
            throw WireFormatException("TLV size ${payload.size} does not match 5 + VLEN ($vlen)")
        }
        return Tlv(
            type = payload[0].toInt() and 0xFF,
            id = payload.readU16Be(1),
            value = payload.copyOfRange(HEADER_SIZE, payload.size),
        )
    }
}
