package uz.xasan.orvell.watch

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.xasan.orvell.protocol.wire.ResponseStatus

class BindFlowTest {
    private val ok = ResponseStatus.Success
    private val other = ResponseStatus.OtherCode(0x05)
    private val waitingOnWatch = ResponseStatus.OtherCode(0x01)

    @Test
    fun startChecksWithConfirmRequestFirst() {
        val f = BindFlow()
        assertEquals(BindFlow.Next.SendCheck, f.start())
        assertEquals(BindFlow.Phase.CHECK_WAIT, f.phase)
    }

    @Test
    fun secondStartIsIgnored() {
        val f = BindFlow()
        f.start()
        assertEquals(BindFlow.Next.None, f.start())
        assertEquals(BindFlow.Phase.CHECK_WAIT, f.phase)
    }

    @Test
    fun knownAccountAnswersZeroAndIsBoundWithoutWatchConfirmation() {
        val f = BindFlow()
        f.start()
        assertEquals(BindFlow.Next.Bound, f.onConfirmResponse(ok))
        assertEquals(BindFlow.Phase.BOUND, f.phase)
    }

    @Test
    fun waitingOnWatchAnswerSendsBindAndWaitsForTap() {
        val f = BindFlow()
        f.start()
        assertEquals(BindFlow.Next.SendBind, f.onConfirmResponse(waitingOnWatch))
        assertEquals(BindFlow.Phase.CONFIRM_ON_WATCH, f.phase)
        assertEquals(BindFlow.Next.Bound, f.onBindResponse(ok))
        assertEquals(BindFlow.Phase.BOUND, f.phase)
    }

    @Test
    fun otherConfirmAnswerFallsBackToBind() {
        val f = BindFlow()
        f.start()
        assertEquals(BindFlow.Next.SendBind, f.onConfirmResponse(other))
        assertEquals(BindFlow.Phase.LOGIN_WAIT, f.phase)
        assertEquals(BindFlow.Next.Bound, f.onBindResponse(ok))
    }

    @Test
    fun silentCheckFallsBackToBind() {
        val f = BindFlow()
        f.start()
        assertEquals(BindFlow.Next.SendBind, f.onTick())
        assertEquals(BindFlow.Phase.LOGIN_WAIT, f.phase)
    }

    @Test
    fun confirmAnswerIgnoredOutsideCheckWait() {
        val f = BindFlow()
        f.start()
        f.onTick()
        assertEquals(BindFlow.Next.None, f.onConfirmResponse(ok))
        assertEquals(BindFlow.Phase.LOGIN_WAIT, f.phase)
    }

    @Test
    fun loginWaitTickEntersConfirmOnWatchAndPollsWithCheckNotBind() {
        val f = BindFlow()
        f.start()
        f.onTick()
        assertEquals(BindFlow.Next.SendCheck, f.onTick())
        assertEquals(BindFlow.Phase.CONFIRM_ON_WATCH, f.phase)
    }

    @Test
    fun watchWaitingForTapGetsOneBindThenOnlyPeriodicChecks() {
        // 0x0011 faqat bir marta: har yuborish soatda yangi ✓ so'roviga sabab bo'lishi mumkin
        val f = BindFlow()
        f.start()
        assertEquals(BindFlow.Next.SendBind, f.onConfirmResponse(waitingOnWatch))
        repeat(10) {
            assertEquals(BindFlow.Next.SendCheck, f.onTick())
            assertEquals(BindFlow.Next.None, f.onConfirmResponse(waitingOnWatch))
        }
        assertEquals(BindFlow.Phase.CONFIRM_ON_WATCH, f.phase)
    }

    @Test
    fun silentChecksResendBindOnEveryOtherTick() {
        // Tekshiruvga javob kelmasa (tekshiruv ishlamaydi): 0x0011 ga qaytiladi, lekin har tick emas
        val f = BindFlow()
        f.start()
        f.onConfirmResponse(waitingOnWatch)
        assertEquals(BindFlow.Next.SendCheck, f.onTick())
        assertEquals(BindFlow.Next.SendBind, f.onTick())
        assertEquals(BindFlow.Next.SendCheck, f.onTick())
        assertEquals(BindFlow.Next.SendBind, f.onTick())
        assertEquals(BindFlow.Phase.CONFIRM_ON_WATCH, f.phase)
    }

    @Test
    fun tapSeenByPeriodicCheckBindsWithoutAnotherBind() {
        val f = BindFlow()
        f.start()
        f.onConfirmResponse(waitingOnWatch)
        assertEquals(BindFlow.Next.SendCheck, f.onTick())
        assertEquals(BindFlow.Next.Bound, f.onConfirmResponse(ok))
        assertEquals(BindFlow.Phase.BOUND, f.phase)
    }

    @Test
    fun otherCheckAnswerWhileWaitingKeepsWaiting() {
        val f = BindFlow()
        f.start()
        f.onConfirmResponse(waitingOnWatch)
        assertEquals(BindFlow.Next.None, f.onConfirmResponse(other))
        assertEquals(BindFlow.Phase.CONFIRM_ON_WATCH, f.phase)
    }

    @Test
    fun otherBindStatusFailsWithRawByte() {
        val f = BindFlow()
        f.start()
        f.onTick()
        assertEquals(BindFlow.Next.Failed(0x05), f.onBindResponse(other))
        assertEquals(BindFlow.Phase.FAILED, f.phase)
    }

    @Test
    fun responsesAndTicksInIdleBoundAndFailedAreIgnored() {
        val idle = BindFlow()
        assertEquals(BindFlow.Next.None, idle.onBindResponse(ok))
        assertEquals(BindFlow.Next.None, idle.onConfirmResponse(ok))
        assertEquals(BindFlow.Next.None, idle.onTick())
        assertEquals(BindFlow.Phase.IDLE, idle.phase)

        val bound = BindFlow()
        bound.start()
        bound.onConfirmResponse(ok)
        assertEquals(BindFlow.Next.None, bound.onBindResponse(other))
        assertEquals(BindFlow.Next.None, bound.onTick())
        assertEquals(BindFlow.Phase.BOUND, bound.phase)

        val failed = BindFlow()
        failed.start()
        failed.onTick()
        failed.onBindResponse(other)
        assertEquals(BindFlow.Next.None, failed.onBindResponse(ok))
        assertEquals(BindFlow.Next.None, failed.onTick())
        assertEquals(BindFlow.Phase.FAILED, failed.phase)
    }

    @Test
    fun resetReturnsToIdleAndNextStartChecksAgain() {
        val f = BindFlow()
        f.start()
        f.reset()
        assertEquals(BindFlow.Phase.IDLE, f.phase)
        assertEquals(BindFlow.Next.SendCheck, f.start())
    }
}
