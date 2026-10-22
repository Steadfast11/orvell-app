package uz.xasan.orvell.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.R
import uz.xasan.orvell.data.AppSettings
import uz.xasan.orvell.data.DayStats
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.data.StorageMode
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.AnimatedNumber
import uz.xasan.orvell.ui.components.AuroraBackground
import uz.xasan.orvell.ui.components.BarChart
import uz.xasan.orvell.ui.components.CircleIconButton
import uz.xasan.orvell.ui.components.LegacyNotice
import uz.xasan.orvell.ui.components.HrZonesBar
import uz.xasan.orvell.ui.components.Hypnogram
import uz.xasan.orvell.ui.components.IconBadge
import uz.xasan.orvell.ui.components.LargeTitle
import uz.xasan.orvell.ui.components.LegendDot
import uz.xasan.orvell.ui.components.LineChart
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.ProgressLine
import uz.xasan.orvell.ui.components.SectionHeader
import uz.xasan.orvell.ui.components.Segmented
import uz.xasan.orvell.ui.components.SkeletonCard
import uz.xasan.orvell.ui.components.Sparkline
import uz.xasan.orvell.ui.components.TopBar
import uz.xasan.orvell.ui.components.ValueText
import uz.xasan.orvell.ui.components.staggered
import uz.xasan.orvell.ui.logic.HrZones
import uz.xasan.orvell.ui.nav.sharedContainer
import uz.xasan.orvell.ui.nav.sharedKey
import uz.xasan.orvell.ui.preview.OrvellPreviews
import uz.xasan.orvell.ui.preview.PreviewData
import uz.xasan.orvell.ui.preview.PreviewSurface
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Space
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** Ko'rsatkich turlari: nom, rang, belgi va qisqa tushuntirish. */
data class MetricKind(val id: String, @StringRes val title: Int, val tone: Tone, val icon: ImageVector, @StringRes val about: Int)

val metricKinds = listOf(
    MetricKind("activity", R.string.kind_activity, Tones.activity, Icons.Rounded.Bolt, R.string.about_activity),
    MetricKind("heart", R.string.kind_heart, Tones.heart, Icons.Rounded.Favorite, R.string.about_heart),
    MetricKind("sleep", R.string.kind_sleep, Tones.sleep, Icons.Rounded.Bedtime, R.string.about_sleep),
    MetricKind("oxygen", R.string.kind_oxygen, Tones.oxygen, Icons.Rounded.WaterDrop, R.string.about_oxygen),
    MetricKind("pressure", R.string.kind_pressure, Tones.pressure, Icons.Rounded.MonitorHeart, R.string.about_pressure),
    MetricKind("breath", R.string.kind_breath, Tones.breath, Icons.Rounded.Air, R.string.about_breath),
)

fun kindOf(id: String) = metricKinds.firstOrNull { it.id == id } ?: metricKinds.first()

/** Kunlik nuqtadan ko'rsatkich qiymati (0 = o'lchov yo'q). */
private fun valueOf(kind: String, p: Health.DayPoint): Float = when (kind) {
    "activity" -> p.steps.toFloat()
    "heart" -> (p.hrAvg ?: 0).toFloat()
    "sleep" -> p.sleepMin.toFloat()
    "oxygen" -> (p.spo2Avg ?: 0).toFloat()
    "breath" -> (p.respAvg ?: 0).toFloat()
    else -> 0f
}

/** Ikki davr o'rtachasi farqi (%), ma'lumot yetarli bo'lmasa null. */
private fun trendPct(now: List<Float>, prev: List<Float>): Int? {
    val a = now.filter { it > 0 }; val b = prev.filter { it > 0 }
    if (a.size < 2 || b.size < 2) return null
    val pa = a.average(); val pb = b.average()
    if (pb <= 0) return null
    val pct = ((pa - pb) / pb * 100).roundToInt()
    return if (abs(pct) >= 3) pct else null
}

// ================================================================ Salomatlik tabi

@Composable
fun HealthScreen(vm: AppViewModel, openDetail: (String) -> Unit) {
    val week by vm.week.collectAsState()
    val s by vm.settings.collectAsState()
    val legacy by vm.legacyCount.collectAsState()
    var stats by remember { mutableStateOf<Pair<Long, Long?>?>(null) }
    var prev by remember { mutableStateOf<List<Health.DayPoint>>(emptyList()) }
    LaunchedEffect(week) { stats = vm.stats(); prev = vm.series(LocalDate.now().minusDays(7), 7) }
    HealthContent(week, prev, stats, s, openDetail, legacy = legacy)
}

