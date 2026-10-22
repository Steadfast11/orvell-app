package uz.xasan.orvell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.R
import uz.xasan.orvell.data.AppSettings
import uz.xasan.orvell.data.Goals
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.data.Profile
import uz.xasan.orvell.data.Sex
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.AnimatedNumber
import uz.xasan.orvell.ui.components.CircleIconButton
import uz.xasan.orvell.ui.components.LargeTitle
import uz.xasan.orvell.ui.components.LegacyNotice
import uz.xasan.orvell.ui.components.NumberDialog
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.OField
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.Pill
import uz.xasan.orvell.ui.components.PrimaryButton
import uz.xasan.orvell.ui.components.Segmented
import uz.xasan.orvell.ui.components.SettingRow
import uz.xasan.orvell.ui.components.SettingsGroup
import uz.xasan.orvell.ui.components.SkeletonCard
import uz.xasan.orvell.ui.components.TopBar
import uz.xasan.orvell.ui.components.ValueText
import uz.xasan.orvell.ui.components.rememberGrow
import uz.xasan.orvell.ui.components.rememberHaptics
import uz.xasan.orvell.ui.components.staggered
import uz.xasan.orvell.ui.logic.Achievement
import uz.xasan.orvell.ui.logic.Achievements
import uz.xasan.orvell.ui.logic.Streaks
import uz.xasan.orvell.ui.preview.ComponentPreviews
import uz.xasan.orvell.ui.preview.OrvellPreviews
import uz.xasan.orvell.ui.preview.PreviewData
import uz.xasan.orvell.ui.preview.PreviewSurface
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Space
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private const val STREAK_DOTS = 14

@Composable
fun ProfileScreen(vm: AppViewModel, open: (String) -> Unit) {
    val s by vm.settings.collectAsState()
    val legacy by vm.legacyCount.collectAsState()
    var series by remember { mutableStateOf<List<Health.DayPoint>?>(null) }
    var since by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { series = vm.series(LocalDate.now(), 90); since = vm.stats().second }
    ProfileContent(s, series, since, LocalDate.now(), open, onGoals = { g -> vm.update { it.copy(goals = g) } }, legacy = legacy)
}

/** Profil sahifasi uchun hisoblangan qiymatlar (90 kunlik qatordan, eskidan yangiga). */
private class ProfileStats(series: List<Health.DayPoint>, goals: Goals) {
    private val steps = series.map { it.steps }
    val total30 = steps.takeLast(30).sum()
    val record30 = steps.takeLast(30).maxOrNull() ?: 0
    val streak = Streaks.current(steps, goals.steps)
    val best = Streaks.best(steps, goals.steps)
    val last = steps.takeLast(STREAK_DOTS).map { goals.steps > 0 && it >= goals.steps }
    val lastMet = last.count { it }
    val achievements = Achievements.compute(
        steps, series.map { it.sleepMin }, goals.steps, goals.sleepMin,
        daysWithData = series.count { it.steps > 0 || it.sleepMin > 0 },
    )
    val earned = achievements.count { it.earned }
}

