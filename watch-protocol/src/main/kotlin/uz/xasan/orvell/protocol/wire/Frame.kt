package uz.xasan.orvell.protocol.wire

/** Frame sarlavhasining birinchi bayti (doim 0xAB). */
const val FRAME_MAGIC = 0xAB

/** Sarlavha hajmi: 8 bayt. */
const val FRAME_HEADER_SIZE = 8

/** Ma'lumot frame'i. Soat -> telefon yo'nalishda payload keyingi notify'da keladi. */
const val KIND_DATA = 0x00

/** ACK (tasdiqlash): LEN = 0, CRC = 0x0000. */
const val KIND_ACK = 0x10

/** Sarlavha: KIND, LEN (payload bayti), CRC (payload CRC-16/ARC), SEQ. */
data class FrameHeader(val kind: Int, val length: Int, val crc: Int, val seq: Int)

/** Tayyor frame. ACK uchun payload bo'sh. */
class Frame(val header: FrameHeader, val payload: ByteArray)
