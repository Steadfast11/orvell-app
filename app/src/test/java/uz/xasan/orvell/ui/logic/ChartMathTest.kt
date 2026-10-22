package uz.xasan.orvell.ui.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {
    @Test fun barIndexClampsToSlots() {
        assertEquals(0, ChartMath.barIndex(0f, 700f, 7))
        assertEquals(0, ChartMath.barIndex(99.9f, 700f, 7))
        assertEquals(1, ChartMath.barIndex(100f, 700f, 7))
        assertEquals(6, ChartMath.barIndex(699f, 700f, 7))
        assertEquals(6, ChartMath.barIndex(9999f, 700f, 7))   // barmoq grafikdan chiqib ketsa ham
        assertEquals(0, ChartMath.barIndex(-50f, 700f, 7))
        assertEquals(-1, ChartMath.barIndex(10f, 700f, 0))
    }

    @Test fun nearestIndexPicksClosestSample() {
        val xs = listOf(0.1f, 0.2f, 0.5f, 0.9f)
        assertEquals(0, ChartMath.nearestIndex(0f, xs))
        assertEquals(1, ChartMath.nearestIndex(0.26f, xs))
        assertEquals(2, ChartMath.nearestIndex(0.4f, xs))
        assertEquals(3, ChartMath.nearestIndex(1f, xs))
        assertEquals(0, ChartMath.nearestIndex(0.375f, listOf(0.25f, 0.5f)))   // teng masofada chapdagi
        assertEquals(-1, ChartMath.nearestIndex(0.5f, emptyList()))
    }

    @Test fun monotoneSlopesNeverOvershoot() {
        val xs = floatArrayOf(0f, 1f, 2f, 3f, 4f)
        val ys = floatArrayOf(60f, 60f, 120f, 121f, 121f)
        val m = ChartMath.monotoneSlopes(xs, ys)
        // tekis bo'laklarda qiyalik 0 (egri chiziq tepaga/pastga chiqmaydi)
        assertEquals(0f, m[0], 0f); assertEquals(0f, m[1], 0f); assertEquals(0f, m[4], 0f)
        // kubik Hermite bilan har bo'lakni tekshiramiz: qiymatlar uchlari orasida qoladi
        for (i in 0 until xs.size - 1) {
            for (k in 1..9) {
                val t = k / 10f; val h = xs[i + 1] - xs[i]
                val y = (2 * t * t * t - 3 * t * t + 1) * ys[i] + (t * t * t - 2 * t * t + t) * h * m[i] +
                    (-2 * t * t * t + 3 * t * t) * ys[i + 1] + (t * t * t - t * t) * h * m[i + 1]
                assertTrue("bo'lak $i t=$t y=$y", y >= minOf(ys[i], ys[i + 1]) - 1e-3f && y <= maxOf(ys[i], ys[i + 1]) + 1e-3f)
            }
        }
    }

    @Test fun niceMaxRoundsUp() {
        assertEquals(10_000f, ChartMath.niceMax(8412f), 0f)
        assertEquals(2_000f, ChartMath.niceMax(1200f), 0f)
        assertEquals(250f, ChartMath.niceMax(240f), 0f)
        assertEquals(5f, ChartMath.niceMax(4.2f), 0f)
        assertEquals(1f, ChartMath.niceMax(0f), 0f)
        assertEquals(1f, ChartMath.niceMax(Float.NaN), 0f)
    }
}
