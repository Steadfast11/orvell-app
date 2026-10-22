package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Random

/** Buzilgan kirish: kodekdan faqat WireFormatException chiqadi, reader bo'sh holatga qaytadi. */
class MalformedInputTest {
    // V4 (frame.md): soat ajratish javobi, sarlavha va payload
    private val v4Header = hex("ab 00 00 06 eb 05 1b 9e")
    private val v4Payload = hex("03 00 16 00 01 00")

    @Test
    fun truncatedHeaderIsRejected() {
        val ack = hex("ab 10 00 00 00 00 00 66")
        for (n in 0 until FRAME_HEADER_SIZE) {
            assertThrows("hajm $n", WireFormatException::class.java) { FrameCodec.parseHeader(ack.copyOf(n)) }
            assertThrows("hajm $n", WireFormatException::class.java) { FrameReader().feed(ack.copyOf(n)) }
        }
    }

    @Test
    fun truncatedPayloadIsRejectedAndReaderRecovers() {
        val reader = FrameReader()
        for (n in 0 until v4Payload.size) {
            assertNull(reader.feed(v4Header))
            assertThrows("hajm $n", WireFormatException::class.java) { reader.feed(v4Payload.copyOf(n)) }
            assertFalse(reader.isAwaitingPayload)
        }
        // Xatolardan keyin to'liq frame odatdagidek qabul qilinadi
        assertNull(reader.feed(v4Header))
        assertArrayEquals(v4Payload, reader.feed(v4Payload)!!.payload)
    }

    @Test
    fun truncatedTlvIsRejected() {
        val tlv = hex("05 00 06 00 04 00 00 00 21")
        for (n in 0 until tlv.size) {
            assertThrows("hajm $n", WireFormatException::class.java) { TlvCodec.decode(tlv.copyOf(n)) }
        }
    }

    @Test
    fun maxLenIsRejectedByPayloadSizeNotByAllocation() {
        // LEN = 0xFFFF: sarlavhaning o'zi rad etilmaydi (frame.md: LEN > 244 alohida tekshirilmaydi)
        val header = hex("ab 00 ff ff 12 34 00 01")
        val parsed = FrameCodec.parseHeader(header)
        assertEquals(0xFFFF, parsed.length)
        assertFalse(FrameCodec.isPayloadValid(parsed, ByteArray(244)))

        // Notify hajmi LEN ga teng emas: nusxa olinmasdan rad, reader bo'sh holatga qaytadi
        val reader = FrameReader()
        assertNull(reader.feed(header))
        assertThrows(WireFormatException::class.java) { reader.feed(ByteArray(244)) }
        assertFalse(reader.isAwaitingPayload)
        assertNull(reader.feed(header))
        assertThrows(WireFormatException::class.java) { reader.feed(ByteArray(0)) }
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun lenOneMoreThanPayloadIsRejected() {
        // V4 sarlavhasi, lekin LEN 7 (payload 6 bayt)
        val reader = FrameReader()
        assertNull(reader.feed(hex("ab 00 00 07 eb 05 1b 9e")))
        assertThrows(WireFormatException::class.java) { reader.feed(v4Payload) }
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun corruptedHeaderCrcIsRejected() {
        // V4 sarlavhasida CRC EB04 (to'g'risi EB05)
        val reader = FrameReader()
        assertNull(reader.feed(hex("ab 00 00 06 eb 04 1b 9e")))
        assertThrows(WireFormatException::class.java) { reader.feed(v4Payload) }
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun everySingleBitFlipInPayloadIsRejected() {
        val reader = FrameReader()
        for (bit in 0 until v4Payload.size * 8) {
            val bad = v4Payload.copyOf()
            bad[bit / 8] = (bad[bit / 8].toInt() xor (1 shl (bit % 8))).toByte()
            assertNull(reader.feed(v4Header))
            assertThrows("bit $bit", WireFormatException::class.java) { reader.feed(bad) }
        }
    }

    @Test
    fun vlenLargerThanRemainingBytesIsTypedError() {
        // VLEN 0xFFFF, qiymat yo'q: IndexOutOfBounds emas, WireFormatException
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(hex("03 00 16 ff ff")) }
        // VLEN 2, qolgani 1 bayt
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(hex("03 00 16 00 02 00")) }
        // VLEN 0x0100 (BE): LE deb o'qilsa 1 bo'lib qabul qilinardi
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(hex("03 00 16 01 00 00")) }
    }

    @Test
    fun frameWithValidCrcButBadVlenIsRejectedAtTlvLayer() {
        // CRC to'g'ri, VLEN 5, qiymat 1 bayt: frame qabul qilinadi, TLV rad etiladi
        val payload = hex("03 00 16 00 05 00")
        val reader = FrameReader()
        assertNull(reader.feed(FrameCodec.encodeData(1, payload).copyOf(FRAME_HEADER_SIZE)))
        val frame = reader.feed(payload)!!
        assertThrows(WireFormatException::class.java) { TlvCodec.decode(frame.payload) }
    }

    @Test
    fun randomInputOnlyThrowsWireFormatException() {
        // Qat'iy seed: test har safar bir xil
        val rnd = Random(0x0AB0L)
        val reader = FrameReader()
        val decoders: List<(ByteArray) -> Any?> = listOf(
            { b -> FrameCodec.parseHeader(b) },
            { b -> reader.feed(b) },
            { b -> TlvCodec.decode(b) },
            { b -> TimeCodec.decode(b) },
            { b -> BindMessages.decodeBindResponse(TlvCodec.decode(b)) },
            { b -> HistoryMessages.decodeCount(TlvCodec.decode(b)) },
        )
        repeat(5000) {
            val size = when (rnd.nextInt(4)) {
                0 -> FRAME_HEADER_SIZE
                1 -> 4
                else -> rnd.nextInt(24)
            }
            val bytes = ByteArray(size).also { rnd.nextBytes(it) }
            // Ko'pincha to'g'ri MAGIC/KIND va mos VLEN: chuqurroq tekshiruvlar ham ishlaydi
            if (size >= 2 && rnd.nextBoolean()) {
                bytes[0] = FRAME_MAGIC.toByte()
                bytes[1] = (if (rnd.nextBoolean()) KIND_DATA else KIND_ACK).toByte()
            }
            if (size >= 5 && rnd.nextBoolean()) bytes.writeU16Be(3, size - 5)
            for (decode in decoders) {
                try {
                    decode(bytes)
                } catch (_: WireFormatException) {
                    // kutilgan xato turi
                }
            }
        }
    }
}