@Composable
fun HealthContent(week: List<Health.DayPoint>, prev: List<Health.DayPoint>, stats: Pair<Long, Long?>?, s: AppSettings, openDetail: (String) -> Unit, legacy: Long = 0) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 132.dp), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        item { LargeTitle(stringResource(R.string.health_title), stringResource(R.string.health_subtitle)) }
        if (legacy > 0) item { LegacyNotice() }
        itemsIndexed(metricKinds, key = { _, k -> k.id }) { i, k -> HealthRow(k, week, prev, Modifier.staggered(i + 1)) { openDetail(k.id) } }
        item {
            stats?.let { (count, first) ->
                val n = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                val text = if (count == 0L) stringResource(R.string.health_no_data) else
                    pluralStringResource(R.plurals.health_records, n, Fmt.int(n)) +
                        (first?.let { stringResource(R.string.health_since, Fmt.date(java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(), true)) } ?: "") +
                        (if (s.storage == StorageMode.LOCAL) stringResource(R.string.health_local_only) else "")
                Text(text, style = OType.caption, color = O.c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(Space.m))
            }
        }
    }
}

@Composable
private fun HealthRow(k: MetricKind, week: List<Health.DayPoint>, prev: List<Health.DayPoint>, modifier: Modifier, onClick: () -> Unit) {
    val values = week.map { valueOf(k.id, it) }
    val present = values.filter { it > 0 }
    val avg = present.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    val trend = if (k.id == "pressure") null else trendPct(values, prev.map { valueOf(k.id, it) })
    OCard(modifier.sharedContainer(sharedKey(k.id, "health")), onClick = onClick, glow = k.tone.a, padding = Space.l, onClickLabel = stringResource(R.string.today_open_details)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(k.icon, k.tone, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(k.title), style = OType.label, color = k.tone.ink)
                when {
                    avg == null -> ValueText("—", style = OType.title)
                    k.id == "activity" -> AnimatedNumber(avg, Fmt::int, unit = pluralStringResource(R.plurals.unit_steps_per_day, avg), style = OType.title)
                    k.id == "sleep" -> ValueText(Fmt.duration(avg), style = OType.title)
                    k.id == "heart" -> AnimatedNumber(avg, { it.toString() }, unit = stringResource(R.string.unit_bpm), style = OType.title)
                    k.id == "oxygen" -> AnimatedNumber(avg, { it.toString() }, unit = "%", style = OType.title)
                    else -> AnimatedNumber(avg, { it.toString() }, unit = stringResource(R.string.unit_per_min), style = OType.title)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (k.id == "pressure") R.string.health_list else R.string.health_avg7), style = OType.caption, color = O.c.text2,
                        modifier = Modifier.weight(1f, fill = false))
                    if (trend != null) {
                        val cd = stringResource(if (trend > 0) R.string.trend_up_cd else R.string.trend_down_cd, abs(trend))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (trend > 0) R.string.trend_up else R.string.trend_down, abs(trend)), style = OType.label, color = k.tone.ink, maxLines = 1, softWrap = false,
                            modifier = Modifier.clip(CircleShape).background(k.tone.soft(0.14f)).padding(horizontal = 8.dp, vertical = 2.dp).semantics { contentDescription = cd })
                    }
                }
            }
            if (present.size >= 2) Sparkline(values, k.tone, Modifier.width(92.dp), height = 40.dp)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = O.c.text3)
        }
    }
}

// ================================================================ batafsil ekran

@Composable
fun DetailScreen(vm: AppViewModel, kindId: String, from: String, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val today by vm.today.collectAsState()  // yangi ma'lumot kelganda qayta yuklash uchun
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(anchor)
    val span = when (mode) { 0 -> 1L; 1 -> 7L; else -> 30L }
    var dayStats by remember { mutableStateOf<DayStats?>(null) }
    var series by remember { mutableStateOf<List<Health.DayPoint>>(emptyList()) }
    LaunchedEffect(kindId, mode, anchor, today) {
        if (mode == 0) dayStats = vm.day(date) else series = vm.series(date, span.toInt())
    }
    DetailContent(
        kindId, from, s, mode, { mode = it }, date,
        onPrev = { anchor = date.minusDays(span).toString() },
        onNext = { anchor = minOf(date.plusDays(span), LocalDate.now()).toString() },
        dayStats = dayStats, series = series, onBack = onBack,
    )
}

