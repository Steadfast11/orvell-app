package uz.xasan.orvell.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.R
import uz.xasan.orvell.data.Goals
import uz.xasan.orvell.data.StorageMode
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.Art
import uz.xasan.orvell.ui.components.CircleIconButton
import uz.xasan.orvell.ui.components.IconBadge
import uz.xasan.orvell.ui.components.Illustration
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.PrimaryButton
import uz.xasan.orvell.ui.components.SecondaryButton
import uz.xasan.orvell.ui.components.ValueText
import uz.xasan.orvell.ui.components.rememberHaptics
import uz.xasan.orvell.ui.components.staggered
import uz.xasan.orvell.ui.preview.OrvellPreviews
import uz.xasan.orvell.ui.preview.PreviewData
import uz.xasan.orvell.ui.preview.PreviewSurface
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Space
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones

private const val STEPS = 7

/**
 * "Kinematik" onboarding: har qadamda Canvas sahna, yuqorida jonli progress chizig'i,
 * mazmun ketma-ket (stagger) kiradi, asosiy tugma pastga mahkamlangan. Mantiq o'zgarmagan.
 */
@Composable
fun OnboardingFlow(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf(s.profile) }
    val next = { step = (step + 1).coerceAtMost(STEPS - 1) }
    val back = { step = (step - 1).coerceAtLeast(0) }
    val reduce = O.reduceMotion
    BackHandler(enabled = step > 0) { back() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (step > 0) StepBar(step, back)
        AnimatedContent(
            step, Modifier.weight(1f),
            transitionSpec = {
                if (reduce) fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                else {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(Motion.CONTAINER_MS, easing = Motion.Emphasized)) { it / 5 * dir } + fadeIn(tween(Motion.ENTER_MS))) togetherWith
                        (slideOutHorizontally(tween(Motion.ENTER_MS, easing = Motion.Emphasized)) { -it / 5 * dir } + fadeOut(tween(200)))
                }
            }, label = "onb",
        ) { st ->
            when (st) {
                0 -> WelcomeStep(next)
                1 -> StorageStep(s.storage, { m -> vm.update { it.copy(storage = m) } }, next)
                2 -> StepScaffold(
                    art = null, title = stringResource(R.string.onb_about_title), body = stringResource(R.string.onb_about_body),
                    cta = { PrimaryButton(stringResource(R.string.common_continue), { vm.update { it.copy(profile = profile) }; next() }, Modifier.fillMaxWidth(), enabled = profile.name.isNotBlank()) },
                ) { Box(Modifier.staggered(2)) { ProfileForm(profile, s.units) { profile = it } } }
                3 -> GoalsStep(s.goals, { g -> vm.update { it.copy(goals = g) } }, next)
                4 -> PermissionsStep(next)
                5 -> StepScaffold(art = null, title = stringResource(R.string.onb_pair_title), body = stringResource(R.string.onb_pair_body), cta = null) {
                    Box(Modifier.staggered(2)) { PairContent(vm, onPaired = next, skip = next) }
                }
                else -> DoneStep(s.profile.name) {
                    vm.update { it.copy(onboarded = true) }
                    vm.startService()
                }
            }
        }
    }
}

/** Orqaga tugmasi + jonli progress chizig'i + "2/6" hisoblagich. */
@Composable
private fun StepBar(step: Int, onBack: () -> Unit) {
    val total = STEPS - 1
    val cd = stringResource(R.string.onb_step_cd, step, total)
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack, contentDescription = stringResource(R.string.cd_back))
        Spacer(Modifier.width(12.dp))
        StepProgress(step.toFloat() / total, Modifier.weight(1f).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(step.toFloat(), 0f..total.toFloat())
            contentDescription = cd
        })
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.onb_step_count, step, total), style = OType.label, color = O.c.text2, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.width(8.dp))
    }
}

