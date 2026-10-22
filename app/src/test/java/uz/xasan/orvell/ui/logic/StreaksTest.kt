package uz.xasan.orvell.ui.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreaksTest {
    @Test fun currentStreakIncludesToday() = assertEquals(3, Streaks.current(listOf(100, 8000, 9000, 8000), 8000))

    @Test fun currentStreakSurvivesUnfinishedToday() = assertEquals(2, Streaks.current(listOf(8000, 9000, 1200), 8000))

    @Test fun currentStreakBrokenYesterday() = assertEquals(0, Streaks.current(listOf(9000, 100, 1200), 8000))

    @Test fun bestStreak() {
        assertEquals(4, Streaks.best(listOf(9000, 9000, 0, 8000, 8000, 8000, 8000, 10), 8000))
        assertEquals(0, Streaks.best(emptyList(), 8000))
        assertEquals(0, Streaks.best(listOf(9000), 0))
    }

    @Test fun achievementsFromRealData() {
        val steps = listOf(12_000, 8000, 8500, 9000, 100, 21_000)
        val a = Achievements.compute(steps, listOf(480, 500, 0, 490, 480, 300), stepGoal = 8000, sleepGoal = 480, daysWithData = 6)
            .associateBy { it.id }
        assertTrue(a.getValue(Achievement.Id.FIRST_DAY).earned)
        assertTrue(a.getValue(Achievement.Id.STREAK_3).earned)
        assertFalse(a.getValue(Achievement.Id.STREAK_7).earned)
        assertEquals(4f / 7f, a.getValue(Achievement.Id.STREAK_7).progress, 1e-4f)
        assertTrue(a.getValue(Achievement.Id.STEPS_20K).earned)
        assertEquals(4, a.getValue(Achievement.Id.SLEEP_GOAL_5).value)
        assertEquals(58_600, a.getValue(Achievement.Id.TOTAL_100K).value)
    }

    @Test fun noDataEarnsNothing() {
        assertTrue(Achievements.compute(emptyList(), emptyList(), 8000, 480, 0).none { it.earned })
    }
}