@Composable
private fun ProfileContent(
    s: AppSettings,
    series: List<Health.DayPoint>?,
    since: Long?,
    today: LocalDate,
    open: (String) -> Unit,
    onGoals: (Goals) -> Unit,
    legacy: Long = 0,
) {
    val p = s.profile
    val stats = remember(series, s.goals) { series?.let { ProfileStats(it, s.goals) } }
    var goalDialog by remember { mutableStateOf<String?>(null) }
    // kalendar kunlari (yoz/qish vaqti o'tishida 23/25 soatlik kunlar ham to'g'ri sanaladi)
    val days = since?.let { ChronoUnit.DAYS.between(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate(), today).toInt() + 1 } ?: 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        LargeTitle(stringResource(R.string.profile_title)) {
            CircleIconButton(Icons.Rounded.Settings, { open("settings") }, contentDescription = stringResource(R.string.cd_settings))
        }
        if (legacy > 0) LegacyNotice()
        IdentityCard(s, days, stats, Modifier.staggered(0)) { open("profile/edit") }
        if (stats == null) {
            SkeletonCard(150.dp)
            SkeletonCard(240.dp)
        } else {
            StreakCard(stats, Modifier.staggered(1))
            AchievementsSection(stats, Modifier.staggered(2))
        }
        Box(Modifier.staggered(3)) {
            SettingsGroup(stringResource(R.string.profile_goals)) {
                item { SettingRow(Icons.Rounded.Bolt, Tones.activity, stringResource(R.string.profile_goal_steps), value = Fmt.int(s.goals.steps), onClick = { goalDialog = "steps" }) }
                item { SettingRow(Icons.Rounded.Bedtime, Tones.sleep, stringResource(R.string.profile_goal_sleep), value = Fmt.duration(s.goals.sleepMin), onClick = { goalDialog = "sleep" }) }
                item { SettingRow(Icons.Rounded.LocalFireDepartment, Tones.calories, stringResource(R.string.profile_goal_kcal), value = "${Fmt.int(s.goals.kcal)} ${stringResource(R.string.unit_kcal)}", onClick = { goalDialog = "kcal" }) }
                item { SettingRow(Icons.Rounded.Route, Tones.distance, stringResource(R.string.profile_goal_distance), value = Fmt.distance(s.goals.distanceM, s.units).let { "${it.first} ${it.second}" }, onClick = { goalDialog = "dist" }) }
            }
        }
        Box(Modifier.staggered(4)) {
            SettingsGroup(stringResource(R.string.profile_body), stringResource(R.string.profile_body_footer)) {
                item { SettingRow(Icons.Rounded.Height, Tones.distance, stringResource(R.string.profile_height), value = Fmt.height(p.heightCm, s.units), onClick = { open("profile/edit") }) }
                item { SettingRow(Icons.Rounded.MonitorWeight, Tones.pressure, stringResource(R.string.profile_weight), value = Fmt.weight(p.weightKg, s.units), onClick = { open("profile/edit") }) }
                p.bmi?.let { b -> item { SettingRow(Icons.Rounded.Speed, Tones.breath, stringResource(R.string.profile_bmi), stringResource(R.string.profile_bmi_sub), value = Fmt.dec(b.toDouble(), 1)) } }
                item { SettingRow(Icons.Rounded.Cake, Tones.heart, stringResource(R.string.profile_birth), value = p.birthDate?.let { Fmt.date(it, true) } ?: stringResource(R.string.profile_not_set), onClick = { open("profile/edit") }) }
                item { SettingRow(Icons.Rounded.Person, Tones.sleep, stringResource(R.string.profile_sex), value = stringResource(when (p.sex) { Sex.MALE -> R.string.profile_male; Sex.FEMALE -> R.string.profile_female; Sex.UNSET -> R.string.profile_sex_unset }), onClick = { open("profile/edit") }) }
            }
        }
        Box(Modifier.staggered(5)) {
            SettingsGroup {
                item { SettingRow(Icons.Rounded.Settings, Tones.device, stringResource(R.string.settings_title), onClick = { open("settings") }) }
                item { SettingRow(Icons.Rounded.PrivacyTip, Tones.activity, stringResource(R.string.profile_privacy), onClick = { open("privacy") }) }
                item { SettingRow(Icons.Rounded.Info, Tones.oxygen, stringResource(R.string.settings_about), onClick = { open("about") }) }
            }
        }
    }

    val g = s.goals
    when (goalDialog) {
        "steps" -> NumberDialog(stringResource(R.string.profile_goal_steps_dialog), g.steps, 1000..30000, 500, pluralStringResource(R.plurals.unit_steps, g.steps), { Fmt.int(it) }, { onGoals(g.copy(steps = it)) }, { goalDialog = null })
        "sleep" -> NumberDialog(stringResource(R.string.profile_goal_sleep_dialog), g.sleepMin, 240..720, 15, "", { Fmt.duration(it) }, { onGoals(g.copy(sleepMin = it)) }, { goalDialog = null })
        "kcal" -> NumberDialog(stringResource(R.string.profile_goal_kcal_dialog), g.kcal, 50..1500, 25, stringResource(R.string.unit_kcal), { Fmt.int(it) }, { onGoals(g.copy(kcal = it)) }, { goalDialog = null })
        "dist" -> NumberDialog(stringResource(R.string.profile_goal_distance_dialog), g.distanceM, 500..30000, 500, "", { Fmt.distance(it, s.units).let { d -> "${d.first} ${d.second}" } }, { onGoals(g.copy(distanceM = it)) }, { goalDialog = null })
    }
}