/** Progress chizig'i: gradient keshda, animatsiyalangan qiymat faqat chizish fazasida o'qiladi. */
@Composable
private fun StepProgress(fraction: Float, modifier: Modifier) {
    val reduce = O.reduceMotion
    val p by animateFloatAsState(fraction, if (reduce) tween(0) else tween(Motion.CONTAINER_MS * 2, easing = Motion.Emphasized), label = "onbProgress")
    val track = O.c.track
    Spacer(
        modifier.height(8.dp).drawWithCache {
            val brush = Brush.horizontalGradient(listOf(Tones.brand.a, Tones.brand.b), endX = size.width)
            val r = CornerRadius(size.height / 2)
            onDrawBehind {
                drawRoundRect(track, cornerRadius = r)
                val w = size.width * p.coerceIn(0f, 1f)
                if (w > 0f) drawRoundRect(brush, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r)
            }
        },
    )
}

/** Qadam qolipi: aylanuvchi mazmun (sahna, sarlavha, mazmun) va pastga mahkamlangan amal tugmalari. */
@Composable
private fun StepScaffold(
    art: Art?,
    title: String,
    body: String,
    cta: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter + 4.dp, vertical = Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            if (art != null) Box(Modifier.fillMaxWidth().staggered(0), contentAlignment = Alignment.Center) { Illustration(art, size = sceneSize(168.dp)) }
            Column(Modifier.padding(top = Space.s).staggered(1)) {
                Text(title, style = OType.display, color = O.c.text, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Space.s))
                Text(body, style = OType.body, color = O.c.text2)
            }
            content()
            Spacer(Modifier.height(Space.s))
        }
        if (cta != null) Column(
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter + 4.dp, vertical = Space.m),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = cta,
        )
    }
}

/** Katta shriftda sahna kichrayadi — matn va tugmalarga joy qolsin. */
@Composable
private fun sceneSize(base: Dp): Dp = if (LocalDensity.current.fontScale > 1.3f) base * 0.66f else base

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter + 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Spacer(Modifier.height(Space.l))
            Box(Modifier.staggered(0)) { Illustration(Art.WELCOME, size = sceneSize(260.dp)) }
            Column(Modifier.staggered(1), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), style = OType.hero, color = O.c.text, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_tagline), style = OType.bodyStrong, color = O.c.text2, textAlign = TextAlign.Center)
            }
            OCard(Modifier.staggered(2), glow = Tones.brand.a, padding = 0.dp) {
                Feature(Icons.Rounded.Favorite, Tones.heart, stringResource(R.string.onb_feature1_t), stringResource(R.string.onb_feature1))
                HorizontalDivider(color = O.c.stroke, modifier = Modifier.padding(start = 72.dp))
                Feature(Icons.Rounded.Shield, Tones.activity, stringResource(R.string.onb_feature2_t), stringResource(R.string.onb_feature2))
                HorizontalDivider(color = O.c.stroke, modifier = Modifier.padding(start = 72.dp))
                Feature(Icons.Rounded.Code, Tones.oxygen, stringResource(R.string.onb_feature3_t), stringResource(R.string.onb_feature3))
            }
            Spacer(Modifier.height(Space.s))
        }
        PrimaryButton(
            stringResource(R.string.onb_start), onStart,
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter + 4.dp, vertical = Space.m).staggered(3),
            icon = Icons.Rounded.RocketLaunch,
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, tone: Tone, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tone, 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = OType.bodyStrong, color = O.c.text)
            Text(body, style = OType.caption, color = O.c.text2)
        }
    }
}

/** Saqlash joyi: faqat telefonda (bulut/akkaunt rejimi yo'q). Karta tanlangan holatda ko'rinadi. */
@Composable
private fun StorageStep(current: StorageMode, onPick: (StorageMode) -> Unit, next: () -> Unit) {
    StepScaffold(
        Art.STORAGE, stringResource(R.string.onb_storage_title), stringResource(R.string.onb_storage_body),
        cta = { PrimaryButton(stringResource(R.string.common_continue), next, Modifier.fillMaxWidth()) },
    ) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ChoiceCard(
                Icons.Rounded.Lock, Tones.activity, stringResource(R.string.onb_local_title), current == StorageMode.LOCAL,
                lines = listOf(stringResource(R.string.onb_local_1), stringResource(R.string.onb_local_2), stringResource(R.string.onb_local_3)),
                modifier = Modifier.staggered(2),
            ) { onPick(StorageMode.LOCAL) }
        }
    }
}