@Composable
fun DetailContent(
    kindId: String,
    from: String,
    s: AppSettings,
    mode: Int,
    onMode: (Int) -> Unit,
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    dayStats: DayStats?,
    series: List<Health.DayPoint>,
    onBack: () -> Unit,
) {
    val k = kindOf(kindId)
    val span = when (mode) { 0 -> 1L; 1 -> 7L; else -> 30L }
    val canNext = date.plusDays(1) <= LocalDate.now()
    // kartochka "shisha"si shu ekranga kengayadi (konteyner o'tishi)
    AuroraBackground(k.tone, Modifier.sharedContainer(sharedKey(kindId, from))) {
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(k.title), onBack)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                item { Segmented(listOf(stringResource(R.string.detail_day), stringResource(R.string.detail_week), stringResource(R.string.detail_month)), mode, onMode) }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircleIconButton(Icons.Rounded.ChevronLeft, onPrev, contentDescription = stringResource(R.string.cd_previous))
                        Text(
                            if (mode == 0) Fmt.dayTitle(date) else "${Fmt.dateShort(date.minusDays(span - 1))} – ${Fmt.dateShort(date)}",
                            style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f).semantics { heading() }, textAlign = TextAlign.Center,
                        )
                        // bugundan keyingi davr yo'q: tugma haqiqatan o'chiq (ekran o'qigich ham shunday o'qiydi)
                        CircleIconButton(Icons.Rounded.ChevronRight, onNext, tint = if (canNext) O.c.text else O.c.text3,
                            contentDescription = stringResource(R.string.cd_next), enabled = canNext)
                    }
                }
                item {
                    AnimatedContent(mode == 0, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "d") { isDay ->
                        if (isDay) DayView(k, dayStats, s) else PeriodView(k, series, s)
                    }
                }
                item {
                    OCard(padding = Space.l) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Info, Tones.device, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.detail_about), style = OType.bodyStrong, color = O.c.text, modifier = Modifier.semantics { heading() })
                        }
                        Spacer(Modifier.height(Space.s))
                        Text(stringResource(k.about), style = OType.caption, color = O.c.text2)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.common_not_medical), style = OType.caption, color = O.c.text2)
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(label: String, tone: Tone, value: @Composable () -> Unit) {
    Column {
        Overline(label, color = tone.ink)
        value()
    }
}

private val bigStyle get() = OType.hero.copy(fontSize = OType.hero.fontSize * 0.8f)

/** Statistika qatori: katta shriftda ham sig'adi (kerak bo'lsa keyingi qatorga o'tadi). */
@Composable
private fun StatsRow(vararg items: Pair<String, String>) {
    FlowRow(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        items.forEach { (label, value) ->
            Column(Modifier.widthIn(min = 72.dp)) {
                Text(label, style = OType.label, color = O.c.text2, maxLines = 2)
                Text(value, style = OType.headline, color = O.c.text, maxLines = 1)
            }
        }
    }
}

private fun dayX(ms: Long): Float {
    val t = java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return (t.hour * 60 + t.minute) / 1440f
}

