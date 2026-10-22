package uz.xasan.orvell.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.R
import uz.xasan.orvell.data.StageSpan
import uz.xasan.orvell.ui.logic.ChartMath
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones
import kotlin.math.roundToInt

// Grafiklar: barmoq bilan surib aniq qiymatni ko'rish, haptik "tiq", animatsiyali paydo bo'lish.
// Cho'tkalar drawWithCache'da; animatsiya va tanlov faqat chizish fazasida o'qiladi.

/** Ekran ochilganda 0 dan 1 gacha o'sish (reduce motion: darhol 1). State qaytaradi — chizishda o'qiladi. */
@Composable
fun rememberGrow(durationMs: Int = Motion.FILL_MS, key: Any? = Unit): State<Float> {
    val reduce = O.reduceMotion
    val a = remember(key) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(key) { if (!reduce) a.animateTo(1f, tween(durationMs, easing = Motion.EaseOutCubic)) }
    return a.asState()
}

/** Eski API (moslik uchun): o'sayotgan qiymat. */
@Composable
fun animatedProgress(target: Float, durationMs: Int = Motion.FILL_MS): Float = rememberGrow(durationMs).value * target

/** Grafik ustidagi qiymat pufakchasi. */
@Composable
fun ChartTooltip(text: String, modifier: Modifier = Modifier) {
    val c = O.c
    Text(
        text, style = OType.label, color = c.text, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(c.raised)
            .border(1.dp, c.strokeHi, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Tooltip qatori: pufakcha x nuqta ustida, chetlardan chiqmaydi. */
@Composable
private fun TooltipLane(text: String?, centerX: Float) {
    Box(Modifier.fillMaxWidth().height(34.dp)) {
        if (text != null) ChartTooltip(text, Modifier.layout { m, cons ->
            val p = m.measure(cons.copy(minWidth = 0))
            layout(cons.maxWidth, p.height) {
                p.place((centerX - p.width / 2f).roundToInt().coerceIn(0, (cons.maxWidth - p.width).coerceAtLeast(0)), 0)
            }
        })
    }
}

/**
 * Ustunli grafik. `valueText` berilsa — interaktiv: bosish yoki surish ustunni tanlaydi, ustida aniq qiymat,
 * har yangi ustunda haptik. Maqsad chizig'i punktir va "maqsad" yorlig'i bilan.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    tone: Tone,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    highlight: Int = values.lastIndex,
    height: Dp = 150.dp,
    valueText: ((Int) -> String)? = null,
    description: String? = null,
) {
    val c = O.c
    val haptics = rememberHaptics()
    val grow = rememberGrow(900, values.size)
    var sel by remember(values.size) { mutableIntStateOf(-1) }
    LaunchedEffect(sel) { if (sel >= 0) haptics.tick() }
    val tm = rememberTextMeasurer()
    val goalLabel = stringResource(R.string.chart_goal)
    val goalText = remember(goalLabel, c.text3) { tm.measure(goalLabel, OType.label.copy(color = c.text3)) }
    val n = values.size
    BoxWithConstraints(modifier.fillMaxWidth().then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)) {
        val w = constraints.maxWidth.toFloat()
        Column {
            if (valueText != null) TooltipLane(if (sel in 0 until n) valueText(sel) else null, if (n > 0) w / n * (sel + 0.5f) else 0f)
            Box(
                Modifier.fillMaxWidth().height(height)
                    .then(if (valueText != null && n > 0) Modifier
                        .pointerInput(n) { detectTapGestures { o -> val i = ChartMath.barIndex(o.x, size.width.toFloat(), n); sel = if (sel == i) -1 else i } }
                        .pointerInput(n) {
                            detectHorizontalDragGestures(onDragEnd = {}) { change, _ -> sel = ChartMath.barIndex(change.position.x, size.width.toFloat(), n) }
                        } else Modifier)
                    .drawWithCache {
                        val maxV = maxOf(values.maxOrNull() ?: 0f, goal ?: 0f, 1f) * 1.12f
                        val slot = size.width / n.coerceAtLeast(1)
                        val bw = (slot * 0.56f).coerceAtMost(28.dp.toPx())
                        val bar = Brush.verticalGradient(listOf(tone.a, tone.b), startY = 0f, endY = size.height)
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        val r = CornerRadius(bw / 2, bw / 2)
                        val minH = 3.dp.toPx()
                        val lift = 4.dp.toPx()
                        onDrawBehind {
                            val g = grow.value
                            val s = sel
                            for (i in 0 until n) {
                                val v = values[i]
                                val x = slot * i + (slot - bw) / 2
                                val h = if (v > 0) (v / maxV * size.height * g).coerceAtLeast(minH + 1f) else minH
                                val chosen = if (s >= 0) i == s else i == highlight
                                val top = size.height - h - if (s == i) lift else 0f
                                if (v <= 0) drawRoundRect(c.track, Offset(x, size.height - minH), Size(bw, minH), r)
                                else {
                                    drawRoundRect(bar, Offset(x, top), Size(bw, h), r, alpha = if (chosen) 1f else if (s >= 0) 0.28f else 0.5f)
                                    if (chosen) drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(x + bw * 0.22f, top + bw * 0.25f),
                                        Size(bw * 0.18f, (h - bw * 0.5f).coerceAtLeast(0f)), r)
                                }
                            }
                            if (goal != null && goal > 0f) {
                                val y = size.height - goal / maxV * size.height
                                drawLine(c.text3, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = dash)
                                drawText(goalText, topLeft = Offset(size.width - goalText.size.width, (y - goalText.size.height - 2.dp.toPx()).coerceAtLeast(0f)))
                            }
                        }
                    },
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, l ->
                    val on = if (sel >= 0) i == sel else i == highlight
                    // Belgi ustundan keng bo'lishi mumkin (oyda "28"): kesilmaydi, ustun markazida chiqadi
                    Text(l, style = OType.label, color = if (on) c.text else c.text3, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                        modifier = Modifier.weight(1f).wrapContentWidth(unbounded = true))
                }
            }
        }
    }
}

/**
 * Silliq chiziqli grafik (yurak urishi, SpO₂): xs vaqt bo'yicha 0..1, ys qiymatlar.
 * Monoton egri chiziq (haqiqiy qiymatdan oshmaydi), gradient to'ldirish, o'ngda o'q belgilari.
 * `valueText` berilsa — surilganda nishon chizig'i, nuqta va aniq qiymat.
 */
@Composable
fun LineChart(
    xs: List<Float>,
    ys: List<Float>,
    tone: Tone,
    modifier: Modifier = Modifier,
    minY: Float? = null,
    maxY: Float? = null,
    height: Dp = 160.dp,
    axis: Boolean = true,
    fill: Boolean = true,
    valueText: ((Int) -> String)? = null,
    description: String? = null,
) {
    val c = O.c
    val haptics = rememberHaptics()
    val grow = rememberGrow(1000, ys.size)
    var sel by remember(ys.size) { mutableIntStateOf(-1) }
    LaunchedEffect(sel) { if (sel >= 0) haptics.tick() }
    val tm = rememberTextMeasurer()
    val lo = minY ?: ((ys.minOrNull() ?: 0f) - 5f)
    val hi = maxY ?: ((ys.maxOrNull() ?: 1f) + 5f)
    val axisTexts = remember(lo, hi, axis, c.text3) {
        if (!axis) emptyList() else List(3) { k -> tm.measure((lo + (hi - lo).coerceAtLeast(1f) * k / 2f).roundToInt().toString(), OType.label.copy(color = c.text3)) }
    }
    BoxWithConstraints(modifier.fillMaxWidth().then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)) {
        val right = if (axis) with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.toPx() } else 0f
        val plotW = constraints.maxWidth - right
        Column {
            if (valueText != null) TooltipLane(if (sel in ys.indices) valueText(sel) else null, if (sel in xs.indices) xs[sel] * plotW else 0f)
            Box(
                Modifier.fillMaxWidth().height(height)
                    .then(if (valueText != null && ys.size >= 2) Modifier
                        .pointerInput(xs) { detectTapGestures { o -> val i = ChartMath.nearestIndex(o.x / (size.width - right), xs); sel = if (sel == i) -1 else i } }
                        .pointerInput(xs) {
                            detectHorizontalDragGestures(onDragEnd = { }) { ch, _ -> sel = ChartMath.nearestIndex(ch.position.x / (size.width - right), xs) }
                        } else Modifier)
                    .drawWithCache {
                        val n = ys.size
                        val w = size.width - right
                        val top = 6.dp.toPx()
                        val h = size.height - top * 2
                        val span = (hi - lo).coerceAtLeast(1f)
                        val px = FloatArray(n) { xs[it] * w }
                        val py = FloatArray(n) { top + h - (ys[it] - lo) / span * h }
                        val line = Path()
                        val area = Path()
                        if (n >= 2) {
                            val m = ChartMath.monotoneSlopes(px, py)
                            line.moveTo(px[0], py[0])
                            for (i in 0 until n - 1) {
                                val dx = (px[i + 1] - px[i]) / 3f
                                line.cubicTo(px[i] + dx, py[i] + m[i] * dx, px[i + 1] - dx, py[i + 1] - m[i + 1] * dx, px[i + 1], py[i + 1])
                            }
                            area.addPath(line); area.lineTo(px[n - 1], top + h); area.lineTo(px[0], top + h); area.close()
                        }
                        val fillBrush = Brush.verticalGradient(listOf(tone.a.copy(alpha = 0.34f), tone.a.copy(alpha = 0f)), startY = top, endY = top + h)
                        val stroke = Brush.horizontalGradient(listOf(tone.b, tone.a), startX = 0f, endX = w)
                        val st = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                        val haloR = 14.dp.toPx()
                        val halo = Brush.radialGradient(listOf(tone.a.copy(alpha = 0.7f), Color.Transparent), center = Offset.Zero, radius = haloR)
                        val dot = 3.5.dp.toPx()
                        onDrawBehind {
                            if (n < 2) return@onDrawBehind
                            if (axis) for (k in 0..2) {
                                val y = top + h - h * k / 2f
                                drawLine(c.stroke, Offset(0f, y), Offset(w, y), 1.dp.toPx())
                                axisTexts.getOrNull(k)?.let { drawText(it, topLeft = Offset(w + 8.dp.toPx(), y - it.size.height / 2f)) }
                            }
                            clipRect(right = w * grow.value + 1f) {
                                if (fill) drawPath(area, fillBrush)
                                drawPath(line, stroke, style = st)
                            }
                            val s = sel
                            val i = if (s in 0 until n) s else n - 1
                            if (s in 0 until n) drawLine(c.text3, Offset(px[s], top), Offset(px[s], top + h), 1.dp.toPx())
                            if (grow.value >= 0.99f || s >= 0) translate(px[i], py[i]) {
                                drawCircle(halo, haloR, Offset.Zero)
                                drawCircle(Color.White, dot, Offset.Zero)
                            }
                        }
                    },
            )
        }
    }
}

/** Kichik egri chiziq (kartochkalar ichida), oxirida nuqta. */
@Composable
fun Sparkline(values: List<Float>, tone: Tone, modifier: Modifier = Modifier, height: Dp = 36.dp) {
    if (values.size < 2) { Spacer(modifier.height(height)); return }
    val xs = remember(values.size) { List(values.size) { it / (values.size - 1f) } }
    LineChart(xs, values, tone, modifier, height = height, axis = false)
}

fun stageColor(stage: Int): Color = when (stage) { 5 -> Tones.deep; 4 -> Tones.light; 6 -> Tones.rem; else -> Tones.awake }
private fun stageRow(stage: Int) = when (stage) { 1 -> 0; 6 -> 1; 4 -> 2; 5 -> 3; else -> 0 }
private val laneStages = intArrayOf(1, 6, 4, 5)

/**
 * Uyqu gipnogrammasi: 4 yo'lak (uyg'oq, REM, yengil, chuqur) o'z rangida yengil bo'yalgan, bloklar yumaloq,
 * orasida ulovchi chiziqlar. Blokka bosilsa — bosqich, vaqt oralig'i va davomiyligi ko'rsatiladi.
 */
@Composable
fun Hypnogram(
    spans: List<StageSpan>,
    from: Long,
    to: Long,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    inspectText: ((StageSpan) -> String)? = null,
    description: String? = null,
) {
    val c = O.c
    val haptics = rememberHaptics()
    val grow = rememberGrow(1100, spans.size)
    val sorted = remember(spans) { spans.sortedBy { it.start } }
    var sel by remember(spans) { mutableIntStateOf(-1) }
    LaunchedEffect(sel) { if (sel >= 0) haptics.tick() }
    val total = (to - from).toFloat()
    BoxWithConstraints(modifier.fillMaxWidth().then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)) {
        val w = constraints.maxWidth.toFloat()
        Column {
            if (inspectText != null) {
                val s = sorted.getOrNull(sel)
                TooltipLane(s?.let(inspectText), if (s != null && total > 0) ((s.start + s.end) / 2f - from) / total * w else 0f)
            }
            Box(
                Modifier.fillMaxWidth().height(height)
                    .then(if (inspectText != null && total > 0) Modifier.pointerInput(sorted) {
                        detectTapGestures { o ->
                            val t = from + o.x / size.width * total
                            val i = sorted.indexOfFirst { t >= it.start && t <= it.end }
                            sel = if (i == sel) -1 else i
                        }
                    } else Modifier)
                    .drawWithCache {
                        val rowH = size.height / 4f
                        val bh = rowH * 0.62f
                        val lanes = List(4) { r -> Brush.verticalGradient(listOf(stageColor(laneStages[r]), stageColor(laneStages[r]).copy(alpha = 0.7f)),
                            startY = rowH * r + (rowH - bh) / 2, endY = rowH * r + (rowH + bh) / 2) }
                        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 10f))
                        val corner = CornerRadius(6.dp.toPx())
                        val connector = 1.5.dp.toPx()
                        onDrawBehind {
                            if (sorted.isEmpty() || total <= 0f) return@onDrawBehind
                            for (r in 0..3) {
                                drawRect(stageColor(laneStages[r]).copy(alpha = if (c.isDark) 0.05f else 0.07f), Offset(0f, rowH * r), Size(size.width, rowH))
                                drawLine(c.stroke, Offset(0f, rowH * r + rowH / 2), Offset(size.width, rowH * r + rowH / 2), 1.dp.toPx(), pathEffect = dash)
                            }
                            val limit = size.width * grow.value
                            var prevX = -1f
                            var prevY = 0f
                            val s = sel
                            for ((i, sp) in sorted.withIndex()) {
                                val x0 = (sp.start - from) / total * size.width
                                val x1 = ((sp.end - from) / total * size.width).coerceAtMost(limit)
                                if (x1 <= x0) continue
                                val row = stageRow(sp.stage)
                                val y = rowH * row + (rowH - bh) / 2
                                val col = stageColor(sp.stage)
                                if (prevX >= 0f) drawLine(col.copy(alpha = 0.35f), Offset(x0, prevY), Offset(x0, y + bh / 2), connector)
                                val a = if (s < 0 || s == i) 1f else 0.35f
                                drawRoundRect(lanes[row], Offset(x0, y), Size(x1 - x0, bh), corner, alpha = a)
                                drawRoundRect(Color.White.copy(alpha = 0.18f * a), Offset(x0 + 2.dp.toPx(), y + 2.dp.toPx()),
                                    Size((x1 - x0 - 4.dp.toPx()).coerceAtLeast(0f), 2.dp.toPx()), CornerRadius(2.dp.toPx()))
                                if (s == i) drawRoundRect(Color.White, Offset(x0, y), Size(x1 - x0, bh), corner, style = Stroke(1.5.dp.toPx()))
                                prevX = x1; prevY = y + bh / 2
                            }
                        }
                    },
            )
        }
    }
}