/** Tanlov kartochkasi: ekran o'qigich uchun radio tugma, tanlanganda gradient qirra va belgi. */
@Composable
private fun ChoiceCard(
    icon: ImageVector, tone: Tone, title: String, selected: Boolean, lines: List<String>,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val c = O.c
    val shape = RoundedCornerShape(26.dp)
    val haptics = rememberHaptics()
    Box(
        modifier.fillMaxWidth().clip(shape)
            .selectable(selected = selected, role = Role.RadioButton) { haptics.tick(); onClick() }
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) tone.brush else Brush.verticalGradient(listOf(c.strokeHi, c.stroke)), shape,
            ),
    ) {
        OCard(glow = if (selected) tone.a else null, radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, tone, 46.dp, filled = selected)
                Spacer(Modifier.width(14.dp))
                Text(title, style = OType.headline, color = c.text, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = tone.ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(Space.m))
            lines.forEach { line ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Check, null, tint = tone.ink, modifier = Modifier.padding(top = 2.dp).size(15.dp))
                    Spacer(Modifier.width(Space.s))
                    Text(line, style = OType.caption, color = c.text2)
                }
            }
        }
    }
}

@Composable
private fun GoalsStep(goals: Goals, onGoals: (Goals) -> Unit, next: () -> Unit) {
    StepScaffold(
        Art.GOALS, stringResource(R.string.onb_goals_title), stringResource(R.string.onb_goals_body),
        cta = { PrimaryButton(stringResource(R.string.common_continue), next, Modifier.fillMaxWidth()) },
    ) {
        OCard(Modifier.staggered(2), glow = Tones.activity.a) {
            Overline(stringResource(R.string.onb_goal_steps), color = Tones.activity.ink)
            ValueText(Fmt.int(goals.steps), pluralStringResource(R.plurals.unit_steps, goals.steps), style = OType.display)
            Spacer(Modifier.height(Space.m))
            Chips(listOf(5000, 8000, 10000, 12000), goals.steps, { Fmt.int(it) }, Tones.activity) { onGoals(goals.copy(steps = it)) }
        }
        OCard(Modifier.staggered(3), glow = Tones.sleep.a) {
            Overline(stringResource(R.string.onb_goal_sleep), color = Tones.sleep.ink)
            ValueText(Fmt.duration(goals.sleepMin), null, style = OType.display)
            Spacer(Modifier.height(Space.m))
            Chips(listOf(420, 450, 480, 540), goals.sleepMin, { Fmt.duration(it) }, Tones.sleep) { onGoals(goals.copy(sleepMin = it)) }
        }
    }
}

