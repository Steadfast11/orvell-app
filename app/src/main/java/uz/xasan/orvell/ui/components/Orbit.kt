package uz.xasan.orvell.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class Ring(val progress: Float, val tone: Tone)

private const val TRAIL = 5
private const val BURST = 14
private const val TRAIL_STEP = (7.0 * PI / 180.0).toFloat()

/** Infinite animatsiya yoki reduce motion bo'lsa doimiy qiymat (shartli chaqiruv: qiymat kompozitsiya davomida o'zgarmaydi). */
@Composable
internal fun loop(label: String, durationMs: Int, reverse: Boolean, off: Boolean, resting: Float = 0f): State<Float> =
    if (off) remember { mutableFloatStateOf(resting) }
    else rememberInfiniteTransition(label = label).animateFloat(
        0f, 1f, infiniteRepeatable(tween(durationMs, easing = if (reverse) FastOutSlowInEasing else LinearEasing), if (reverse) RepeatMode.Reverse else RepeatMode.Restart),
        label = label,
    )

/** Chiziqli takrorlanuvchi faza 0..1 (rasm va bezaklar uchun). */
@Composable
internal fun loopState(label: String, durationMs: Int, off: Boolean, resting: Float = 0f): State<Float> = loop(label, durationMs, reverse = false, off = off, resting = resting)

/**
 * Orvell'ning asosiy belgisi — "shisha naycha" orbitalar (docs/UI-REDESIGN.md §4.1):
 * chuqurcha (trek), gradientli naycha, yuqoridan yaltirash, uchida sayyora + kometa izi.
 * O'lchash paytida halo yurak urishidek uradi; maqsad yopilganda bir marta zarrachalar sochiladi.
 */