@Composable
private fun DayView(k: MetricKind, d: DayStats?, s: AppSettings) {
    if (d == null) { SkeletonCard(260.dp); return }
    val bpm = stringResource(R.string.unit_bpm)
    val perMin = stringResource(R.string.unit_per_min)
    val mmhg = stringResource(R.string.unit_mmhg)
    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        OCard(glow = k.tone.a) {
            when (k.id) {
                "activity" -> {
                    val progress = Fmt.ratio(d.steps, s.goals.steps)
                    // Qadam yozuvi bo'lmasa "0" emas, "—": o'lchov yo'q, nol emas
                    val known = d.hasSteps
                    BigStat(stringResource(R.string.metric_steps), k.tone) {
                        if (known) AnimatedNumber(d.steps, Fmt::int, style = bigStyle) else Text("—", style = bigStyle, color = O.c.text)
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(progress, k.tone)
                    Text(stringResource(R.string.detail_goal_line, Fmt.int(s.goals.steps), if (known) Fmt.pct(progress) else "—"), style = OType.caption, color = O.c.text2, modifier = Modifier.padding(top = 6.dp))
                    val (dv, du) = Fmt.distance(d.distanceM, s.units)
                    StatsRow(stringResource(R.string.metric_distance) to (if (known) "$dv $du" else "—"),
                        stringResource(R.string.metric_calories) to (if (known) "${Fmt.int(d.kcal)} ${stringResource(R.string.unit_kcal)}" else "—"),
                        stringResource(R.string.detail_active_hours) to "${d.hourlySteps.count { it >= 250 }}")
                    Spacer(Modifier.height(18.dp))
                    Overline(stringResource(R.string.today_by_hour))
                    val labels = d.hourlySteps.mapIndexed { h, v -> "%02d:00 · %s %s".format(h, Fmt.int(v), pluralStringResource(R.plurals.unit_steps, v)) }
                    BarChart(d.hourlySteps.map { it.toFloat() }, (0..23).map { if (it % 6 == 0) "$it" else "" }, k.tone,
                        highlight = if (d.date == LocalDate.now()) java.time.LocalTime.now().hour else -1, height = 120.dp, valueText = { labels[it] })
                }
                "heart" -> {
                    BigStat(stringResource(R.string.detail_avg), k.tone) { ValueText(d.hrAvg?.toString() ?: "—", bpm, style = bigStyle) }
                    StatsRow(stringResource(R.string.detail_min) to (d.hrMin?.toString() ?: "—"), stringResource(R.string.detail_max) to (d.hrMax?.toString() ?: "—"),
                        stringResource(R.string.today_heart_sleep) to (d.restingHr?.toString() ?: "—"), stringResource(R.string.detail_readings) to "${d.hr.size}")
                    if (d.hr.size >= 2) {
                        Spacer(Modifier.height(Space.l))
                        val tips = d.hr.map { stringResource(R.string.detail_tooltip_hr, Fmt.time(it.start, s.clock24), it.value.toInt()) }
                        LineChart(d.hr.map { dayX(it.start) }, d.hr.map { it.value.toFloat() }, k.tone, height = 180.dp, valueText = { tips[it] })
                        TimeAxis()
                    } else if (d.hr.isEmpty()) NoDataText()
                }
                "sleep" -> {
                    val n = d.sleep
                    if (n == null) NoDataText(R.string.detail_sleep_none)
                    else {
                        val (v, u) = Fmt.durationShort(n.inBedMin)
                        BigStat(stringResource(R.string.detail_in_bed), k.tone) { ValueText(v, u, style = bigStyle) }
                        StatsRow(stringResource(R.string.detail_bed) to Fmt.time(n.bed, s.clock24), stringResource(R.string.detail_wake) to Fmt.time(n.wake, s.clock24),
                            stringResource(R.string.detail_asleep) to Fmt.duration(n.asleepMin))
                        Spacer(Modifier.height(18.dp))
                        val names = mapOf(1 to stringResource(R.string.stage_awake), 4 to stringResource(R.string.stage_light_long), 5 to stringResource(R.string.stage_deep_long), 6 to stringResource(R.string.stage_rem))
                        val fmt = stringResource(R.string.hypno_inspect)
                        Hypnogram(n.spans, n.bed, n.wake, height = 160.dp, inspectText = { sp ->
                            fmt.format(names[sp.stage] ?: "", Fmt.time(sp.start, s.clock24), Fmt.time(sp.end, s.clock24), Fmt.duration(((sp.end - sp.start) / 60_000).toInt()))
                        })
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(Fmt.time(n.bed, s.clock24), style = OType.label, color = O.c.text2, modifier = Modifier.weight(1f))
                            Text(Fmt.time(n.wake, s.clock24), style = OType.label, color = O.c.text2)
                        }
                        Spacer(Modifier.height(14.dp))
                        val total = (n.deepMin + n.lightMin + n.remMin + n.awakeMin).coerceAtLeast(1)
                        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                            LegendDot(Tones.deep, stringResource(R.string.stage_deep_long), stringResource(R.string.detail_stage_value, Fmt.duration(n.deepMin), n.deepMin * 100 / total))
                            LegendDot(Tones.light, stringResource(R.string.stage_light_long), stringResource(R.string.detail_stage_value, Fmt.duration(n.lightMin), n.lightMin * 100 / total))
                            if (n.remMin > 0) LegendDot(Tones.rem, stringResource(R.string.stage_rem), stringResource(R.string.detail_stage_value, Fmt.duration(n.remMin), n.remMin * 100 / total))
                            LegendDot(Tones.awake, stringResource(R.string.stage_awake), stringResource(R.string.detail_stage_value, Fmt.duration(n.awakeMin), n.awakeMin * 100 / total))
                        }
                        val gap = n.inBedMin - n.asleepMin - n.awakeMin
                        if (gap > 20) Text(stringResource(R.string.detail_sleep_gap, Fmt.duration(gap)), style = OType.caption, color = O.c.text2, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                "oxygen" -> {
                    val v = d.spo2
                    BigStat(stringResource(R.string.detail_avg), k.tone) { ValueText(v.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.toInt()?.toString() ?: "—", "%", style = bigStyle) }
                    StatsRow(stringResource(R.string.detail_min) to (v.minOfOrNull { it.value }?.toInt()?.let { "$it%" } ?: "—"),
                        stringResource(R.string.detail_max) to (v.maxOfOrNull { it.value }?.toInt()?.let { "$it%" } ?: "—"), stringResource(R.string.detail_readings) to "${v.size}")
                    if (v.size >= 2) {
                        Spacer(Modifier.height(Space.l))
                        val tips = v.map { stringResource(R.string.detail_tooltip_spo2, Fmt.time(it.start, s.clock24), it.value.toInt()) }
                        LineChart(v.map { dayX(it.start) }, v.map { it.value.toFloat() }, k.tone, minY = 85f, maxY = 100f, height = 160.dp, valueText = { tips[it] })
                        TimeAxis()
                    } else if (v.isEmpty()) NoDataText()
                }
                else -> {
                    val isBp = k.id == "pressure"
                    val v = if (isBp) d.bp else d.resp
                    val last = v.lastOrNull()
                    BigStat(stringResource(R.string.detail_last), k.tone) {
                        ValueText(last?.let { if (isBp) "${it.value.toInt()}/${it.extra?.toInt() ?: "—"}" else "${it.value.toInt()}" } ?: "—", if (isBp) mmhg else perMin, style = bigStyle)
                    }
                    Spacer(Modifier.height(14.dp))
                    if (v.isEmpty()) Text(stringResource(R.string.detail_no_data_day), style = OType.caption, color = O.c.text2)
                    v.reversed().take(12).forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(Fmt.time(it.start, s.clock24), style = OType.body, color = O.c.text2, modifier = Modifier.weight(1f))
                            Text(if (isBp) "${it.value.toInt()}/${it.extra?.toInt() ?: "—"} $mmhg" else "${it.value.toInt()} $perMin", style = OType.bodyStrong, color = O.c.text)
                        }
                    }
                }
            }
        }
        if (k.id == "heart" && d.hr.isNotEmpty()) HrZonesCard(d, s)
    }
}

