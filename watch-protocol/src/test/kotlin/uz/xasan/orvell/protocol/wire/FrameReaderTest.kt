package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameReaderTest {
    @Test
    fun watchUnbindResponseIsAcceptedV4() {
        val reader = FrameReader()
        assertNull(reader.feed(hex("ab 00 00 06 eb 05 1b 9e")))
        assertTrue(reader.isAwaitingPayload)

        val frame = reader.feed(hex("03 00 16 00 01 00"))!!
        assertFalse(reader.isAwaitingPayload)
        assertEquals(0xEB05, frame.header.crc)
        assertEquals(0x1B9E, frame.header.seq)

        val tlv = TlvCodec.decode(frame.payload)
        assertEquals(BindMessages.TYPE, tlv.type)
        assertEquals(BindMessages.ID_UNBIND_RESPONSE, tlv.id)
        assertArrayEquals(byteArrayOf(0x00), tlv.value)
        assertEquals(ResponseStatus.Success, BindMessages.decodeUnbindResponse(tlv))
    }

    @Test
    fun watchBindRelatedFrameIsAcceptedV5() {
        val reader = FrameReader()
        assertNull(reader.feed(hex("ab 00 00 06 93 c5 1b 29")))
        val frame = reader.feed(hex("03 00 14 00 01 01"))!!
        assertEquals(0x93C5, frame.header.crc)
        // 0x0014 faqat umumiy TLV sifatida o'qiladi
        val tlv = TlvCodec.decode(frame.payload)
        assertEquals(0x0014, tlv.id)
        assertArrayEquals(byteArrayOf(0x01), tlv.value)
    }

    @Test
    fun watchBindResponseIsAcceptedV6() {
        val reader = FrameReader()
        assertNull(reader.feed(hex("ab 00 00 06 db 04 1b 9f")))
        val frame = reader.feed(hex("03 00 12 00 01 00"))!!
        assertEquals(ResponseStatus.Success, BindMessages.decodeBindResponse(TlvCodec.decode(frame.payload)))
    }

    @Test
    fun ackIsReturnedImmediatelyWithEmptyPayload() {
        val reader = FrameReader()
        val frame = reader.feed(hex("ab 10 00 00 00 00 00 66"))!!
        assertEquals(KIND_ACK, frame.header.kind)
        assertEquals(0, frame.payload.size)
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun payloadCrcMismatchIsRejected() {
        val reader = FrameReader()
        reader.feed(hex("ab 00 00 06 eb 05 1b 9e"))
        assertThrows(WireFormatException::class.java) { reader.feed(hex("03 00 16 00 01 01")) }
    }

    @Test
    fun payloadSizeMismatchIsRejected() {
        val reader = FrameReader()
        reader.feed(hex("ab 00 00 06 eb 05 1b 9e"))
        assertThrows(WireFormatException::class.java) { reader.feed(hex("03 00 16 00 01")) }
    }

    private fun header(len: Int, crc: Int, seq: Int = 1) =
        hex("ab 00 %04x %04x %04x".format(len, crc, seq))

    private val big = ByteArray(261) { (it * 7 + 3).toByte() }

    @Test
    fun payloadSplitAs244Plus17IsReassembled() {
        val reader = FrameReader()
        assertNull(reader.feed(header(261, Crc16Arc.compute(big))))
        assertNull(reader.feed(big.copyOfRange(0, 244)))
        assertTrue(reader.isAwaitingPayload)
        val frame = reader.feed(big.copyOfRange(244, 261))!!
        assertArrayEquals(big, frame.payload)
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun splitPayloadFollowsChunkSize() {
        val reader = FrameReader().apply { chunkSize = 20 }
        assertNull(reader.feed(header(261, Crc16Arc.compute(big))))
        for (i in 0 until 13) assertNull(reader.feed(big.copyOfRange(i * 20, i * 20 + 20)))
        assertArrayEquals(big, reader.feed(big.copyOfRange(260, 261))!!.payload)
    }

    @Test
    fun splitPayloadWithBadCrcIsRejected() {
        val reader = FrameReader()
        reader.feed(header(261, Crc16Arc.compute(big) xor 1))
        reader.feed(big.copyOfRange(0, 244))
        assertThrows(WireFormatException::class.java) { reader.feed(big.copyOfRange(244, 261)) }
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun shortFirstChunkOrOverflowIsRejected() {
        val reader = FrameReader()
        reader.feed(header(261, Crc16Arc.compute(big)))
        assertThrows(WireFormatException::class.java) { reader.feed(big.copyOfRange(0, 243)) }
        assertFalse(reader.isAwaitingPayload)

        reader.feed(header(261, Crc16Arc.compute(big)))
        reader.feed(big.copyOfRange(0, 244))
        assertThrows(WireFormatException::class.java) { reader.feed(ByteArray(18)) }
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun lenAboveReassemblyLimitIsNotBuffered() {
        val reader = FrameReader()
        reader.feed(header(MAX_REASSEMBLED_PAYLOAD + 1, 0x1234))
        assertThrows(WireFormatException::class.java) { reader.feed(ByteArray(244)) }
        assertFalse(reader.isAwaitingPayload)
    }

    @Test
    fun payloadLookingLikeAckIsNotParsedAsHeader() {
        val payload = hex("ab 10 00 00 00 00 00 66")
        val crc = Crc16Arc.compute(payload)
        val reader = FrameReader()
        assertNull(reader.feed(hex("ab 00 00 08 " + "%04x".format(crc) + " 00 01")))
        val frame = reader.feed(payload)!!
        assertEquals(KIND_DATA, frame.header.kind)
        assertArrayEquals(payload, frame.payload)
    }

    @Test
    fun readerIsIdleAfterPayloadCrcError() {
        val reader = FrameReader()
        reader.feed(hex("ab 00 00 06 eb 05 1b 9e"))
        assertThrows(WireFormatException::class.java) { reader.feed(hex("03 00 16 00 01 01")) }
        assertFalse(reader.isAwaitingPayload)
        assertEquals(KIND_ACK, reader.feed(hex("ab 10 00 00 00 00 00 66"))!!.header.kind)
    }

    @Test
    fun readerIsIdleAfterSizeError() {
        val reader = FrameReader()
        reader.feed(hex("ab 00 01 05 00 00 00 01"))
        assertThrows(WireFormatException::class.java) { reader.feed(ByteArray(200)) }
        assertFalse(reader.isAwaitingPayload)
        assertEquals(KIND_ACK, reader.feed(hex("ab 10 00 00 00 00 00 66"))!!.header.kind)
    }

    @Test
    fun resetDropsPendingHeader() {
        val reader = FrameReader()
        reader.feed(hex("ab 00 00 06 eb 05 1b 9e"))
        assertTrue(reader.isAwaitingPayload)
        reader.reset()
        assertFalse(reader.isAwaitingPayload)
        // Tashlangan sarlavhadan keyin 6 baytli notify sarlavha sifatida tekshiriladi va rad etiladi: hajmi 8 emas (hajm tekshiruvi, MAGIC emas)
        assertThrows(WireFormatException::class.java) { reader.feed(hex("03 00 16 00 01 00")) }
        assertFalse(reader.isAwaitingPayload)
    }
}