/** Tez tanlov kapsulalari: 48 dp, katta shriftda yangi qatorga o'tadi, radio semantikasi. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(values: List<Int>, selected: Int, label: (Int) -> String, tone: Tone, onPick: (Int) -> Unit) {
    val c = O.c
    val haptics = rememberHaptics()
    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s), maxItemsInEachRow = 4) {
        values.forEach { v ->
            val sel = v == selected
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                    .background(if (sel) tone.brush else Brush.linearGradient(listOf(c.surfaceHi, c.surfaceHi)))
                    .border(1.dp, if (sel) Color.Transparent else c.stroke, CircleShape)
                    .selectable(selected = sel, role = Role.RadioButton) { if (!sel) { haptics.tick(); onPick(v) } }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label(v), style = OType.label, color = if (sel) c.onTone else c.text, maxLines = 1, softWrap = false) }
        }
    }
}

@Composable
private fun PermissionsStep(next: () -> Unit) {
    val ctx = LocalContext.current
    val needNotif = Build.VERSION.SDK_INT >= 33
    var notifGranted by remember { mutableStateOf(!needNotif || hasPermissions(ctx, arrayOf(Manifest.permission.POST_NOTIFICATIONS))) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { asked = true; notifGranted = it }
    PermissionsContent(
        granted = notifGranted, denied = asked && !notifGranted, canAsk = !notifGranted && needNotif && !asked,
        onAsk = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }, next = next,
    )
}

@Composable
private fun PermissionsContent(granted: Boolean, denied: Boolean, canAsk: Boolean, onAsk: () -> Unit, next: () -> Unit) {
    StepScaffold(
        Art.NOTIFY, stringResource(R.string.onb_notif_title), stringResource(R.string.onb_notif_body),
        cta = {
            if (canAsk) PrimaryButton(stringResource(R.string.common_grant), onAsk, Modifier.fillMaxWidth(), icon = Icons.Rounded.Notifications)
            if (granted) PrimaryButton(stringResource(R.string.common_continue), next, Modifier.fillMaxWidth())
            else SecondaryButton(stringResource(R.string.onb_skip), next, Modifier.fillMaxWidth())
        },
    ) {
        OCard(Modifier.staggered(2), glow = Tones.sleep.a) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Notifications, Tones.sleep, 46.dp, filled = granted)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.onb_notif_title), style = OType.headline, color = O.c.text)
                    Text(
                        stringResource(if (granted) R.string.onb_notif_granted else R.string.onb_notif_requested),
                        style = OType.caption, color = if (granted) O.c.success else O.c.text2,
                    )
                }
                if (granted) Icon(Icons.Rounded.CheckCircle, null, tint = O.c.success, modifier = Modifier.size(24.dp))
            }
        }
        if (denied) Text(stringResource(R.string.onb_notif_denied), style = OType.caption, color = O.c.warning, modifier = Modifier.staggered(3))
        Text(stringResource(R.string.onb_notif_bt_later), style = OType.caption, color = O.c.text2, modifier = Modifier.staggered(3))
    }
}

@Composable
private fun DoneStep(name: String, finish: () -> Unit) {
    val haptics = rememberHaptics()
    LaunchedEffect(Unit) { haptics.celebrate() }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter + 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Spacer(Modifier.height(Space.xl))
            Box(Modifier.staggered(0)) { Illustration(Art.DONE, size = sceneSize(240.dp)) }
            Text(
                if (name.isNotBlank()) stringResource(R.string.onb_done_title_name, name) else stringResource(R.string.onb_done_title),
                style = OType.display, color = O.c.text, textAlign = TextAlign.Center, modifier = Modifier.staggered(1).semantics { heading() },
            )
            Text(stringResource(R.string.onb_done_body), style = OType.body, color = O.c.text2, textAlign = TextAlign.Center, modifier = Modifier.staggered(2))
        }
        PrimaryButton(stringResource(R.string.onb_open), finish, Modifier.fillMaxWidth().padding(horizontal = Space.gutter + 4.dp, vertical = Space.m).staggered(3))
    }
}

// ---------------------------------------------------------------- previewlar

@OrvellPreviews
@Composable
private fun OnbWelcomePreview() = PreviewSurface(Tones.brand) { WelcomeStep {} }

@OrvellPreviews
@Composable
private fun OnbStoragePreview() = PreviewSurface(Tones.activity) {
    Column { StepBar(1) {}; StorageStep(StorageMode.LOCAL, {}, {}) }
}

@OrvellPreviews
@Composable
private fun OnbGoalsPreview() = PreviewSurface(Tones.activity) {
    Column { StepBar(3) {}; GoalsStep(PreviewData.settings.goals, {}, {}) }
}

@Preview(name = "Notify · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, widthDp = 393, heightDp = 852)
@Preview(name = "Notify · light", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, widthDp = 393, heightDp = 852)
@Composable
private fun OnbNotifyPreview() = PreviewSurface(Tones.sleep) {
    Column { StepBar(4) {}; PermissionsContent(granted = false, denied = true, canAsk = false, onAsk = {}, next = {}) }
}

@OrvellPreviews
@Composable
private fun OnbDonePreview() = PreviewSurface(Tones.brand) { DoneStep("Aziz") {} }
