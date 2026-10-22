package uz.xasan.orvell.ui.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.xasan.orvell.data.DayStats
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.data.SleepNight
import java.time.LocalDate

class InsightsTest {
    private val today = LocalDate.of(2026, 10, 4)

    private fun day(steps: Int = 0, asleep: Int = 0, resting: Int? = null) = DayStats(
        date = today, steps = steps, distanceM = 0, kcal = 0, hourlySteps = List(24) { 0 },
        hr = emptyList(), hrAvg = null, hrMin = null, hrMax = null, restingHr = resting,
        spo2 = emptyList(), bp = emptyList(), resp = emptyList(),
        sleep = if (asleep > 0) SleepNight(0, asleep * 60_000L, emptyList(), 0, asleep, 0, 0) else null,
    )

    private fun prev(vararg steps: Int, sleep: Int = 0, resting: Int? = null) = steps.mapIndexed { i, s ->
        Health.DayPoint(today.minusDays((steps.size - i).toLong()), s, 0, 0, sleep, sleep, null, resting, null, null)
    }

    @Test fun morningSleepComparedToAverage() {
        val i = Insights.pick(day(steps = 300, asleep = 480), prev(5000, 6000, 7000, sleep = 440), 8000, hour = 8)
        assertEquals(Insight.SleepVsAverage(40), i)
    }

    @Test fun smallSleepDifferenceIsIgnored() {
        val i = Insights.pick(day(steps = 300, asleep = 450), prev(5000, 6000, sleep = 440), 8000, hour = 8)
        assertEquals(Insight.StepsToGoal(7700), i)
    }

    @Test fun goalReachedBeatsSleepInTheAfternoon() {
        val i = Insights.pick(day(steps = 9000, asleep = 300), prev(5000, 6000, sleep = 440), 8000, hour = 15)
        assertEquals(Insight.GoalReached(9000), i)
    }

    @Test fun streakOfThreeOrMore() {
        val i = Insights.pick(day(steps = 8100), prev(1000, 8000, 9000), 8000, hour = 15)
        assertEquals(Insight.GoalStreak(3), i)
    }

    @Test fun eveningStepsVersusAverage() {
        val i = Insights.pick(day(steps = 4000), prev(6000, 6000, 6000), 8000, hour = 20)
        assertEquals(Insight.StepsVsAverage(-2000), i)
    }

    @Test fun restingHeartRateChange() {
        val i = Insights.pick(day(resting = 62), prev(0, 0, 0, resting = 56), 8000, hour = 9)
        assertEquals(Insight.RestingHrVsAverage(6), i)
    }

    @Test fun nothingHonestToSay() {
        assertNull(Insights.pick(day(), emptyList(), 8000, hour = 9))
        assertNull(Insights.pick(null, emptyList(), 8000, hour = 9))
        assertNull(Insights.pick(day(steps = 100), emptyList(), 0, hour = 9))  // maqsad 0
    }
}
