package uz.xasan.orvell.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.xasan.orvell.R
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Radius
import uz.xasan.orvell.ui.theme.Space
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones

// ---------------------------------------------------------------- fon va kartochka

/** Eski nom (moslik uchun): kontekst rangisiz aurora fon. */
@Composable
fun ScreenBackground(glow: Color? = null, content: @Composable BoxScope.() -> Unit) =
    AuroraBackground(glow?.let { Tone(it, it) }, content = content)

/**
 * "Shisha" kartochka: yuqoridan yorug'lik (ingichka yorug' qirra), ostida ko'rsatkich rangidagi yumshoq nur,
 * yorug' mavzuda — neytral soya. Bosilganda prujina bilan kichrayadi.
 */
@Composable
fun OCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glow: Color? = null,
    padding: Dp = Space.card,
    radius: Dp = Radius.card,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = O.c
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    val glowAlpha = c.glowAlpha
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .shadow(if (c.isDark) 0.dp else 10.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.surfaceHi, c.surface)))
            .drawWithCache {
                val r = size.maxDimension * 0.85f
                val brush = glow?.let { Brush.radialGradient(listOf(it.copy(alpha = glowAlpha), Color.Transparent), center = Offset(size.width * 0.1f, 0f), radius = r) }
                onDrawBehind { if (brush != null) drawCircle(brush, r, Offset(size.width * 0.1f, 0f)) }
            }
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(c.highlight, c.stroke, c.stroke))), shape)
            .then(if (onClick != null) Modifier.clickable(interaction, LocalIndication.current, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

// ---------------------------------------------------------------- belgi va matnlar

/** Ma'noli rangdagi belgi: yumshoq rangli "shisha" kvadrat, yuqori yorug' qirra. */
@Composable
fun IconBadge(icon: ImageVector, tone: Tone, size: Dp = 40.dp, filled: Boolean = false, contentDescription: String? = null) {
    val shape = RoundedCornerShape(size * 0.34f)
    val c = O.c
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (filled) tone.brush else Brush.linearGradient(listOf(tone.soft(if (c.isDark) 0.24f else 0.18f), tone.soft(0.08f))))
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (filled) 0.35f else 0.10f), Color.Transparent), endY = this.size.height * 0.55f))
            }
            .border(1.dp, Brush.verticalGradient(listOf(tone.a.copy(alpha = 0.45f), tone.a.copy(alpha = 0.08f))), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (filled) c.onTone else tone.ink, modifier = Modifier.size(size * 0.52f))
    }
}

/** Katta raqam + kichik birlik (masalan "8 412" + "qadam"). */
@Composable
fun ValueText(
    value: String,
    unit: String? = null,
    style: TextStyle = OType.number,
    color: Color = O.c.text,
    unitColor: Color = O.c.text2,
    modifier: Modifier = Modifier,
) {
    Text(
        buildAnnotatedString {
            append(value)
            if (!unit.isNullOrBlank()) withStyle(SpanStyle(fontSize = (style.fontSize.value * 0.46f).coerceAtLeast(12f).sp, color = unitColor, letterSpacing = 0.sp, fontFeatureSettings = "")) { append(" $unit") }
        },
        style = style, color = color, maxLines = 1, modifier = modifier,
    )
}

@Composable
fun Overline(text: String, color: Color = O.c.text3, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = OType.overline, color = color, modifier = modifier)

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null && onAction != null) {
            Text(action, style = OType.label, color = O.c.text2,
                modifier = Modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onAction).padding(8.dp))
        }
    }
}

/** Holat kapsulasi: rangli nuqta yoki belgi + matn (rang yagona belgi emas — matn doim bor). */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        else Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text, style = OType.label, color = color, maxLines = 1)
    }
}

// ---------------------------------------------------------------- tugmalar

