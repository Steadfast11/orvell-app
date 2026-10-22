package uz.xasan.orvell.ui.logic

import uz.xasan.orvell.data.Sample

/**
 * Yurak urishi zonalari (taxminiy, "ma'lumot uchun"): maksimal puls = 220 − yosh.
 * Zona 0 = maksimalning 50% dan past (dam), 1..5 = 50–60, 60–70, 70–80, 80–90, 90%+.
 */
object HrZones {
    const val ZONES = 6
    /** Ikki o'lchov orasidagi vaqtni bundan uzoq hisoblamaymiz (soat o'lchamagan bo'shliq). */
    const val MAX_GAP_MS = 15 * 60_000L
    /** Oxirgi o'lchov uchun taxminiy davomiylik. */
    const val LAST_MS = 5 * 60_000L

    fun maxHr(age: Int?): Int? = age?.takeIf { it in 10..100 }?.let { 220 - it }

    fun zoneOf(bpm: Int, maxHr: Int): Int {
        if (maxHr <= 0) return 0
        val pct = bpm * 100 / maxHr
        return when {
            pct < 50 -> 0
            pct < 60 -> 1
            pct < 70 -> 2
            pct < 80 -> 3
            pct < 90 -> 4
            else -> 5
        }
    }

    /** Har zonada o'tgan daqiqalar. */
    fun minutesInZones(samples: List<Sample>, maxHr: Int): IntArray {
        val out = LongArray(ZONES)
        val s = samples.sortedBy { it.start }
        for (i in s.indices) {
            val dur = if (i < s.lastIndex) (s[i + 1].start - s[i].start).coerceIn(0, MAX_GAP_MS) else LAST_MS
            out[zoneOf(s[i].value.toInt(), maxHr)] += dur
        }
        return IntArray(ZONES) { (out[it] / 60_000).toInt() }
    }
}