/** Shaxs kartochkasi: orbita-avatar, ism, yosh · bo'y · vazn, saqlash joyi va uchta jonli raqam. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityCard(s: AppSettings, days: Int, stats: ProfileStats?, modifier: Modifier, onEdit: () -> Unit) {
    val p = s.profile
    OCard(modifier, glow = avatarTones[p.avatar.mod(avatarTones.size)].a, radius = 32.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p, 76.dp)
            Spacer(Modifier.width(Space.l))
            Column(Modifier.weight(1f)) {
                Text(p.name.ifBlank { stringResource(R.string.profile_name_placeholder) }, style = OType.title, color = O.c.text, modifier = Modifier.semantics { heading() })
                Text(
                    listOfNotNull(p.age?.let { pluralStringResource(R.plurals.profile_age, it, it) }, Fmt.height(p.heightCm, s.units), Fmt.weight(p.weightKg, s.units)).joinToString(" · "),
                    style = OType.caption, color = O.c.text2,
                )
            }
            CircleIconButton(Icons.Rounded.Edit, onEdit, contentDescription = stringResource(R.string.cd_edit_profile))
        }
        Spacer(Modifier.height(Space.m))
        // ma'lumot faqat shu telefonda: bulut (akkaunt) rejimi yo'q
        Pill(stringResource(R.string.profile_storage_local), O.c.success, icon = Icons.Rounded.Lock)
        Spacer(Modifier.height(Space.l))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ProfileStat(stringResource(R.string.profile_with_orvell), days, pluralStringResource(R.plurals.unit_days, days))
            val total = stats?.total30 ?: 0
            ProfileStat(stringResource(R.string.profile_30_days), total, pluralStringResource(R.plurals.unit_steps, total))
            val record = stats?.record30 ?: 0
            ProfileStat(stringResource(R.string.profile_record), record, pluralStringResource(R.plurals.unit_steps, record))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: Int, unit: String) {
    Column(Modifier.padding(end = Space.m).semantics(mergeDescendants = true) { }) {
        Text(label, style = OType.label, color = O.c.text2)
        AnimatedNumber(value, { Fmt.int(it) }, unit = unit, style = OType.headline)
    }
}

/** Seriya: joriy va eng uzun seriya, so'nggi 14 kun nuqtalari (bajarilgan kun — rangli). */
@Composable
private fun StreakCard(st: ProfileStats, modifier: Modifier) {
    val tone = Tones.calories
    OCard(modifier, glow = tone.a) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (st.streak > 0) tone.brush else Brush.linearGradient(listOf(tone.soft(0.2f), tone.soft(0.08f)))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (st.streak > 0) O.c.onTone else tone.ink, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
                Overline(stringResource(R.string.profile_streak_title), color = tone.ink)
                AnimatedNumber(st.streak, { Fmt.int(it) }, unit = pluralStringResource(R.plurals.unit_days, st.streak), style = OType.display)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.semantics(mergeDescendants = true) { }) {
                Text(stringResource(R.string.profile_streak_best), style = OType.label, color = O.c.text2)
                ValueText(Fmt.int(st.best), pluralStringResource(R.plurals.unit_days, st.best), style = OType.headline)
            }
        }
        Spacer(Modifier.height(Space.l))
        val cd = stringResource(R.string.profile_streak_cd, st.lastMet, STREAK_DOTS)
        StreakDots(st.last, tone, Modifier.fillMaxWidth().height(14.dp).semantics { contentDescription = cd })
        Spacer(Modifier.height(Space.s))
        Text(stringResource(R.string.profile_streak_hint), style = OType.caption, color = O.c.text2)
    }
}

/** 14 ta nuqta: cho'tka keshda, gradient butun qator bo'ylab (bajarilgan kunlar "olov" rangida). */
@Composable
private fun StreakDots(met: List<Boolean>, tone: Tone, modifier: Modifier) {
    val track = O.c.track
    Spacer(
        modifier.drawWithCache {
            val brush = Brush.horizontalGradient(listOf(tone.b, tone.a), endX = size.width)
            val n = met.size.coerceAtLeast(1)
            val r = (size.height / 2).coerceAtMost(size.width / n / 2 * 0.7f)
            val step = if (n > 1) (size.width - 2 * r) / (n - 1) else 0f
            onDrawBehind {
                met.forEachIndexed { i, ok ->
                    val c = Offset(r + i * step, size.height / 2)
                    if (ok) drawCircle(brush, r, c) else drawCircle(track, r * 0.7f, c)
                }
            }
        },
    )
}

