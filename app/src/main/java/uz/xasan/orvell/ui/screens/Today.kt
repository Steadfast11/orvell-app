package uz.xasan.orvell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.xasan.orvell.R
import uz.xasan.orvell.data.AppSettings
import uz.xasan.orvell.data.DayStats
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.data.Profile
import uz.xasan.orvell.data.Units
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.AnimatedNumber
import uz.xasan.orvell.ui.components.Art
import uz.xasan.orvell.ui.components.BarChart
import uz.xasan.orvell.ui.components.CoachMark
import uz.xasan.orvell.ui.components.EmptyState
import uz.xasan.orvell.ui.components.IconBadge
import uz.xasan.orvell.ui.components.Illustration
import uz.xasan.orvell.ui.components.LegacyNotice
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.OrbitRings
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.Ring
import uz.xasan.orvell.ui.components.SecondaryButton
import uz.xasan.orvell.ui.components.SectionHeader
import uz.xasan.orvell.ui.components.SkeletonCard
import uz.xasan.orvell.ui.components.Sparkline
import uz.xasan.orvell.ui.components.StageBar
import uz.xasan.orvell.ui.components.ValueText
import uz.xasan.orvell.ui.components.rememberHint
import uz.xasan.orvell.ui.components.staggered
import uz.xasan.orvell.ui.logic.Insight
import uz.xasan.orvell.ui.logic.Insights
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
import uz.xasan.orvell.watch.Conn
import uz.xasan.orvell.watch.WatchState
import java.time.LocalDate
import java.time.LocalTime

val avatarTones = listOf(Tones.brand, Tones.sleep, Tones.heart, Tones.oxygen, Tones.pressure, Tones.activity)

/** Profil belgisi: bosh harflar, atrofida gradient orbita va kichik sayyora. */
@Composable
fun Avatar(profile: Profile, size: Dp = 44.dp, onClick: (() -> Unit)? = null, contentDescription: String? = null) {
    val tone = avatarTones[profile.avatar.mod(avatarTones.size)]
    val c = O.c
    Box(
        Modifier.size(size)
            .then(if (onClick != null) Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick) else Modifier)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(tone.brush).padding(size * 0.06f).clip(CircleShape).background(c.raised), contentAlignment = Alignment.Center) {
            Text(profile.initials, style = OType.headline.copy(fontSize = (size.value * 0.36f).sp), color = c.text, modifier = Modifier.clearAndSetSemantics { })
        }
        Box(Modifier.align(Alignment.TopEnd).size(size * 0.22f).clip(CircleShape).background(Color.White).border(size * 0.04f, tone.a, CircleShape))
    }
}