/** Yurak zonalari ranglari: dam (neytral) → issiq. */
val zoneColors = listOf(Color(0xFF7D8799), Color(0xFF4F8CFF), Color(0xFF45E3F2), Color(0xFFC8F56A), Color(0xFFFFB04A), Color(0xFFFF5D73))

/** Zonalardagi vaqt: bitta ko'p rangli chiziq + har zona qatori (nom, daqiqa). */
@Composable
fun HrZonesBar(minutes: IntArray, labels: List<String>, format: (Int) -> String, modifier: Modifier = Modifier) {
    val c = O.c
    val total = minutes.sum().coerceAtLeast(1)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(c.track), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            minutes.forEachIndexed { i, m -> if (m > 0) Box(Modifier.weight(m.toFloat() / total).fillMaxSize().background(zoneColors[i])) }
        }
        for (i in minutes.indices.reversed()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(zoneColors[i]))
                Spacer(Modifier.width(8.dp))
                Text(labels.getOrElse(i) { "" }, style = OType.caption, color = c.text2, modifier = Modifier.weight(1f))
                Text(format(minutes[i]), style = OType.bodyStrong, color = if (minutes[i] > 0) c.text else c.text3)
            }
        }
    }
}

/** Bosqichlar ulushi: bitta yumaloq chiziqda. */
@Composable
fun StageBar(parts: List<Pair<Color, Float>>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val total = parts.sumOf { it.second.toDouble() }.toFloat()
    Row(modifier.fillMaxWidth().height(height).clip(CircleShape).background(O.c.track), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (total > 0) parts.filter { it.second > 0 }.forEach { (col, v) ->
            Box(Modifier.weight(v / total).fillMaxSize().background(Brush.verticalGradient(listOf(col, col.copy(alpha = 0.75f)))))
        }
    }
}

/** Ingichka progress chizig'i (o'sish chizish fazasida). */
@Composable
fun ProgressLine(progress: Float, tone: Tone, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val grow = rememberGrow()
    val c = O.c
    val p = progress.coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
    Box(
        modifier.fillMaxWidth().height(height).clip(CircleShape).background(c.track).drawWithCache {
            val brush = Brush.horizontalGradient(listOf(tone.b, tone.a), startX = 0f, endX = size.width)
            onDrawBehind { drawRoundRect(brush, size = Size(size.width * p * grow.value, size.height), cornerRadius = CornerRadius(size.height / 2)) }
        },
    )
}

@Composable
fun LegendDot(color: Color, text: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, style = OType.caption, color = O.c.text2, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(6.dp))
        Text(value, style = OType.bodyStrong, color = O.c.text)
    }
}
