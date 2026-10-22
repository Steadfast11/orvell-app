package uz.xasan.orvell.ui.logic

import uz.xasan.orvell.data.DayStats
import uz.xasan.orvell.data.Health
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Kun "hikoyasi" uchun bitta qisqa xulosa. Faqat saqlangan o'lchovlardan va foydalanuvchi maqsadidan —
 * hech qanday o'ylab topilgan ball yo'q. Matni UI'da tanlangan tilda chiqariladi.
 */
sealed interface Insight {
    /** Kechagi uyqu oldingi kechalar o'rtachasidan farqi (daqiqa, + ko'p). */
    data class SleepVsAverage(val diffMin: Int) : Insight
    /** Bugun qadam maqsadi bajarildi. */
    data class GoalReached(val steps: Int) : Insight
    /** Maqsad ketma-ket N kun bajarilmoqda (bugun ham). */
    data class GoalStreak(val days: Int) : Insight
    /** Kechqurun: bugungi qadam oldingi kunlar o'rtachasidan farqi. */
    data class StepsVsAverage(val diff: Int) : Insight
    /** Uyqudagi yurak urishi odatdagidan farqi (bpm). */
    data class RestingHrVsAverage(val diffBpm: Int) : Insight
    /** Maqsadgacha qolgan qadam. */
    data class StepsToGoal(val remaining: Int) : Insight
}

object Insights {
    const val MIN_SLEEP_DIFF = 15
    const val MIN_HR_DIFF = 3

    /**
     * @param today bugungi kun
     * @param previous bugundan oldingi kunlar (eskidan yangiga), odatda 6–7 kun
     * @param hour hozirgi soat (0–23): ertalab uyqu, kechqurun kunlik natija muhimroq
     */
    fun pick(today: DayStats?, previous: List<Health.DayPoint>, stepGoal: Int, hour: Int): Insight? {
        if (today == null) return null
        val sleep = sleepInsight(today, previous)
        val streak = Streaks.current(previous.map { it.steps } + today.steps, stepGoal)
        val goal = if (stepGoal > 0 && today.steps >= stepGoal) {
            if (streak >= 3) Insight.GoalStreak(streak) else Insight.GoalReached(today.steps)
        } else null
        val stepsAvg = if (hour >= 18) stepsInsight(today, previous) else null
        val hr = hrInsight(today, previous)
        val toGoal = if (stepGoal > 0 && today.steps in 1 until stepGoal) Insight.StepsToGoal(stepGoal - today.steps) else null
        val order = if (hour < 12) listOf(sleep, goal, hr, toGoal) else listOf(goal, stepsAvg, sleep, hr, toGoal)
        return order.firstOrNull { it != null }
    }

    private fun sleepInsight(today: DayStats, previous: List<Health.DayPoint>): Insight? {
        val last = today.sleep?.asleepMin?.takeIf { it > 0 } ?: return null
        val nights = previous.map { it.sleepMin }.filter { it > 0 }
        if (nights.size < 2) return null
        val diff = (last - nights.average()).roundToInt()
        return if (abs(diff) >= MIN_SLEEP_DIFF) Insight.SleepVsAverage(diff) else null
    }

    private fun stepsInsight(today: DayStats, previous: List<Health.DayPoint>): Insight? {
        val days = previous.map { it.steps }.filter { it > 0 }
        if (days.size < 3 || today.steps <= 0) return null
        val avg = days.average()
        val diff = (today.steps - avg).roundToInt()
        return if (abs(diff) >= 500 && abs(diff) >= avg * 0.1) Insight.StepsVsAverage(diff) else null
    }

    private fun hrInsight(today: DayStats, previous: List<Health.DayPoint>): Insight? {
        val now = today.restingHr ?: return null
        val prev = previous.mapNotNull { it.restingHr }
        if (prev.size < 3) return null
        val diff = (now - prev.average()).roundToInt()
        return if (abs(diff) >= MIN_HR_DIFF) Insight.RestingHrVsAverage(diff) else null
    }
}
