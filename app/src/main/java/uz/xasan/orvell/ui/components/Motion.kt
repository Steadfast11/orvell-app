package uz.xasan.orvell.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O

// Harakat yordamchilari: kirish (stagger), bosilish (spring) va haptika. Hammasi "reduce motion"ni hurmat qiladi.

/** Ro'yxat elementining kirish animatsiyasi: index bo'yicha kechikish (maks. 8 ta), pastdan yengil ko'tarilish. */
@Composable
fun rememberEntrance(index: Int): Animatable<Float, AnimationVector1D> {
    val reduce = O.reduceMotion
    val a = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce || a.value >= 1f) return@LaunchedEffect
        delay(index.coerceIn(0, 8) * Motion.STAGGER_MS.toLong())
        a.animateTo(1f, tween(Motion.ENTER_MS, easing = Motion.Emphasized))
    }
    return a
}

/** Kirish holatini chizish fazasida qo'llaydi (qayta kompozitsiyasiz). */
fun Modifier.entrance(progress: Animatable<Float, AnimationVector1D>, risePx: Float): Modifier =
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * risePx
    }

/** Qisqa yo'l: index bo'yicha kirish animatsiyasi bilan modifier. */
@Composable
fun Modifier.staggered(index: Int): Modifier {
    val e = rememberEntrance(index)
    val rise = with(LocalDensity.current) { 24.dp.toPx() }
    return this.entrance(e, rise)
}

/** Bosilganda prujina bilan biroz kichrayish (kartochka va tugmalar). */
@Composable
fun Modifier.pressScale(interaction: InteractionSource, enabled: Boolean = true, pressed: Float = 0.97f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val reduce = O.reduceMotion
    val s by animateFloatAsState(if (isPressed && enabled && !reduce) pressed else 1f, Motion.press(), label = "press")
    return graphicsLayer { scaleX = s; scaleY = s }
}

/** Haptik javoblar (docs/UI-REDESIGN.md §3.4): grafik bo'ylab yurish, tasdiq, maqsad yopilishi, xavfli amal. */
@Stable
class Haptics(private val h: HapticFeedback) {
    fun tick() = h.performHapticFeedback(HapticFeedbackType.SegmentTick)
    fun confirm() = h.performHapticFeedback(HapticFeedbackType.Confirm)
    fun celebrate() = h.performHapticFeedback(HapticFeedbackType.LongPress)
    fun reject() = h.performHapticFeedback(HapticFeedbackType.Reject)
    fun toggle(on: Boolean) = h.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
}

@Composable
fun rememberHaptics(): Haptics {
    val h = LocalHapticFeedback.current
    return remember(h) { Haptics(h) }
}
