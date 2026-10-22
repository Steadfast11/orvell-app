package uz.xasan.orvell.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Canvas'da chizilgan sahnalar (rasm fayllarisiz): onboarding, bo'sh, xato va muvaffaqiyat holatlari. */
enum class Art { WELCOME, STORAGE, GOALS, NOTIFY, PAIR, DONE, NO_WATCH, NO_DATA, NO_SLEEP, NOT_FOUND, ERROR }

private const val TAU = (2 * PI).toFloat()
private val GOAL_FILLS = floatArrayOf(0.82f, 0.64f, 0.9f)

/** Bezak rasmi: ekran o'qigich uchun yashirilgan (ma'no yonidagi matnda). Reduce motion: harakatsiz. */
@Composable
fun Illustration(art: Art, modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val c = O.c
    val dark = c.isDark
    val t = loopState("art", 9000, off = O.reduceMotion, resting = 0.12f)
    val ink = c.text
    val track = c.track
    val surface = c.raised
    Spacer(
        modifier.size(size).clearAndSetSemantics { }.drawWithCache {
            val s = this.size.minDimension
            val halo = { tone: Tone, r: Float -> Brush.radialGradient(listOf(tone.a.copy(alpha = if (dark) 0.55f else 0.35f), Color.Transparent), center = Offset.Zero, radius = r) }
            val brandHalo = halo(Tones.brand, s * 0.45f)
            val planetHalos = listOf(Tones.activity, Tones.heart, Tones.sleep, Tones.oxygen).map { halo(it, s * 0.07f) }
            val dangerHalo = halo(Tones.danger, s * 0.12f)
            val sleepHalo = halo(Tones.sleep, s * 0.35f)
            // to'ldirish cho'tkalari ham keshda (har kadrda yangi Brush yaratilmaydi)
            val brandFill = Tones.brand.brush
            val actFill = Tones.activity.brush
            val sleepFill = Tones.sleep.brush
            val goalFills = listOf(Tones.activity, Tones.distance, Tones.calories).map { it.brush }
            val doneFills = listOf(Tones.activity, Tones.oxygen, Tones.sleep).map { it.brush }
            val glass = Brush.verticalGradient(listOf(surface, surface.copy(alpha = 0.7f)))
            val welcomeTones = listOf(Tones.activity, Tones.heart, Tones.sleep, Tones.oxygen)
            val doneTones = listOf(Tones.activity, Tones.oxygen, Tones.sleep)
            val dash = PathEffect.dashPathEffect(floatArrayOf(s * 0.02f, s * 0.025f))
            val path = Path()
            onDrawBehind {
                val ph = t.value
                when (art) {
                    Art.WELCOME -> {
                        translate(center.x, center.y) { drawCircle(brandHalo, s * 0.45f, Offset.Zero) }
                        drawCircle(brandFill, s * 0.11f, center)
                        drawCircle(Color.White.copy(alpha = 0.35f), s * 0.05f, center - Offset(s * 0.03f, s * 0.03f))
                        welcomeTones.forEachIndexed { i, tone ->
                            val rx = s * (0.22f + i * 0.065f); val ry = rx * 0.42f; val tilt = -24f + i * 14f
                            orbit(rx, ry, tilt, ink.copy(alpha = 0.16f), s * 0.006f)
                            val a = (ph * (1.4f - i * 0.25f) + i * 0.27f) * TAU
                            planet(rx, ry, tilt, a, s * 0.022f, tone.a, planetHalos[i], s * 0.07f)
                        }
                        stars(s, ink, ph)
                    }
                    Art.STORAGE -> {
                        val w = s * 0.34f; val h = s * 0.58f
                        val tl = center - Offset(w / 2, h / 2)
                        drawRoundRect(glass, tl, Size(w, h), CornerRadius(s * 0.06f))
                        drawRoundRect(ink.copy(alpha = 0.25f), tl, Size(w, h), CornerRadius(s * 0.06f), style = Stroke(s * 0.008f))
                        shield(path, center, s * 0.13f)
                        drawPath(path, actFill)
                        check(path, center, s * 0.06f)
                        drawPath(path, Color(0xFF0B0E14), style = Stroke(s * 0.018f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        orbit(s * 0.42f, s * 0.16f, -18f, ink.copy(alpha = 0.18f), s * 0.006f)
                        planet(s * 0.42f, s * 0.16f, -18f, ph * TAU, s * 0.026f, Tones.oxygen.a, planetHalos[3], s * 0.07f)
                    }
                    Art.GOALS -> {
                        for (i in 0..2) {
                            val r = s * (0.38f - i * 0.1f); val sw = s * 0.065f
                            drawCircle(track, r, center, style = Stroke(sw))
                            val f = (GOAL_FILLS[i] * (0.55f + 0.45f * (0.5f + 0.5f * sin(ph * TAU + i)))).coerceIn(0.05f, 1f)
                            rotate(-90f, center) { drawArc(goalFills[i], 0f, 360f * f, false, center - Offset(r, r), Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round)) }
                        }
                    }
                    Art.NOTIFY -> {
                        for (k in 0..2) {
                            val p = (ph * 3f + k / 3f) % 1f
                            drawCircle(Tones.sleep.a.copy(alpha = (1f - p) * 0.4f), s * (0.18f + p * 0.28f), center, style = Stroke(s * 0.008f))
                        }
                        bell(path, center, s * 0.17f)
                        drawPath(path, sleepFill)
                        drawCircle(Tones.heart.a, s * 0.035f, center + Offset(s * 0.12f, -s * 0.13f))
                    }
                    Art.PAIR -> {
                        watchShape(center - Offset(s * 0.22f, 0f), s * 0.2f, ink, glass)
                        val pw = s * 0.2f; val phh = s * 0.36f
                        val ptl = center + Offset(s * 0.13f, -phh / 2)
                        drawRoundRect(glass, ptl, Size(pw, phh), CornerRadius(s * 0.04f))
                        drawRoundRect(ink.copy(alpha = 0.25f), ptl, Size(pw, phh), CornerRadius(s * 0.04f), style = Stroke(s * 0.007f))
                        val from = center - Offset(s * 0.08f, 0f); val to = center + Offset(s * 0.11f, 0f)
                        drawLine(Tones.oxygen.a.copy(alpha = 0.35f), from, to, s * 0.006f, pathEffect = dash)
                        val p = ph * 4f % 1f
                        val dot = Offset(from.x + (to.x - from.x) * p, from.y)
                        translate(dot.x, dot.y) { drawCircle(planetHalos[3], s * 0.07f, Offset.Zero) }
                        drawCircle(Color.White, s * 0.016f, dot)
                    }
                    Art.DONE -> {
                        for (i in 0..2) {
                            val r = s * (0.36f - i * 0.09f)
                            drawCircle(doneFills[i], r, center, style = Stroke(s * 0.055f))
                        }
                        for (k in 0 until 16) {
                            val a = k / 16f * TAU
                            val d = s * (0.42f + 0.05f * sin(ph * TAU * 2 + k))
                            drawCircle(doneTones[k % 3].a.copy(alpha = 0.7f), s * 0.012f, center + Offset(d * cos(a), d * sin(a)))
                        }
                        check(path, center, s * 0.09f)
                        drawPath(path, ink, style = Stroke(s * 0.03f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    Art.NO_WATCH -> {
                        drawCircle(ink.copy(alpha = 0.18f), s * 0.38f, center, style = Stroke(s * 0.006f, pathEffect = dash))
                        watchShape(center, s * 0.24f, ink, glass)
                        planet(s * 0.38f, s * 0.38f, 0f, ph * TAU, s * 0.022f, Tones.oxygen.a, planetHalos[3], s * 0.07f)
                    }
                    Art.NO_DATA -> {
                        for (i in 0..2) drawCircle(ink.copy(alpha = 0.14f), s * (0.18f + i * 0.1f), center, style = Stroke(s * 0.006f, pathEffect = dash))
                        drawCircle(Tones.brand.a.copy(alpha = 0.25f), s * 0.06f, center)
                        planet(s * 0.28f, s * 0.28f, 0f, ph * TAU, s * 0.02f, Tones.activity.a, planetHalos[0], s * 0.07f)
                    }
                    Art.NO_SLEEP -> {
                        translate(center.x, center.y) { drawCircle(sleepHalo, s * 0.35f, Offset.Zero) }
                        drawCircle(sleepFill, s * 0.17f, center)
                        drawCircle(c.bg, s * 0.15f, center + Offset(s * 0.08f, -s * 0.05f))
                        stars(s, ink, ph)
                    }
                    Art.NOT_FOUND -> {
                        for (k in 0..2) {
                            val p = (ph * 2f + k / 3f) % 1f
                            drawCircle(Tones.oxygen.a.copy(alpha = (1f - p) * 0.35f), s * (0.1f + p * 0.32f), center, style = Stroke(s * 0.007f))
                        }
                        drawCircle(ink.copy(alpha = 0.6f), s * 0.1f, center - Offset(s * 0.03f, s * 0.03f), style = Stroke(s * 0.025f))
                        drawLine(ink.copy(alpha = 0.6f), center + Offset(s * 0.045f, s * 0.045f), center + Offset(s * 0.13f, s * 0.13f), s * 0.03f, cap = StrokeCap.Round)
                    }
                    Art.ERROR -> {
                        val r = s * 0.3f
                        rotate(-90f, center) { drawArc(ink.copy(alpha = 0.25f), 30f, 300f, false, center - Offset(r, r), Size(r * 2, r * 2), style = Stroke(s * 0.04f, cap = StrokeCap.Round)) }
                        val p = Offset(center.x + r * cos(-PI.toFloat() / 2 + 0.26f), center.y + r * sin(-PI.toFloat() / 2 + 0.26f))
                        translate(p.x, p.y) { drawCircle(dangerHalo, s * 0.12f, Offset.Zero) }
                        drawCircle(Tones.danger.a, s * 0.035f, p)
                        drawLine(Tones.danger.a, center - Offset(0f, s * 0.08f), center + Offset(0f, s * 0.03f), s * 0.035f, cap = StrokeCap.Round)
                        drawCircle(Tones.danger.a, s * 0.02f, center + Offset(0f, s * 0.09f))
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------- chizish yordamchilari

private fun DrawScope.orbit(rx: Float, ry: Float, tiltDeg: Float, color: Color, width: Float) =
    rotate(tiltDeg, center) { drawOval(color, center - Offset(rx, ry), Size(rx * 2, ry * 2), style = Stroke(width)) }

private fun DrawScope.planet(rx: Float, ry: Float, tiltDeg: Float, angle: Float, r: Float, color: Color, halo: Brush, haloR: Float) {
    val x = rx * cos(angle); val y = ry * sin(angle)
    val tr = tiltDeg * (PI / 180.0).toFloat()
    val p = Offset(center.x + x * cos(tr) - y * sin(tr), center.y + x * sin(tr) + y * cos(tr))
    translate(p.x, p.y) { drawCircle(halo, haloR, Offset.Zero) }
    drawCircle(color, r, p)
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.4f, p - Offset(r * 0.3f, r * 0.3f))
}

private fun DrawScope.stars(s: Float, ink: Color, ph: Float) {
    for (k in 0 until 9) {
        val x = ((k * 0.618f) % 1f) * size.width
        val y = ((k * 0.382f + 0.1f) % 1f) * size.height
        drawCircle(ink.copy(alpha = 0.12f + 0.18f * (0.5f + 0.5f * sin(ph * TAU * 2 + k))), s * 0.006f, Offset(x, y))
    }
}

private fun shield(p: Path, c: Offset, r: Float) {
    p.reset()
    p.moveTo(c.x, c.y - r)
    p.cubicTo(c.x + r * 0.6f, c.y - r * 0.7f, c.x + r, c.y - r * 0.75f, c.x + r, c.y - r * 0.75f)
    p.cubicTo(c.x + r, c.y + r * 0.3f, c.x + r * 0.5f, c.y + r * 0.85f, c.x, c.y + r * 1.05f)
    p.cubicTo(c.x - r * 0.5f, c.y + r * 0.85f, c.x - r, c.y + r * 0.3f, c.x - r, c.y - r * 0.75f)
    p.cubicTo(c.x - r, c.y - r * 0.75f, c.x - r * 0.6f, c.y - r * 0.7f, c.x, c.y - r)
    p.close()
}

private fun check(p: Path, c: Offset, r: Float) {
    p.reset()
    p.moveTo(c.x - r, c.y)
    p.lineTo(c.x - r * 0.25f, c.y + r * 0.7f)
    p.lineTo(c.x + r, c.y - r * 0.65f)
}

private fun bell(p: Path, c: Offset, r: Float) {
    p.reset()
    p.moveTo(c.x - r, c.y + r * 0.6f)
    p.cubicTo(c.x - r * 0.75f, c.y + r * 0.35f, c.x - r * 0.75f, c.y - r * 0.2f, c.x - r * 0.6f, c.y - r * 0.5f)
    p.cubicTo(c.x - r * 0.4f, c.y - r * 0.95f, c.x + r * 0.4f, c.y - r * 0.95f, c.x + r * 0.6f, c.y - r * 0.5f)
    p.cubicTo(c.x + r * 0.75f, c.y - r * 0.2f, c.x + r * 0.75f, c.y + r * 0.35f, c.x + r, c.y + r * 0.6f)
    p.close()
    p.addOval(androidx.compose.ui.geometry.Rect(c.x - r * 0.18f, c.y + r * 0.7f, c.x + r * 0.18f, c.y + r * 1.02f))
}

private fun DrawScope.watchShape(c: Offset, w: Float, ink: Color, glass: Brush) {
    val h = w * 1.18f
    drawRoundRect(ink.copy(alpha = 0.18f), Offset(c.x - w * 0.3f, c.y - h * 0.95f), Size(w * 0.6f, h * 1.9f), CornerRadius(w * 0.12f))
    drawRoundRect(glass, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(w * 0.26f))
    drawRoundRect(ink.copy(alpha = 0.35f), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(w * 0.26f), style = Stroke(w * 0.03f))
    drawCircle(Tones.activity.a, w * 0.24f, c, style = Stroke(w * 0.06f, cap = StrokeCap.Round))
}