/** Soat holati belgisi: rangli nuqta + matn (rang yagona belgi emas), batareya va oxirgi sinx vaqti. */
@Composable
fun WatchChip(w: WatchState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = O.c
    val (color, text) = when (w.conn) {
        Conn.CONNECTED -> c.success to (if (w.battery >= 0) "${w.battery}%" else stringResource(R.string.watch_chip_connected))
        Conn.CONNECTING -> c.warning to stringResource(R.string.watch_chip_connecting)
        Conn.CONFIRM_ON_WATCH -> c.warning to stringResource(R.string.watch_chip_confirm)
        Conn.BLUETOOTH_OFF -> c.danger to stringResource(R.string.watch_chip_bt_off)
        Conn.DISCONNECTED -> c.text3 to stringResource(R.string.watch_chip_disconnected)
        Conn.UNPAIRED -> c.text3 to stringResource(R.string.watch_chip_unpaired)
    }
    Row(
        modifier.heightIn(min = 40.dp).clip(CircleShape).background(c.surfaceHi).border(1.dp, Brush.verticalGradient(listOf(c.highlight, c.stroke)), CircleShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (w.conn == Conn.BLUETOOTH_OFF) Icons.Rounded.BluetoothDisabled else Icons.Rounded.Watch, null, tint = c.text2, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = OType.label, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // oxirgi muvaffaqiyatli tarix sinxroni (lastSync'ga hech kim yozmaydi)
        if (w.conn == Conn.CONNECTED && w.historyAt > 0) Text(" · ${Fmt.ago(w.historyAt)}", style = OType.label, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ================================================================ ekran

@Composable
fun TodayScreen(vm: AppViewModel, openDetail: (String) -> Unit, openDevice: () -> Unit, openProfile: () -> Unit) {
    val s by vm.settings.collectAsState()
    val w by vm.watch.collectAsState()
    val day by vm.today.collectAsState()
    val week by vm.week.collectAsState()
    val legacy by vm.legacyToday.collectAsState()
    val insight = remember(day, week, s.goals.steps) { Insights.pick(day, week.dropLast(1), s.goals.steps, LocalTime.now().hour) }
    // Qurilma ekranidagi bilan bir xil sinxron amali
    val sync = rememberSyncAction(vm)
    TodayContent(
        settings = s, watch = w, day = day, week = week, insight = insight,
        onSync = sync, openDetail = openDetail, openDevice = openDevice, openProfile = openProfile, legacy = legacy,
    )
}

/** Holatsiz "Bugun" (preview va testlar uchun): kun hikoyasi yuqoridan pastga. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayContent(
    settings: AppSettings,
    watch: WatchState,
    day: DayStats?,
    week: List<Health.DayPoint>,
    insight: Insight?,
    onSync: () -> Unit,
    openDetail: (String) -> Unit,
    openDevice: () -> Unit,
    openProfile: () -> Unit,
    legacy: Long = 0,
) {
    val s = settings
    val today = LocalDate.now()
    val list = rememberLazyListState()
    val paired = watch.conn != Conn.UNPAIRED
    val cardHint = rememberHint("today_cards", enabled = paired)
    val empty = day != null && day.isEmpty() && week.all { it.steps == 0 && it.sleepMin == 0 && it.hrAvg == null }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            item { Header(s, watch, today, openDevice, openProfile, Modifier.staggered(0)) }
            if (legacy > 0) item { LegacyNotice(Modifier.staggered(1)) }
            if (!paired) item { PairBanner(openDevice, Modifier.staggered(1)) }
            else if (empty) item { OCard(Modifier.staggered(1)) { EmptyState(Icons.Rounded.Bolt, stringResource(R.string.today_empty_title), stringResource(R.string.today_empty_body), Tones.brand, art = { Illustration(Art.NO_DATA, size = 150.dp) }) } }
            if (insight != null) item { InsightCard(insight, Modifier.staggered(1)) }
            if (day == null) {
                item { SkeletonCard(260.dp) }
                item { SkeletonCard(150.dp) }
                item { SkeletonCard(180.dp) }
            } else {
                item { HeroActivity(day, s, list, { cardHint.dismiss(); openDetail("activity") }, Modifier.staggered(2)) }
                item { CoachMark(cardHint, stringResource(R.string.hint_tap_card)) }
                item { QuickActions(watch, onSync, Modifier.staggered(3)) }
                item { HeartCard(day, s.clock24, { openDetail("heart") }, Modifier.staggered(4)) }
                item { SleepCard(day, s.goals.sleepMin, s.clock24, { openDetail("sleep") }, Modifier.staggered(5)) }
                item {
                    Row(Modifier.staggered(6), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        val spoList = day.spo2
                        SmallMetric(Modifier.weight(1f), "oxygen", Tones.oxygen, Icons.Rounded.WaterDrop, stringResource(R.string.metric_oxygen_short),
                            spoList.lastOrNull()?.value?.toInt()?.toString() ?: "—", "%",
                            if (spoList.isNotEmpty()) pluralStringResource(R.plurals.today_spo2_avg, spoList.size, spoList.map { x -> x.value }.average().toInt(), spoList.size)
                            else stringResource(R.string.today_no_reading)) { openDetail("oxygen") }
                        val bp = day.bp.lastOrNull()
                        SmallMetric(Modifier.weight(1f), "pressure", Tones.pressure, Icons.Rounded.MonitorHeart, stringResource(R.string.metric_pressure_short),
                            bp?.let { "${it.value.toInt()}/${it.extra?.toInt() ?: "—"}" } ?: "—", stringResource(R.string.unit_mmhg),
                            bp?.let { stringResource(R.string.today_at_time, Fmt.time(it.start, s.clock24)) } ?: stringResource(R.string.today_no_reading)) { openDetail("pressure") }
                    }
                }
                item {
                    Row(Modifier.staggered(7), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        val r = day.resp.lastOrNull()
                        SmallMetric(Modifier.weight(1f), "breath", Tones.breath, Icons.Rounded.Air, stringResource(R.string.metric_breath_short), r?.value?.toInt()?.toString() ?: "—",
                            stringResource(R.string.unit_per_min), r?.let { stringResource(R.string.today_at_time, Fmt.time(it.start, s.clock24)) } ?: stringResource(R.string.today_no_reading)) { openDetail("breath") }
                        // Qadam yozuvi yo'q bo'lsa o'rtacha "0" emas, "—" (o'lchov yo'q, nol emas)
                        val avg = week.takeIf { w -> w.any { it.steps > 0 } }?.map { it.steps }?.average()?.toInt()
                        val atGoal = week.count { it.steps >= s.goals.steps }
                        SmallMetric(Modifier.weight(1f), null, Tones.activity, Icons.Rounded.Bolt, stringResource(R.string.today_week_avg), avg?.let { Fmt.int(it) } ?: "—",
                            pluralStringResource(R.plurals.unit_steps, avg ?: 0), pluralStringResource(R.plurals.today_days_at_goal, atGoal, atGoal)) { openDetail("activity") }
                    }
                }
                if (week.isNotEmpty()) item { WeekCard(week, s.goals.steps, { openDetail("activity") }, Modifier.staggered(8)) }
            }
            item {
                Text(stringResource(R.string.common_disclaimer), style = OType.caption, color = O.c.text3, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Space.l))
            }
        }
    }
}

/** Kun bo'yi hech qanday o'lchov yo'q (birinchi ochilish yoki soat hali hech narsa bermagan). */
private fun DayStats.isEmpty() = steps == 0 && distanceM == 0 && kcal == 0 && hr.isEmpty() && spo2.isEmpty() && bp.isEmpty() && resp.isEmpty() && sleep == null

@Composable
private fun Header(s: AppSettings, w: WatchState, today: LocalDate, openDevice: () -> Unit, openProfile: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().statusBarsPadding().padding(top = Space.m)) {
        Row(Modifier.padding(start = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.greeting() + ",", style = OType.body, color = O.c.text2)
                Text(s.profile.name.ifBlank { stringResource(R.string.today_friend) }, style = OType.display, color = O.c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Avatar(s.profile, 52.dp, openProfile, stringResource(R.string.tab_profile))
        }
        // uzun (ruscha) holat matni sanani siqib qo'ymasin: kerak bo'lsa ikki qatorga o'tadi
        FlowRow(Modifier.fillMaxWidth().padding(top = Space.m, start = Space.xs), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text(Fmt.fullDate(today, today), style = OType.caption, color = O.c.text2, modifier = Modifier.align(Alignment.CenterVertically).padding(end = Space.s))
            WatchChip(w, openDevice)
        }
    }
}

/** Kun xulosasi: bitta halol jumla (faqat saqlangan o'lchovlardan). */
@Composable
private fun insightText(i: Insight): String = when (i) {
    is Insight.SleepVsAverage -> stringResource(if (i.diffMin > 0) R.string.insight_sleep_more else R.string.insight_sleep_less, Fmt.duration(kotlin.math.abs(i.diffMin)))
    is Insight.GoalReached -> pluralStringResource(R.plurals.insight_goal, i.steps, Fmt.int(i.steps))
    is Insight.GoalStreak -> pluralStringResource(R.plurals.insight_streak, i.days, i.days)
    is Insight.StepsVsAverage -> kotlin.math.abs(i.diff).let { pluralStringResource(if (i.diff > 0) R.plurals.insight_steps_more else R.plurals.insight_steps_less, it, Fmt.int(it)) }
    is Insight.RestingHrVsAverage -> stringResource(if (i.diffBpm > 0) R.string.insight_hr_higher else R.string.insight_hr_lower, kotlin.math.abs(i.diffBpm))
    is Insight.StepsToGoal -> pluralStringResource(R.plurals.insight_to_goal, i.remaining, Fmt.int(i.remaining))
}

private fun insightTone(i: Insight): Tone = when (i) {
    is Insight.SleepVsAverage -> Tones.sleep
    is Insight.RestingHrVsAverage -> Tones.heart
    else -> Tones.activity
}

@Composable
private fun InsightCard(i: Insight, modifier: Modifier = Modifier) {
    val tone = insightTone(i)
    OCard(modifier, glow = tone.a, padding = Space.l) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.AutoAwesome, tone, 36.dp)
            Spacer(Modifier.width(Space.m))
            Text(insightText(i), style = OType.bodyStrong, color = O.c.text, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PairBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OCard(modifier, onClick = onClick, glow = Tones.brand.a) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Illustration(Art.NO_WATCH, size = 84.dp)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_pair_title), style = OType.headline, color = O.c.text)
                Text(stringResource(R.string.today_pair_body), style = OType.caption, color = O.c.text2)
            }
            Icon(Icons.Rounded.Bluetooth, null, tint = Tones.brand.ink)
        }
    }
}

