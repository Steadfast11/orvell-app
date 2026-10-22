package uz.xasan.orvell.protocol.wire

/**
 * CRC-16/ARC: poly 0x8005 (reflected 0xA001), init 0x0000, xorout 0x0000,
 * kirish va chiqish reflected. Tekshiruv: "123456789" uchun 0xBB3D.
 */
object Crc16Arc {
    private const val POLY_REFLECTED = 0xA001

    fun compute(data: ByteArray): Int {
        var crc = 0
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor POLY_REFLECTED else crc ushr 1
            }
        }
        // crc har doim 0..0xFFFF oralig'ida qoladi
        return crc
    }
}
