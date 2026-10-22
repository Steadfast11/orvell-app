package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BindMessagesTest {
    // Sintetik hisob satri: haqiqiy hisob emas
    private val testAccount = "TEST-ACCOUNT-001"

    @Test
    fun bindRequestHasFixedLayout() {
        val payload = BindMessages.bindRequest(testAccount)
        assertEquals(37, payload.size)
        val expected = hex("03 00 11 00 20") + testAccount.toByteArray(Charsets.US_ASCII) + ByteArray(16)
        assertArrayEquals(expected, payload)
    }

    @Test
    fun bindRequestAcceptsExactly32Chars() {
        val payload = BindMessages.bindRequest("A".repeat(32))
        assertEquals(37, payload.size)
        assertEquals(0x41, payload[5].toInt())
        assertEquals(0x41, payload[36].toInt())
    }

    @Test
    fun bindRequestRejects33Chars() {
        assertThrows(IllegalArgumentException::class.java) { BindMessages.bindRequest("A".repeat(33)) }
    }

    @Test
    fun bindRequestRejectsNonAscii() {
        assertThrows(IllegalArgumentException::class.java) { BindMessages.bindRequest("TEST-ü") }
    }

    @Test
    fun unbindRequestIsEmptyValueTlv() {
        assertArrayEquals(hex("03 00 15 00 00"), BindMessages.unbindRequest())
    }

    @Test
    fun bindResponseStatusIsDecoded() {
        assertEquals(
            ResponseStatus.Success,
            BindMessages.decodeBindResponse(Tlv(0x03, 0x0012, byteArrayOf(0x00))),
        )
        assertEquals(
            ResponseStatus.OtherCode(0x05),
            BindMessages.decodeBindResponse(Tlv(0x03, 0x0012, byteArrayOf(0x05))),
        )
    }

    @Test
    fun unbindResponseStatusIsDecoded() {
        assertEquals(
            ResponseStatus.Success,
            BindMessages.decodeUnbindResponse(Tlv(0x03, 0x0016, byteArrayOf(0x00))),
        )
    }

    @Test
    fun responseWithWrongTypeIdOrSizeIsRejected() {
        // ID noto'g'ri: ajratish javobi bog'lash javobi sifatida
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeBindResponse(Tlv(0x03, 0x0016, byteArrayOf(0x00)))
        }
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeUnbindResponse(Tlv(0x03, 0x0012, byteArrayOf(0x00)))
        }
        // TYPE noto'g'ri
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeBindResponse(Tlv(0x02, 0x0012, byteArrayOf(0x00)))
        }
        // Qiymat hajmi 1 bayt emas
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeBindResponse(Tlv(0x03, 0x0012, ByteArray(0)))
        }
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeBindResponse(Tlv(0x03, 0x0012, byteArrayOf(0x00, 0x00)))
        }
    }

    @Test
    fun confirmRequestHasFixedLayout() {
        val payload = BindMessages.confirmRequest(testAccount)
        assertEquals(37, payload.size)
        val expected = hex("03 00 13 00 20") + testAccount.toByteArray(Charsets.US_ASCII) + ByteArray(16)
        assertArrayEquals(expected, payload)
    }

    @Test
    fun confirmRequestUsesSameAccountFieldRules() {
        assertThrows(IllegalArgumentException::class.java) { BindMessages.confirmRequest("A".repeat(33)) }
        assertThrows(IllegalArgumentException::class.java) { BindMessages.confirmRequest("TEST-ü") }
        val payload = BindMessages.confirmRequest("A".repeat(32))
        assertEquals(37, payload.size)
        assertEquals(0x41, payload[36].toInt())
    }

    @Test
    fun confirmResponseStatusIsDecoded() {
        // 00: muvaffaqiyat; 01: soat tasdig'ini kutmoqda (ilova talqini)
        assertEquals(
            ResponseStatus.Success,
            BindMessages.decodeConfirmResponse(Tlv(0x03, 0x0014, byteArrayOf(0x00))),
        )
        assertEquals(
            ResponseStatus.OtherCode(0x01),
            BindMessages.decodeConfirmResponse(Tlv(0x03, 0x0014, byteArrayOf(0x01))),
        )
    }

    @Test
    fun confirmResponseWithWrongIdOrSizeIsRejected() {
        // ID noto'g'ri: bog'lash javobi tasdiqlash javobi sifatida
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeConfirmResponse(Tlv(0x03, 0x0012, byteArrayOf(0x00)))
        }
        // Qiymat hajmi 1 bayt emas
        assertThrows(WireFormatException::class.java) {
            BindMessages.decodeConfirmResponse(Tlv(0x03, 0x0014, ByteArray(0)))
        }
    }
}