@Composable
private fun HeroActivity(d: DayStats, s: AppSettings, list: LazyListState, onClick: () -> Unit, modifier: Modifier) {
    val steps = d.steps
    val (dv, du) = Fmt.distance(d.distanceM, s.units)
    val goalSteps = s.goals.steps
    // Qadam yozuvi bo'lmasa qiymat "0" emas, "—" (o'lchov yo'q, nol emas)
    val known = d.hasSteps
    // Telefon qadamida masofa va kkal o'lchanmagan: ular "—" bo'ladi
    val measured = known && !d.stepsFromPhone
    val cd = stringResource(R.string.rings_cd, if (known) Fmt.int(steps) else "—", Fmt.int(goalSteps), if (measured) "$dv $du" else "—",
        if (measured) "${Fmt.int(d.kcal)} ${stringResource(R.string.unit_kcal)}" else "—")
    OCard(modifier.sharedContainer(sharedKey("activity", "today")), onClick = onClick, glow = Tones.activity.a, padding = 18.dp, onClickLabel = stringResource(R.string.today_open_details)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrbitRings(
                listOf(Ring(Fmt.ratio(steps, goalSteps), Tones.activity), Ring(Fmt.ratio(d.distanceM, s.goals.distanceM), Tones.distance), Ring(Fmt.ratio(d.kcal, s.goals.kcal), Tones.calories)),
                Modifier.size(176.dp).graphicsLayer {
                    // parallaks: aylantirilganda halqalar sekinroq siljiydi va so'nadi
                    val off = if (list.firstVisibleItemIndex <= 1) list.firstVisibleItemScrollOffset.toFloat() else 400f
                    translationY = off * 0.25f
                    alpha = (1f - off / 700f).coerceIn(0.3f, 1f)
                },
                stroke = 15.dp, gap = 6.dp, contentDescription = cd,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
                    if (known) AnimatedNumber(steps, Fmt::int, style = OType.number.copy(fontSize = 26.sp))
                    else Text("—", style = OType.number.copy(fontSize = 26.sp), color = O.c.text2)
                    Text(pluralStringResource(R.plurals.unit_steps, steps), style = OType.label, color = O.c.text2)
                }
            }
            Spacer(Modifier.width(Space.l))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.clearAndSetSemantics { }) {
                Legend(Tones.activity, stringResource(R.string.metric_steps)) {
                    if (known) AnimatedNumber(steps, Fmt::int, unit = stringResource(R.string.today_of_goal, Fmt.int(goalSteps)), style = OType.headline)
                    else Text("—", style = OType.headline, color = O.c.text)
                }
                Legend(Tones.distance, stringResource(R.string.metric_distance)) {
                    if (measured) ValueText(dv, du, style = OType.headline) else Text("—", style = OType.headline, color = O.c.text)
                }
                Legend(Tones.calories, stringResource(R.string.metric_calories)) {
                    if (measured) AnimatedNumber(d.kcal, Fmt::int, unit = stringResource(R.string.unit_kcal), style = OType.headline)
                    else Text("—", style = OType.headline, color = O.c.text)
                }
            }
        }
        if (d.stepsFromPhone) {
            Spacer(Modifier.height(Space.s))
            Text(stringResource(R.string.today_steps_phone), style = OType.caption, color = O.c.text2)
        }
        if (d.hourlySteps.any { it > 0 }) {
            Spacer(Modifier.height(Space.l))
            Overline(stringResource(R.string.today_by_hour))
            val now = LocalTime.now().hour
            BarChart(
                d.hourlySteps.map { it.toFloat() }, (0..23).map { if (it % 6 == 0) "$it" else "" }, Tones.activity,
                highlight = now, height = 64.dp,
                valueText = { i -> "%02d:00".format(i) + " · " + Fmt.int(d.hourlySteps[i]) },
            )
        }
    }
}

