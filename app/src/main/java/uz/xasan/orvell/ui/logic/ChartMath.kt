package uz.xasan.orvell.ui.logic

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

// Grafiklar uchun sof matematik (Android'siz, JVM testlari bor).
object ChartMath {

    /** Teng kenglikdagi ustunlar: barmoq x nuqtasi qaysi ustunga tushadi. */
    fun barIndex(x: Float, width: Float, count: Int): Int {
        if (count <= 0 || width <= 0f) return -1
        return floor(x / (width / count)).toInt().coerceIn(0, count - 1)
    }

    /** Vaqt o'qidagi (0..1) nuqtalardan barmoqqa eng yaqini. xs o'sish tartibida. */
    fun nearestIndex(fraction: Float, xs: List<Float>): Int {
        if (xs.isEmpty()) return -1
        var lo = 0
        var hi = xs.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (xs[mid] < fraction) lo = mid + 1 else hi = mid
        }
        return if (lo > 0 && abs(xs[lo - 1] - fraction) <= abs(xs[lo] - fraction)) lo - 1 else lo
    }

    /**
     * Monoton kubik interpolyatsiya (Fritsch–Carlson) uchun nuqtadagi qiyaliklar:
     * egri chiziq hech qachon haqiqiy qiymatlardan oshib ketmaydi (yurak urishi "soxta cho'qqi" bermaydi).
     */
    fun monotoneSlopes(xs: FloatArray, ys: FloatArray): FloatArray {
        val n = xs.size
        val m = FloatArray(n)
        if (n < 2) return m
        val d = FloatArray(n - 1) { i -> val dx = xs[i + 1] - xs[i]; if (dx == 0f) 0f else (ys[i + 1] - ys[i]) / dx }
        m[0] = d[0]
        m[n - 1] = d[n - 2]
        for (i in 1 until n - 1) m[i] = if (d[i - 1] * d[i] <= 0f) 0f else (d[i - 1] + d[i]) / 2f
        for (i in 0 until n - 1) {
            if (d[i] == 0f) { m[i] = 0f; m[i + 1] = 0f; continue }
            val a = m[i] / d[i]
            val b = m[i + 1] / d[i]
            val s = a * a + b * b
            if (s > 9f) {
                val t = 3f / sqrt(s)
                m[i] = t * a * d[i]
                m[i + 1] = t * b * d[i]
            }
        }
        return m
    }

    /** O'q uchun "chiroyli" yuqori chegara: 1, 2, 2.5, 5 × 10^n (masalan 8 412 -> 10 000). */
    fun niceMax(v: Float): Float {
        if (v <= 0f || !v.isFinite()) return 1f
        val exp = floor(log10(v.toDouble()))
        val base = 10.0.pow(exp)
        val f = v / base
        val nice = when { f <= 1.0 -> 1.0; f <= 2.0 -> 2.0; f <= 2.5 -> 2.5; f <= 5.0 -> 5.0; else -> 10.0 }
        return (nice * base).toFloat()
    }
}