/** Asosiy tugma: gradient, yuqori yorug' qirra va rangli nur. Katta shriftda balandligi o'sadi (min 56 dp). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, tone: Tone = Tones.brand, icon: ImageVector? = null) {
    val c = O.c
    val shape = CircleShape
    val interaction = remember { MutableInteractionSource() }
    val active = enabled && !loading
    Box(
        modifier
            .heightIn(min = 56.dp)
            .pressScale(interaction, active)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .shadow(if (active && c.isDark) 18.dp else if (active) 8.dp else 0.dp, shape, ambientColor = tone.a, spotColor = tone.a)
            .clip(shape)
            .background(tone.brush)
            .drawBehind { drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.Transparent), endY = size.height * 0.55f)) }
            .clickable(interaction, LocalIndication.current, enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(color = c.onTone, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = c.onTone, modifier = Modifier.size(20.dp))
            Text(text, style = OType.headline, color = c.onTone, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, color: Color = O.c.text) {
    val c = O.c
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = 52.dp)
            .pressScale(interaction, enabled)
            .clip(CircleShape)
            .background(c.surfaceHi)
            .border(1.dp, Brush.verticalGradient(listOf(c.highlight, c.strokeHi)), CircleShape)
            .clickable(interaction, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = color, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = OType.bodyStrong, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Dumaloq belgi-tugma: ko'rinishi 44 dp, bosiladigan joy kamida 48 dp, ekran o'qigich uchun tavsif. [enabled] = false bo'lsa bosilmaydi. */
@Composable
fun CircleIconButton(
    icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = O.c.text,
    contentDescription: String? = null, enabled: Boolean = true,
) {
    val c = O.c
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.minimumInteractiveComponentSize().size(size).pressScale(interaction, enabled, pressed = 0.92f).clip(CircleShape).background(c.surfaceHi)
            .border(1.dp, Brush.verticalGradient(listOf(c.highlight, c.stroke)), CircleShape)
            .clickable(interaction, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

// ---------------------------------------------------------------- sarlavhalar

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack, contentDescription = stringResource(R.string.cd_back))
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f).semantics { heading() }, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
    }
}

@Composable
fun LargeTitle(title: String, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Overline(subtitle)
            Text(title, style = OType.display, color = O.c.text, modifier = Modifier.semantics { heading() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = trailing)
    }
}

// ---------------------------------------------------------------- segment tanlagich

/** Bo'lim tanlagich: tanlangan "shisha" kapsula prujina bilan suriladi. Har varianti 48 dp balandlikda. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = O.c
    val haptics = rememberHaptics()
    BoxWithConstraints(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape).background(c.surfaceHi)
            .border(1.dp, c.stroke, CircleShape).padding(3.dp).selectableGroup(),
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, Motion.settle(), label = "seg")
        Box(
            Modifier.offset(x = x).width(w).height(maxHeight).clip(CircleShape)
                .background(if (c.isDark) c.raised else Color.White)
                .border(1.dp, Brush.verticalGradient(listOf(c.highlight, c.strokeHi)), CircleShape),
        )
        Row(Modifier.fillMaxWidth().heightIn(min = 42.dp)) {
            options.forEachIndexed { i, s ->
                Box(
                    Modifier.weight(1f).heightIn(min = 42.dp).clip(CircleShape)
                        .selectable(selected = i == selected, role = Role.Tab) { if (i != selected) { haptics.tick(); onSelect(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s, style = OType.label, color = if (i == selected) c.text else c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- sozlama qatorlari

class SettingsScope { val rows = mutableListOf<@Composable () -> Unit>(); fun item(content: @Composable () -> Unit) { rows += content } }

@Composable
fun SettingsGroup(title: String? = null, footer: String? = null, build: SettingsScope.() -> Unit) {
    val scope = SettingsScope().apply(build)
    if (scope.rows.isEmpty()) return  // qidiruvda hamma qator yashirilgan bo'lsa guruh ham ko'rinmaydi
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (title != null) Overline(title, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).semantics { heading() })
        OCard(padding = 0.dp, radius = 24.dp) {
            scope.rows.forEachIndexed { i, row ->
                row()
                if (i < scope.rows.lastIndex) HorizontalDivider(color = O.c.stroke, modifier = Modifier.padding(start = 64.dp))
            }
        }
        if (footer != null) Text(footer, style = OType.caption, color = O.c.text3, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    tone: Tone,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = O.c
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, if (danger) Tones.danger else tone, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = OType.bodyStrong, color = if (danger) c.danger else c.text)
            if (subtitle != null) Text(subtitle, style = OType.caption, color = c.text2)
        }
        // Qiymat bitta qator va o'z kengligida; sarlavha (weight) qolgan joyni oladi
        if (value != null) Text(value, style = OType.body, color = c.text2, maxLines = 1, softWrap = false,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp))
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
        else if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.text3, modifier = Modifier.padding(start = 4.dp).size(20.dp))
    }
}

/** Kalitli qator: butun qator bitta "switch" sifatida o'qiladi (ekran o'qigich uchun). */
@Composable
fun SwitchRow(icon: ImageVector, tone: Tone, title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = O.c
    val haptics = rememberHaptics()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) { haptics.toggle(it); onChange(it) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tone, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = OType.bodyStrong, color = c.text)
            if (subtitle != null) Text(subtitle, style = OType.caption, color = c.text2)
        }
        Spacer(Modifier.width(8.dp))
        OSwitch(checked, enabled, onChange = null)
    }
}