@Composable
private fun Legend(tone: Tone, label: String, value: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 34.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(tone.a, tone.b))))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = OType.label, color = O.c.text2, maxLines = 1)
            value()
        }
    }
}

/** Tez amal: soat tarixini sinxronlash. Sinxron paytida "Sinxron qilinmoqda…"; soat ulanmagan bo'lsa bosilganda qisqa xabar chiqadi. */
@Composable
private fun QuickActions(watch: WatchState, onSync: () -> Unit, modifier: Modifier) {
    SecondaryButton(
        stringResource(if (watch.syncing) R.string.device_syncing else R.string.device_sync), onSync, modifier.fillMaxWidth(),
        enabled = !watch.syncing, icon = Icons.Rounded.Sync,
    )
}

@Composable
private fun HeartCard(d: DayStats, clock24: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val hr = d.hr
    OCard(modifier.sharedContainer(sharedKey("heart", "today")), onClick = onClick, glow = Tones.heart.a, onClickLabel = stringResource(R.string.today_open_details)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Favorite, Tones.heart, 36.dp)
            Spacer(Modifier.width(Space.m))
            Text(stringResource(R.string.today_heart_title), style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f))
            hr.lastOrNull()?.let { Text(Fmt.ago(it.start), style = OType.label, color = O.c.text2) }
        }
        Spacer(Modifier.height(Space.m))
        Row(verticalAlignment = Alignment.Bottom) {
            val lastBpm = hr.lastOrNull()?.value?.toInt()
            if (lastBpm != null) AnimatedNumber(lastBpm, { it.toString() }, unit = stringResource(R.string.unit_bpm), style = OType.display)
            else ValueText("—", stringResource(R.string.unit_bpm), style = OType.display)
            Spacer(Modifier.weight(1f))
            if (d.restingHr != null) Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.today_heart_sleep), style = OType.label, color = O.c.text2)
                ValueText("${d.restingHr}", stringResource(R.string.unit_bpm), style = OType.headline)
            }
        }
        if (hr.size >= 2) {
            Spacer(Modifier.height(10.dp))
            Sparkline(hr.map { it.value.toFloat() }, Tones.heart, height = 64.dp)
            Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatChip(stringResource(R.string.today_min_value, d.hrMin?.toString() ?: "—"))
                StatChip(stringResource(R.string.today_avg_value, d.hrAvg?.toString() ?: "—"))
                StatChip(stringResource(R.string.today_max_value, d.hrMax?.toString() ?: "—"))
            }
        } else Text(stringResource(R.string.today_heart_none), style = OType.caption, color = O.c.text2, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun StatChip(text: String) {
    val c = O.c
    Text(text, style = OType.label, color = c.text2, modifier = Modifier.clip(CircleShape).background(c.track).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun SleepCard(d: DayStats, goalMin: Int, clock24: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val n = d.sleep
    OCard(modifier.sharedContainer(sharedKey("sleep", "today")), onClick = onClick, glow = Tones.sleep.a, onClickLabel = stringResource(R.string.today_open_details)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Bedtime, Tones.sleep, 36.dp)
            Spacer(Modifier.width(Space.m))
            Text(stringResource(R.string.today_sleep_title), style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f))
            if (n != null) Text("${Fmt.time(n.bed, clock24)} → ${Fmt.time(n.wake, clock24)}", style = OType.label, color = O.c.text2)
        }
        Spacer(Modifier.height(Space.m))
        if (n == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Illustration(Art.NO_SLEEP, size = 72.dp)
                Spacer(Modifier.width(Space.m))
                Column {
                    Text(stringResource(R.string.today_sleep_none), style = OType.bodyStrong, color = O.c.text)
                    Text(stringResource(R.string.today_sleep_none_hint), style = OType.caption, color = O.c.text2)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                val (v, u) = Fmt.durationShort(n.inBedMin)
                ValueText(v, u, style = OType.display)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.today_sleep_goal, Fmt.duration(goalMin)), style = OType.caption, color = O.c.text2)
            }
            Spacer(Modifier.height(Space.m))
            StageBar(listOf(Tones.deep to n.deepMin.toFloat(), Tones.light to n.lightMin.toFloat(), Tones.rem to n.remMin.toFloat(), Tones.awake to n.awakeMin.toFloat()))
            Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StageText(Tones.deep, stringResource(R.string.stage_deep), n.deepMin)
                StageText(Tones.light, stringResource(R.string.stage_light), n.lightMin)
                if (n.remMin > 0) StageText(Tones.rem, stringResource(R.string.stage_rem), n.remMin)
                StageText(Tones.awake, stringResource(R.string.stage_awake), n.awakeMin)
            }
        }
    }
}

