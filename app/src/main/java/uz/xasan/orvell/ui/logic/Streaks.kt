package uz.xasan.orvell.ui.logic

/** Ketma-ketliklar va yutuqlar — faqat haqiqiy kunlik qiymatlardan (eskidan yangiga tartibda). */
object Streaks {

    /**
     * Joriy seriya: oxirgi kundan orqaga, maqsad bajarilgan ketma-ket kunlar.
     * Bugun hali bajarilmagan bo'lsa seriya uzilmagan hisoblanadi (kechagi kundan sanaladi).
     */
    fun current(values: List<Int>, goal: Int): Int {
        if (goal <= 0 || values.isEmpty()) return 0
        var i = values.lastIndex
        if (values[i] < goal) i--
        var n = 0
        while (i >= 0 && values[i] >= goal) { n++; i-- }
        return n
    }

    /** Davr ichidagi eng uzun seriya. */
    fun best(values: List<Int>, goal: Int): Int {
        if (goal <= 0) return 0
        var best = 0
        var run = 0
        for (v in values) { if (v >= goal) { run++; if (run > best) best = run } else run = 0 }
        return best
    }
}

/** Yutuq: maqsadga erishilganmi va qancha qolgan (qulflangan yutuq progress bilan ko'rsatiladi). */
data class Achievement(val id: Id, val value: Int, val target: Int) {
    enum class Id { FIRST_DAY, STREAK_3, STREAK_7, STREAK_30, STEPS_10K, STEPS_20K, SLEEP_GOAL_5, TOTAL_100K }
    val earned get() = value >= target
    val progress get() = if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)
}

object Achievements {
    /**
     * @param steps kunlik qadamlar (eskidan yangiga, masalan oxirgi 90 kun)
     * @param sleepMin kunlik uyqu daqiqalari (shu kunlar bilan bir xil uzunlikda)
     * @param daysWithData ma'lumoti bor kunlar soni
     */
    fun compute(steps: List<Int>, sleepMin: List<Int>, stepGoal: Int, sleepGoal: Int, daysWithData: Int): List<Achievement> {
        val best = Streaks.best(steps, stepGoal)
        val maxDay = steps.maxOrNull() ?: 0
        val sleepNights = if (sleepGoal > 0) sleepMin.count { it >= sleepGoal } else 0
        val total = steps.fold(0L) { acc, v -> acc + v.coerceAtLeast(0) }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return listOf(
            Achievement(Achievement.Id.FIRST_DAY, daysWithData, 1),
            Achievement(Achievement.Id.STREAK_3, best, 3),
            Achievement(Achievement.Id.STREAK_7, best, 7),
            Achievement(Achievement.Id.STREAK_30, best, 30),
            Achievement(Achievement.Id.STEPS_10K, maxDay, 10_000),
            Achievement(Achievement.Id.STEPS_20K, maxDay, 20_000),
            Achievement(Achievement.Id.SLEEP_GOAL_5, sleepNights, 5),
            Achievement(Achievement.Id.TOTAL_100K, total, 100_000),
        )
    }
}