/** Yurak zonalari (taxminiy, 220 − yosh). Tug'ilgan sana bo'lmasa — qanday yoqish haqida izoh. */
@Composable
private fun HrZonesCard(d: DayStats, s: AppSettings) {
    val maxHr = HrZones.maxHr(s.profile.age)
    OCard(glow = Tones.heart.a) {
        SectionHeader(stringResource(R.string.hr_zones_title))
        if (maxHr == null) { Text(stringResource(R.string.hr_zones_need_age), style = OType.caption, color = O.c.text2); return@OCard }
        val minutes = remember(d.hr, maxHr) { HrZones.minutesInZones(d.hr, maxHr) }
        val labels = listOf(R.string.hr_zone_0, R.string.hr_zone_1, R.string.hr_zone_2, R.string.hr_zone_3, R.string.hr_zone_4, R.string.hr_zone_5).map { stringResource(it) }
        Spacer(Modifier.height(Space.s))
        HrZonesBar(minutes, labels, { Fmt.duration(it) })
        Spacer(Modifier.height(Space.s))
        Text(stringResource(R.string.hr_zones_note), style = OType.caption, color = O.c.text2)
    }
}

@Composable
private fun NoDataText(@StringRes text: Int = R.string.detail_no_data_day) =
    Text(stringResource(text), style = OType.body, color = O.c.text2, modifier = Modifier.padding(top = Space.m))

@Composable
private fun TimeAxis() {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, end = 34.dp)) {
        listOf("00", "06", "12", "18", "24").forEachIndexed { i, t ->
            Text(t, style = OType.label, color = O.c.text2, maxLines = 1, softWrap = false,
                modifier = Modifier.weight(if (i == 4) 0.0001f else 1f, fill = i != 4).wrapContentWidth(Alignment.Start, unbounded = true))
        }
    }
}