@Composable
fun OrbitRings(
    rings: List<Ring>,
    modifier: Modifier = Modifier,
    stroke: Dp = 18.dp,
    gap: Dp = 8.dp,
    measuring: Boolean = false,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = O.c
    val reduce = O.reduceMotion
    val haptics = rememberHaptics()
    val anims = remember(rings.size) { List(rings.size) { Animatable(if (reduce) rings[it].progress.coerceIn(0f, 1f) else 0f) } }
    val burst = remember { Animatable(0f) }
    var burstRing by remember { mutableIntStateOf(-1) }
    rings.forEachIndexed { i, r ->
        val target = r.progress.coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
        LaunchedEffect(target, reduce) {
            val before = anims[i].value
            if (reduce) anims[i].snapTo(target) else anims[i].animateTo(target, tween(Motion.FILL_MS, easing = Motion.EaseOutCubic))
            // ekran ochilganda emas, kun davomida maqsad yopilganda nishonlaymiz
            if (before in 0.01f..0.999f && target >= 1f) {
                burstRing = i
                haptics.celebrate()
                if (!reduce) { burst.snapTo(0f); burst.animateTo(1f, tween(900, easing = Motion.EaseOutCubic)) }
            }
        }
    }
    val breathe = loop("trail", 2600, reverse = true, off = reduce, resting = 0.5f)
    val beat = loop("beat", 900, reverse = false, off = reduce || !measuring)
    val dark = c.isDark
    val track = c.track
    val tones = rings.map { it.tone }

    Box(
        modifier
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .drawWithCache {
                val sw = stroke.toPx()
                val g = gap.toPx()
                val radii = FloatArray(tones.size) { size.minDimension / 2f - sw * 1.25f - it * (sw + g) }
                val tube = Stroke(sw, cap = StrokeCap.Round)
                val groove = Stroke(sw)
                val edge = Stroke(1.dp.toPx())
                val spec = Stroke(sw * 0.2f, cap = StrokeCap.Round)
                val glowStroke = Stroke(sw * 1.9f)
                val mid = size.center
                val sweeps = tones.map { Brush.sweepGradient(0f to it.b, 0.75f to it.a, 1f to it.a, center = mid) }
                val haloR = sw * 1.9f
                val halos = tones.map { Brush.radialGradient(listOf(it.a.copy(alpha = 0.85f), it.a.copy(alpha = 0.25f), Color.Transparent), center = Offset.Zero, radius = haloR) }
                val dot = sw * 0.26f
                onDrawBehind {
                    for (i in tones.indices) {
                        val r = radii[i]
                        if (r <= sw) continue
                        val tone = tones[i]
                        // chuqurcha: trek + ichki qorong'i va tashqi yorug' qirra
                        drawCircle(track, r, center, style = groove)
                        drawCircle(tone.a.copy(alpha = 0.06f), r, center, style = groove)
                        drawCircle(Color.Black.copy(alpha = if (dark) 0.35f else 0.06f), r - sw / 2f, center, style = edge)
                        drawCircle(Color.White.copy(alpha = if (dark) 0.05f else 0.6f), r + sw / 2f, center, style = edge)
                        val p = anims[i].value
                        if (p <= 0.002f) continue
                        val sweep = 360f * p
                        val tl = Offset(center.x - r, center.y - r)
                        val arc = Size(r * 2, r * 2)
                        if (p >= 0.999f) drawCircle(tone.a.copy(alpha = 0.14f), r, center, style = glowStroke)
                        rotate(-90f, center) {
                            drawArc(sweeps[i], 0f, sweep, false, tl, arc, style = tube)
                            // yorug'lik yuqoridan: naycha ichki qirrasida ingichka yaltirash
                            val rs = r - sw * 0.2f
                            if (sweep > 6f) drawArc(Color.White.copy(alpha = if (dark) 0.26f else 0.45f), 3f, sweep - 6f, false,
                                Offset(center.x - rs, center.y - rs), Size(rs * 2, rs * 2), style = spec)
                        }
                        val ang = (sweep - 90f) * (PI / 180.0).toFloat()
                        val px = center.x + r * cos(ang)
                        val py = center.y + r * sin(ang)
                        // kometa izi: sayyora ortidagi so'nayotgan nuqtalar
                        if (p > 0.08f) for (k in 1..TRAIL) {
                            val a = ang - k * TRAIL_STEP
                            val alpha = (1f - k / (TRAIL + 1f)) * (0.35f + 0.3f * breathe.value)
                            drawCircle(Color.White.copy(alpha = alpha * 0.8f), dot * (1f - k * 0.13f), Offset(center.x + r * cos(a), center.y + r * sin(a)))
                        }
                        val pulse = if (measuring) 1f + 0.45f * sin(beat.value * PI.toFloat()) else 1f
                        translate(px, py) {
                            scale(pulse, pulse, pivot = Offset.Zero) { drawCircle(halos[i], haloR, Offset.Zero) }
                            drawCircle(Color.White, dot, Offset.Zero)
                            drawCircle(tone.a, dot, Offset.Zero, style = edge)
                        }
                    }
                    // maqsad yopildi: sayyoradan zarrachalar sochiladi
                    val bp = burst.value
                    val br = burstRing
                    if (bp > 0f && bp < 1f && br in tones.indices) {
                        val r = radii[br]
                        val ang = (360f * anims[br].value - 90f) * (PI / 180.0).toFloat()
                        val ox = center.x + r * cos(ang)
                        val oy = center.y + r * sin(ang)
                        for (k in 0 until BURST) {
                            val a = 2f * PI.toFloat() * k / BURST + k * 0.37f
                            val d = sw * (0.6f + bp * (2.2f + (k % 3) * 0.6f))
                            drawCircle(tones[br].a.copy(alpha = (1f - bp) * 0.9f), dot * (1f - bp * 0.5f), Offset(ox + d * cos(a), oy + d * sin(a)))
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Kichik orbita: progressni ko'rsatadi, `spinning` bo'lsa sayyora aylanadi (yuklanish/yangilash). */
@Composable
fun OrbitSpinner(fraction: Float, spinning: Boolean, modifier: Modifier = Modifier, tone: Tone = Tones.brand, size: Dp = 28.dp, stroke: Dp = 3.dp) {
    val c = O.c
    val spin = loop("spin", 1100, reverse = false, off = O.reduceMotion || !spinning)
    Box(
        modifier.size(size).drawWithCache {
            val sw = stroke.toPx()
            val r = this.size.minDimension / 2f - sw * 1.5f
            val st = Stroke(sw, cap = StrokeCap.Round)
            val brush = Brush.sweepGradient(listOf(tone.b, tone.a), center = this.size.center)
            onDrawBehind {
                drawCircle(c.track, r, center, style = Stroke(sw))
                val f = fraction.coerceIn(0f, 1f)
                val rot = if (spinning) spin.value * 360f else 0f
                rotate(rot - 90f, center) {
                    val sweep = if (spinning) 110f else 360f * f
                    if (sweep > 0.5f) drawArc(brush, 0f, sweep, false, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = st)
                    val a = sweep * (PI / 180.0).toFloat()
                    if (sweep > 0.5f) drawCircle(Color.White, sw * 0.9f, Offset(center.x + r * cos(a), center.y + r * sin(a)))
                }
            }
        },
    )
}

/** Brendli "pastga tortib yangilash" ko'rsatkichi: tortgan sari orbita to'ladi, yangilanayotganda sayyora aylanadi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitRefreshIndicator(state: PullToRefreshState, isRefreshing: Boolean, modifier: Modifier = Modifier) {
    val c = O.c
    Box(
        modifier
            .graphicsLayer {
                val f = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1.4f)
                translationY = (f * 72.dp.toPx()) - 52.dp.toPx()
                alpha = if (isRefreshing) 1f else (f * 1.6f).coerceIn(0f, 1f)
                scaleX = 0.7f + 0.3f * f.coerceAtMost(1f); scaleY = scaleX
            }
            .size(48.dp)
            .clip(CircleShape)
            .background(c.raised)
            .border(1.dp, Brush.verticalGradient(listOf(c.highlight, c.stroke)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        OrbitSpinner(fraction = state.distanceFraction, spinning = isRefreshing)
    }
}
