package uz.xasan.orvell.protocol.wire

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseStatusTest {
    @Test
    fun zeroIsSuccess() {
        assertEquals(ResponseStatus.Success, ResponseStatus.of(0x00))
        assertEquals(0x00, ResponseStatus.Success.raw)
    }

    @Test
    fun otherValueIsOtherCode() {
        assertEquals(ResponseStatus.OtherCode(5), ResponseStatus.of(0x05))
        assertEquals(5, ResponseStatus.of(0x05).raw)
    }
}