@Composable
private fun PeriodView(k: MetricKind, pts: List<Health.DayPoint>, s: AppSettings) {
    if (pts.isEmpty()) { SkeletonCard(260.dp); return }
    val values = pts.map { valueOf(k.id, it) }
    val present = values.filter { it > 0 }
    val avg = present.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val labels = if (pts.size <= 7) pts.map { Fmt.weekdayShort(it.date) } else pts.mapIndexed { i, p -> if (i % 5 == 0) "${p.date.dayOfMonth}" else "" }
    val goal = when (k.id) { "activity" -> s.goals.steps.toFloat(); "sleep" -> s.goals.sleepMin.toFloat(); else -> null }?.takeIf { it > 0f }
    fun show(v: Float?) = when { v == null -> "—"; k.id == "activity" -> Fmt.int(v.toInt()); k.id == "sleep" -> Fmt.duration(v.toInt()); else -> v.toInt().toString() }
    val unit = when (k.id) {
        "heart" -> stringResource(R.string.unit_bpm); "oxygen" -> "%"; "breath" -> stringResource(R.string.unit_per_min)
        "activity" -> pluralStringResource(R.plurals.unit_steps, avg?.toInt() ?: 0); else -> ""
    }
    val atGoal = if (goal != null) values.count { it >= goal } else 0
    val tips = pts.mapIndexed { i, p -> "${Fmt.dateShort(p.date)} · ${show(values[i].takeIf { it > 0 })} $unit".trim() }

    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        OCard(glow = k.tone.a) {
            if (k.id == "pressure") { Text(stringResource(R.string.detail_pressure_day_only), style = OType.body, color = O.c.text2); return@OCard }
            BigStat(stringResource(R.string.detail_avg), k.tone) { ValueText(show(avg), unit, style = bigStyle) }
            StatsRow(
                stringResource(R.string.detail_highest) to show(present.maxOrNull()),
                stringResource(R.string.detail_lowest) to show(present.minOrNull()),
                (if (goal != null) stringResource(R.string.detail_at_goal) else stringResource(R.string.detail_days)) to
                    (if (goal != null) pluralStringResource(R.plurals.detail_n_days, atGoal, atGoal) else "${present.size}"),
            )
            Spacer(Modifier.height(18.dp))
            if (present.isEmpty()) Text(stringResource(R.string.detail_no_period_data), style = OType.caption, color = O.c.text2)
            BarChart(values, labels, k.tone, goal = goal, highlight = values.lastIndex, height = 170.dp, valueText = { tips[it] })
            Text(stringResource(R.string.detail_interact_hint), style = OType.caption, color = O.c.text3, modifier = Modifier.padding(top = Space.s))
        }
        if (k.id == "activity") {
            OCard {
                SectionHeader(stringResource(R.string.detail_total))
                val (dv, du) = Fmt.distance(pts.sumOf { it.distanceM }, s.units)
                StatsRow(stringResource(R.string.metric_steps) to Fmt.int(pts.sumOf { it.steps }), stringResource(R.string.metric_distance) to "$dv $du",
                    stringResource(R.string.metric_calories) to Fmt.int(pts.sumOf { it.kcal }))
            }
        }
    }
}

// ================================================================ previewlar

@OrvellPreviews
@Composable
private fun HealthPreview() = PreviewSurface(Tones.heart) {
    HealthContent(PreviewData.week, PreviewData.series(14).take(7), 12_480L to PreviewData.today.minusDays(40).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), PreviewData.settings, {})
}

@OrvellPreviews
@Composable
private fun DetailSleepPreview() = PreviewSurface {
    DetailContent("sleep", "today", PreviewData.settings, 0, {}, PreviewData.today, {}, {}, PreviewData.day, emptyList()) {}
}

@OrvellPreviews
@Composable
private fun DetailHeartPreview() = PreviewSurface {
    DetailContent("heart", "today", PreviewData.settings, 0, {}, PreviewData.today, {}, {}, PreviewData.day, emptyList()) {}
}

@OrvellPreviews
@Composable
private fun DetailActivityWeekPreview() = PreviewSurface {
    DetailContent("activity", "health", PreviewData.settings, 1, {}, PreviewData.today, {}, {}, null, PreviewData.week) {}
}
