package uz.xasan.orvell.phone

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StepTallyTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val yesterday = LocalDate.of(2026, 10, 11)

    @Test
    fun firstReadingOfTheDayStartsAtZero() {
        val t = StepTally(today, 0, 500).add(500, today)
        assertEquals(0, t.total)
    }

    @Test
    fun growthOfCounterIsAdded() {
        val t = StepTally(today, 0, 500).add(530, today)
        assertEquals(30, t.total)
        assertEquals(530L, t.last)
    }

    @Test
    fun readingsAccumulate() {
        val t = StepTally(today, 0, 0).add(10, today).add(25, today)
        assertEquals(25, t.total)
    }

    @Test
    fun sameCounterAddsNothing() {
        val t = StepTally(today, 30, 530).add(530, today)
        assertEquals(30, t.total)
    }

    @Test
    fun newDayRestartsTheTally() {
        val t = StepTally(yesterday, 80, 530).add(540, today)
        assertEquals(0, t.total)
        assertEquals(today, t.date)
        assertEquals(540L, t.last)
    }

    @Test
    fun rebootCounterResetAddsFreshCount() {
        // Qurilma qayta yonganda hisoblagich 0 dan boshlanadi: yangi qiymat to'liq qo'shiladi
        val t = StepTally(today, 30, 1000).add(120, today)
        assertEquals(150, t.total)
        assertEquals(120L, t.last)
    }
}