@Composable
private fun StageText(color: Color, label: String, min: Int) {
    Row(Modifier.padding(end = Space.s), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.today_stage_value, label, Fmt.duration(min)), style = OType.label, color = O.c.text2)
    }
}

@Composable
private fun SmallMetric(modifier: Modifier, kind: String?, tone: Tone, icon: ImageVector, title: String, value: String, unit: String, sub: String, onClick: () -> Unit) {
    OCard(modifier.then(if (kind != null) Modifier.sharedContainer(sharedKey(kind, "today")) else Modifier), onClick = onClick, glow = tone.a, padding = Space.l, radius = 24.dp,
        onClickLabel = stringResource(R.string.today_open_details)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tone, 30.dp)
            Spacer(Modifier.width(Space.s))
            Text(title, style = OType.label, color = O.c.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        ValueText(value, unit, style = OType.number)
        Spacer(Modifier.height(Space.xs))
        Text(sub, style = OType.caption, color = O.c.text2, maxLines = 2)
    }
}

@Composable
private fun WeekCard(week: List<Health.DayPoint>, goal: Int, onClick: () -> Unit, modifier: Modifier) {
    OCard(modifier, onClick = onClick, onClickLabel = stringResource(R.string.today_open_details)) {
        SectionHeader(stringResource(R.string.today_this_week))
        // tooltip matni: "Du · 6 240 qadam" (ko'plik shakli har kun uchun to'g'ri)
        val labels = week.map { "${Fmt.weekdayShort(it.date)} · ${Fmt.int(it.steps)} ${pluralStringResource(R.plurals.unit_steps, it.steps)}" }
        BarChart(week.map { it.steps.toFloat() }, week.map { Fmt.weekdayShort(it.date) }, Tones.activity, goal = goal.toFloat(),
            valueText = { i -> labels[i] }, description = stringResource(R.string.today_this_week))
    }
}

// ================================================================ previewlar

@OrvellPreviews
@Composable
private fun TodayPreview() = PreviewSurface(Tones.brand) {
    TodayContent(
        PreviewData.settings, PreviewData.watch, PreviewData.day, PreviewData.week,
        Insight.SleepVsAverage(40), {}, {}, {}, {},
    )
}

@OrvellPreviews
@Composable
private fun TodayEmptyPreview() = PreviewSurface(Tones.brand) {
    TodayContent(PreviewData.settings.copy(watch = PreviewData.settings.watch.copy(address = null)), WatchState(), PreviewData.emptyDay, emptyList(), null, {}, {}, {}, {})
}

@OrvellPreviews
@Composable
private fun TodayLoadingPreview() = PreviewSurface(Tones.brand) {
    TodayContent(PreviewData.settings, PreviewData.watch, null, emptyList(), null, {}, {}, {}, {})
}