/** Yutuqlar: haqiqiy ma'lumotdan; qulflanganlari xira, progress halqa bilan. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AchievementsSection(st: ProfileStats, modifier: Modifier) {
    val perRow = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
    Column(modifier.padding(top = Space.s)) {
        Row(Modifier.fillMaxWidth().padding(start = Space.s, end = Space.xs, bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_achievements), style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f).semantics { heading() })
            Pill(stringResource(R.string.profile_achievements_count, st.earned, st.achievements.size), Tones.pressure.ink, icon = Icons.Rounded.EmojiEvents)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m), verticalArrangement = Arrangement.spacedBy(Space.m), maxItemsInEachRow = perRow) {
            st.achievements.forEach { AchievementTile(it, Modifier.weight(1f)) }
        }
        Text(stringResource(R.string.profile_achievements_footer), style = OType.caption, color = O.c.text2, modifier = Modifier.padding(start = Space.s, top = Space.s))
    }
}

private class AchStyle(val icon: ImageVector, val tone: Tone, val title: Int, val desc: Int)

private fun achStyle(id: Achievement.Id): AchStyle = when (id) {
    Achievement.Id.FIRST_DAY -> AchStyle(Icons.Rounded.Flag, Tones.brand, R.string.ach_first_day, R.string.ach_first_day_d)
    Achievement.Id.STREAK_3 -> AchStyle(Icons.Rounded.LocalFireDepartment, Tones.calories, R.string.ach_streak_3, R.string.ach_streak_3_d)
    Achievement.Id.STREAK_7 -> AchStyle(Icons.Rounded.LocalFireDepartment, Tones.calories, R.string.ach_streak_7, R.string.ach_streak_7_d)
    Achievement.Id.STREAK_30 -> AchStyle(Icons.Rounded.LocalFireDepartment, Tones.heart, R.string.ach_streak_30, R.string.ach_streak_30_d)
    Achievement.Id.STEPS_10K -> AchStyle(Icons.Rounded.Bolt, Tones.activity, R.string.ach_steps_10k, R.string.ach_steps_10k_d)
    Achievement.Id.STEPS_20K -> AchStyle(Icons.Rounded.EmojiEvents, Tones.pressure, R.string.ach_steps_20k, R.string.ach_steps_20k_d)
    Achievement.Id.SLEEP_GOAL_5 -> AchStyle(Icons.Rounded.Bedtime, Tones.sleep, R.string.ach_sleep_5, R.string.ach_sleep_5_d)
    Achievement.Id.TOTAL_100K -> AchStyle(Icons.Rounded.Public, Tones.distance, R.string.ach_total_100k, R.string.ach_total_100k_d)
}

@Composable
private fun AchievementTile(a: Achievement, modifier: Modifier) {
    val st = achStyle(a.id)
    val c = O.c
    OCard(modifier.semantics(mergeDescendants = true) { }, glow = if (a.earned) st.tone.a else null, padding = 14.dp, radius = 22.dp) {
        ProgressRing(a.progress, st.tone, a.earned, Modifier.size(52.dp)) {
            Icon(st.icon, null, tint = if (a.earned) st.tone.ink else c.text3, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(st.title), style = OType.bodyStrong, color = if (a.earned) c.text else c.text2)
        Text(stringResource(st.desc), style = OType.caption, color = c.text2)
        Spacer(Modifier.height(6.dp))
        if (a.earned) Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = st.tone.ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.ach_earned), style = OType.label, color = st.tone.ink)
        } else Text(stringResource(R.string.ach_progress, Fmt.int(a.value.coerceIn(0, a.target)), Fmt.int(a.target)), style = OType.label, color = c.text2)
    }
}

/** Yupqa progress halqa: bir marta o'sadi, cho'tka va chiziq keshda, qiymat chizish fazasida o'qiladi. */
@Composable
private fun ProgressRing(progress: Float, tone: Tone, earned: Boolean, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val grow = rememberGrow()
    val track = O.c.track
    val glowAlpha = O.c.glowAlpha
    Box(
        modifier.drawWithCache {
            val sw = size.minDimension * 0.09f
            val inset = Offset(sw / 2, sw / 2)
            val arc = Size(size.width - sw, size.height - sw)
            val brush = Brush.sweepGradient(listOf(tone.b, tone.a, tone.b), center = Offset(size.width / 2, size.height / 2))
            val fill = if (earned) Brush.radialGradient(listOf(tone.a.copy(alpha = glowAlpha), Color.Transparent)) else null
            val trackStroke = Stroke(sw)
            val stroke = Stroke(sw, cap = StrokeCap.Round)
            onDrawBehind {
                if (fill != null) drawCircle(fill)
                drawArc(track, 0f, 360f, false, inset, arc, style = trackStroke)
                val p = progress.coerceIn(0f, 1f) * grow.value
                if (p > 0f) rotate(-90f) { drawArc(brush, 0f, 360f * p, false, inset, arc, style = stroke) }
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun ProfileEditScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    var draft by remember { mutableStateOf(s.profile) }
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.profile_edit_title), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            item { ProfileForm(draft, s.units) { draft = it } }
        }
        PrimaryButton(stringResource(R.string.common_save), { vm.update { it.copy(profile = draft) }; onBack() }, Modifier.fillMaxWidth().padding(Space.gutter), enabled = draft.name.isNotBlank())
    }
}