@Composable
fun OSwitch(checked: Boolean, enabled: Boolean = true, onChange: ((Boolean) -> Unit)?) {
    val c = O.c
    Switch(
        checked = checked, onCheckedChange = onChange, enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onTone, checkedTrackColor = Tones.activity.a, checkedBorderColor = Tones.activity.a,
            uncheckedThumbColor = c.text2, uncheckedTrackColor = c.surfacePressed, uncheckedBorderColor = c.strokeHi,
        ),
    )
}

// ---------------------------------------------------------------- bo'sh holat

/** Bo'sh / xato holat: rasm (Canvas) yoki belgi, sarlavha, tushuntirish va bitta asosiy amal. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    tone: Tone = Tones.device,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    art: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (art != null) art() else IconBadge(icon, tone, size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = OType.title, color = O.c.text, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(6.dp))
        Text(body, style = OType.body, color = O.c.text2, textAlign = TextAlign.Center)
        if (action != null && onAction != null) { Spacer(Modifier.height(18.dp)); PrimaryButton(action, onAction, Modifier.fillMaxWidth(), tone = tone) }
    }
}

// ---------------------------------------------------------------- dialoglar

@Composable
private fun ODialog(title: String, onDismiss: () -> Unit, confirm: (@Composable () -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val c = O.c
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.raised,
        shape = RoundedCornerShape(Radius.card),
        title = { Text(title, style = OType.title, color = c.text) },
        text = { Column(content = content) },
        confirmButton = { confirm?.invoke() },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = c.text2, style = OType.bodyStrong) } },
    )
}

@Composable
fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    val c = O.c
    ODialog(title, onDismiss, confirm = null) {
        Column(Modifier.selectableGroup()) {
            options.forEach { (v, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                        .selectable(selected = v == selected, role = Role.RadioButton) { onSelect(v); onDismiss() }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = v == selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Tones.activity.ink, unselectedColor = c.text3))
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = OType.body, color = c.text)
                }
            }
        }
    }
}

@Composable
fun NumberDialog(title: String, value: Int, range: IntRange, step: Int = 1, unit: String = "", format: (Int) -> String = { it.toString() }, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = O.c
    var v by remember { mutableFloatStateOf(value.coerceIn(range).toFloat()) }
    val snapped = (Math.round(v / step) * step).coerceIn(range.first, range.last)
    ODialog(title, onDismiss, confirm = { TextButton(onClick = { onConfirm(snapped); onDismiss() }) { Text(stringResource(R.string.common_save), color = Tones.activity.ink, style = OType.bodyStrong) } }) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            ValueText(format(snapped), unit, style = OType.display)
        }
        Slider(
            value = v, onValueChange = { v = it }, valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Tones.activity.a, activeTrackColor = Tones.activity.a, inactiveTrackColor = c.track),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(format(range.first), style = OType.caption, color = c.text3, modifier = Modifier.weight(1f))
            Text(format(range.last), style = OType.caption, color = c.text3)
        }
    }
}

@Composable
fun TextDialog(title: String, value: String, label: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, singleLine: Boolean = true) {
    var t by remember { mutableStateOf(value) }
    ODialog(title, onDismiss, confirm = { TextButton(onClick = { onConfirm(t.trim()); onDismiss() }) { Text(stringResource(R.string.common_save), color = Tones.activity.ink, style = OType.bodyStrong) } }) {
        OField(t, { t = it }, label, singleLine = singleLine)
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, danger: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = O.c
    ODialog(title, onDismiss, confirm = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = if (danger) c.danger else Tones.activity.ink, style = OType.bodyStrong) } }) {
        Text(body, style = OType.body, color = c.text2)
    }
}

@Composable
fun OField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, isPassword: Boolean = false,
           keyboard: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
           leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val c = O.c
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine,
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.field), keyboardOptions = keyboard, leadingIcon = leading, trailingIcon = trailing,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Tones.activity.ink, unfocusedBorderColor = c.strokeHi, focusedLabelColor = Tones.activity.ink, unfocusedLabelColor = c.text2,
            focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = Tones.activity.ink,
            focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
        ),
    )
}

val ScreenPadding = PaddingValues(horizontal = Space.gutter)
