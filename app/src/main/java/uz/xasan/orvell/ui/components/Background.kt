package uz.xasan.orvell.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.Tone
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val NOISE = 64

/** 64 px shovqin plitkasi: bir marta yasaladi, gradient "zinapoya"sini (banding) yo'qotadi. */
private fun noiseTile(): ImageBitmap {
    val img = ImageBitmap(NOISE, NOISE)
    val canvas = Canvas(img)
    val rnd = Random(7)
    // 4 xil yorqinlikdagi nuqtalar guruhi — har biri bitta drawPoints chaqiruvi
    val levels = listOf(0.25f, 0.5f, 0.75f, 1f)
    val groups = levels.map { mutableListOf<Offset>() }
    for (y in 0 until NOISE) for (x in 0 until NOISE) {
        val r = rnd.nextFloat()
        if (r > 0.55f) groups[((r - 0.55f) / 0.45f * levels.size).toInt().coerceIn(0, levels.lastIndex)].add(Offset(x + 0.5f, y + 0.5f))
    }
    groups.forEachIndexed { i, pts ->
        canvas.drawPoints(PointMode.Points, pts, Paint().apply { color = Color.White.copy(alpha = levels[i]); strokeWidth = 1f })
    }
    return img
}

/**
 * Ekran foni "aurora": joriy kontekst rangidagi 2 ta katta yorug'lik hovuzi + shovqin.
 * Rang almashganda 600 ms da o'tadi; hovuzlar juda sekin suzadi (reduce motion bo'lsa to'xtaydi).
 */
@Composable
fun AuroraBackground(tone: Tone?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = O.c
    val reduce = O.reduceMotion
    val primary by animateColorAsState(tone?.a ?: c.bgGlow, tween(600), label = "auroraA")
    val secondary by animateColorAsState(tone?.b ?: c.bgGlow, tween(600), label = "auroraB")
    val drift: State<Float> = if (reduce) remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        else rememberInfiniteTransition(label = "aurora").animateFloat(0f, 1f, infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Restart), label = "drift")
    val noise = remember { noiseTile() }
    val strength = if (c.isDark) 0.30f else 0.55f
    Box(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .drawWithCache {
                val r1 = size.width * 1.05f
                val r2 = size.width * 0.9f
                // markazi (0,0) bo'lgan cho'tkalar: kadrda faqat siljitiladi (har kadrda yangi obyekt yo'q)
                val b1 = Brush.radialGradient(listOf(primary.copy(alpha = strength), Color.Transparent), center = Offset.Zero, radius = r1)
                val b2 = Brush.radialGradient(listOf(secondary.copy(alpha = strength * 0.6f), Color.Transparent), center = Offset.Zero, radius = r2)
                val grain = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
                onDrawBehind {
                    val a = drift.value * 2f * Math.PI.toFloat()
                    translate(size.width * (0.15f + 0.06f * cos(a)), -size.width * (0.2f + 0.04f * sin(a))) { drawCircle(b1, r1, Offset.Zero) }
                    translate(size.width * (0.95f + 0.05f * sin(a)), size.height * (0.38f + 0.03f * cos(a))) { drawCircle(b2, r2, Offset.Zero) }
                    drawRect(grain, alpha = if (c.isDark) 0.035f else 0.025f)
                }
            },
        content = content,
    )
}
