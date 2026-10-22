package uz.xasan.orvell.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.Tones
import java.time.LocalTime

/**
 * Premium soat tasviri: metall korpus, gardish, shisha yaltirashi va jonli siferblat
 * (vaqt, batareya halqasi, qadam yoyi). Orqasidagi nur ulanish holati rangida. Matn har daqiqada yangilanadi.
 */
@Composable
fun WatchRender(
    modifier: Modifier = Modifier,
    battery: Int = -1,
    stepsFraction: Float = 0f,
    stateColor: Color = Tones.oxygen.a,
    glowing: Boolean = true,
    floating: Boolean = true,
    size: Dp = 128.dp,
    contentDescription: String? = null,
) {
    val reduce = O.reduceMotion
    val dark = O.c.isDark
    val bob = loop("watchFloat", 2600, reverse = true, off = reduce || !floating, resting = 0.5f)
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); now = LocalTime.now() } }
    val tm = rememberTextMeasurer()
    val density = LocalDensity.current
    // siferblat matni shrift o'lchamiga bog'liq emas (rasm qismi), shuning uchun px'dan sp
    val timeStyle = remember(size, density) { OType.display.copy(color = Color.White, fontSize = with(density) { (size * 0.2f).toSp() }, letterSpacing = with(density) { (-size * 0.006f).toSp() }) }
    val smallStyle = remember(size, density) { TextStyle(fontFamily = OType.label.fontFamily, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.65f), fontSize = with(density) { (size * 0.085f).toSp() }) }
    val timeText = "%02d:%02d".format(now.hour, now.minute)
    val timeLayout = remember(timeText, timeStyle) { tm.measure(timeText, timeStyle) }
    val subText = if (battery >= 0) "$battery%" else "Orvell"
    val subLayout = remember(subText, smallStyle) { tm.measure(subText, smallStyle) }
    val batt = (battery.coerceIn(0, 100)) / 100f
    val steps = stepsFraction.coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }

    Spacer(
        modifier
            .size(size, size * 1.3f)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .graphicsLayer { translationY = (bob.value - 0.5f) * 6.dp.toPx() }
            .drawWithCache {
                val w = this.size.width; val h = this.size.height
                val bw = w * 0.74f; val bh = h * 0.56f
                val bx = (w - bw) / 2; val by = (h - bh) / 2
                val shadow = Brush.radialGradient(listOf(Color.Black.copy(alpha = if (dark) 0.6f else 0.22f), Color.Transparent), center = Offset(w / 2, h * 0.97f), radius = w * 0.4f)
                val glow = Brush.radialGradient(listOf(stateColor.copy(alpha = if (dark) 0.32f else 0.22f), Color.Transparent), center = Offset(w / 2, h / 2), radius = w * 0.8f)
                val strap = Brush.horizontalGradient(listOf(Color(0xFF151922), Color(0xFF2E3443), Color(0xFF151922)), startX = w * 0.25f, endX = w * 0.75f)
                val strapHole = Color(0xFF0B0D12)
                val case = Brush.linearGradient(listOf(Color(0xFFB4BCCB), Color(0xFF5A6273), Color(0xFF1C2029), Color(0xFF7A8396), Color(0xFFCDD3DE)),
                    start = Offset(bx, by), end = Offset(bx + bw, by + bh))
                val bezel = Brush.verticalGradient(listOf(Color(0xFF2A2F3B), Color(0xFF0A0C11)), startY = by, endY = by + bh)
                val crown = Brush.verticalGradient(listOf(Color(0xFFB8C0CE), Color(0xFF3B4250)), startY = by + bh * 0.3f, endY = by + bh * 0.5f)
                val inset = bw * 0.075f
                val sx = bx + inset; val sy = by + inset; val sw = bw - inset * 2; val sh = bh - inset * 2
                val screen = Brush.verticalGradient(listOf(Color(0xFF0B0F18), Color(0xFF020306)), startY = sy, endY = sy + sh)
                val r = sw * 0.32f
                val cx = sx + sw / 2; val cy = sy + sh * 0.46f
                val battBrush = Brush.sweepGradient(listOf(Tones.activity.b, Tones.activity.a), center = Offset(cx, cy))
                val ringStroke = Stroke(sw * 0.055f, cap = StrokeCap.Round)
                val stepR = r * 0.78f
                val stepStroke = Stroke(sw * 0.035f, cap = StrokeCap.Round)
                val reflect = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), start = Offset(sx, sy), end = Offset(sx + sw * 0.55f, sy + sh * 0.55f))
                val corner = CornerRadius(bw * 0.25f)
                onDrawBehind {
                    drawOval(shadow, Offset(w * 0.15f, h * 0.91f), Size(w * 0.7f, h * 0.09f))
                    if (glowing) drawCircle(glow, w * 0.8f, Offset(w / 2, h / 2))
                    // tasmalar (teshiklari bilan)
                    drawRoundRect(strap, Offset(w * 0.27f, 0f), Size(w * 0.46f, by + 12f), CornerRadius(14f))
                    drawRoundRect(strap, Offset(w * 0.27f, by + bh - 12f), Size(w * 0.46f, h * 0.9f - (by + bh) + 12f), CornerRadius(14f))
                    for (k in 0..2) drawCircle(strapHole, w * 0.018f, Offset(w / 2, by + bh + (h * 0.9f - by - bh) * (0.3f + k * 0.22f)))
                    // toj (crown)
                    drawRoundRect(crown, Offset(bx + bw - 2f, by + bh * 0.3f), Size(w * 0.06f, bh * 0.2f), CornerRadius(6f))
                    // metall korpus va gardish
                    drawRoundRect(case, Offset(bx, by), Size(bw, bh), corner)
                    drawRoundRect(Color.White.copy(alpha = 0.4f), Offset(bx, by), Size(bw, bh), corner, style = Stroke(1.2f))
                    drawRoundRect(bezel, Offset(bx + inset * 0.45f, by + inset * 0.45f), Size(bw - inset * 0.9f, bh - inset * 0.9f), CornerRadius(bw * 0.22f))
                    drawRoundRect(screen, Offset(sx, sy), Size(sw, sh), CornerRadius(sw * 0.2f))
                    // siferblat: batareya halqasi va qadam yoyi
                    drawCircle(Color.White.copy(alpha = 0.08f), r, Offset(cx, cy), style = ringStroke)
                    rotate(-90f, Offset(cx, cy)) {
                        if (battery >= 0 && batt > 0f) drawArc(battBrush, 0f, 360f * batt, false, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = ringStroke)
                        if (steps > 0f) drawArc(Tones.oxygen.a.copy(alpha = 0.9f), 0f, 360f * steps, false, Offset(cx - stepR, cy - stepR), Size(stepR * 2, stepR * 2), style = stepStroke)
                    }
                    drawText(timeLayout, topLeft = Offset(cx - timeLayout.size.width / 2f, cy - timeLayout.size.height / 2f))
                    drawText(subLayout, topLeft = Offset(cx - subLayout.size.width / 2f, sy + sh * 0.84f - subLayout.size.height / 2f))
                    // holat nuqtasi
                    translate(sx + sw * 0.5f, sy + sh * 0.12f) { drawCircle(stateColor, sw * 0.022f, Offset.Zero) }
                    // shisha yaltirashi
                    drawRoundRect(reflect, Offset(sx, sy), Size(sw, sh), CornerRadius(sw * 0.2f))
                }
            },
    )
}