/** Profil shakli (tahrirlash va onboarding uchun bitta). Rang tanlash — 48 dp radio tugmalar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileForm(p: Profile, units: uz.xasan.orvell.data.Units, onChange: (Profile) -> Unit) {
    var dateDialog by remember { mutableStateOf(false) }
    var heightDialog by remember { mutableStateOf(false) }
    var weightDialog by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Avatar(p, 96.dp) }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.Center) {
            avatarTones.forEachIndexed { i, t ->
                val sel = p.avatar == i
                val label = stringResource(R.string.cd_avatar_color, i + 1)
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .selectable(selected = sel, role = Role.RadioButton) { if (!sel) { haptics.tick(); onChange(p.copy(avatar = i)) } }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) { ColorDot(t, sel, 32.dp) }
            }
        }
        OField(p.name, { onChange(p.copy(name = it.take(40))) }, stringResource(R.string.profile_name))
        Overline(stringResource(R.string.profile_sex), modifier = Modifier.padding(start = 6.dp))
        Segmented(listOf(stringResource(R.string.profile_male), stringResource(R.string.profile_female), stringResource(R.string.profile_sex_hide)), when (p.sex) { Sex.MALE -> 0; Sex.FEMALE -> 1; Sex.UNSET -> 2 },
            { onChange(p.copy(sex = when (it) { 0 -> Sex.MALE; 1 -> Sex.FEMALE; else -> Sex.UNSET })) })
        SettingsGroup {
            item { SettingRow(Icons.Rounded.Cake, Tones.heart, stringResource(R.string.profile_birth), value = p.birthDate?.let { Fmt.date(it, true) } ?: stringResource(R.string.profile_choose), onClick = { dateDialog = true }) }
            item { SettingRow(Icons.Rounded.Height, Tones.distance, stringResource(R.string.profile_height), value = Fmt.height(p.heightCm, units), onClick = { heightDialog = true }) }
            item { SettingRow(Icons.Rounded.MonitorWeight, Tones.pressure, stringResource(R.string.profile_weight), value = Fmt.weight(p.weightKg, units), onClick = { weightDialog = true }) }
        }
    }
    if (dateDialog) {
        val init = (p.birthDate ?: LocalDate.now().minusYears(18)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val st = rememberDatePickerState(initialSelectedDateMillis = init, yearRange = 1920..LocalDate.now().year)
        DatePickerDialog(
            onDismissRequest = { dateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(p.copy(birthDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())) }
                    dateDialog = false
                }) { Text(stringResource(R.string.common_select), color = Tones.activity.ink, style = OType.bodyStrong) }
            },
            dismissButton = { TextButton(onClick = { dateDialog = false }) { Text(stringResource(R.string.common_cancel), color = O.c.text2, style = OType.bodyStrong) } },
        ) { DatePicker(st) }
    }
    if (heightDialog) NumberDialog(stringResource(R.string.profile_height), p.heightCm, 100..230, 1, stringResource(R.string.fmt_unit_cm), onConfirm = { onChange(p.copy(heightCm = it)) }, onDismiss = { heightDialog = false })
    if (weightDialog) NumberDialog(stringResource(R.string.profile_weight), Math.round(p.weightKg), 25..200, 1, stringResource(R.string.fmt_unit_kg), onConfirm = { onChange(p.copy(weightKg = it.toFloat())) }, onDismiss = { weightDialog = false })
}

/** Avatar rangi: tanlanganda tashqi halqa (rang yagona belgi emas — radio holati ham o'qiladi). */
@Composable
private fun ColorDot(t: Tone, selected: Boolean, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .border(if (selected) 3.dp else 0.dp, O.c.text, CircleShape)
            .padding(if (selected) 5.dp else 0.dp).clip(CircleShape).background(t.brush),
    )
}

// ---------------------------------------------------------------- previewlar

@OrvellPreviews
@Composable
private fun ProfilePreview() = PreviewSurface(Tones.sleep) {
    ProfileContent(PreviewData.settings, PreviewData.series(90), PreviewData.at(0) - 41L * 86_400_000, PreviewData.today, {}, {})
}

@OrvellPreviews
@Composable
private fun ProfileLoadingPreview() = PreviewSurface(Tones.sleep) {
    ProfileContent(PreviewData.settings, null, null, PreviewData.today, {}, {})
}

@ComponentPreviews
@Composable
private fun ProfileFormPreview() = PreviewSurface {
    Column(Modifier.padding(Space.gutter)) { ProfileForm(PreviewData.settings.profile, PreviewData.settings.units) {} }
}
