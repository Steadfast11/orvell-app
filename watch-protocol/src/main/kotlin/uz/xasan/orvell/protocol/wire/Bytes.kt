package uz.xasan.orvell.protocol.wire

/** Big-endian 2 baytli qiymatni offset dan boshlab o'qiydi. */
internal fun ByteArray.readU16Be(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)

/** Qiymatni (0..0xFFFF) big-endian 2 bayt sifatida offset dan boshlab yozadi. */
internal fun ByteArray.writeU16Be(offset: Int, value: Int) {
    this[offset] = (value ushr 8).toByte()
    this[offset + 1] = value.toByte()
}
