package uz.xasan.orvell.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import kotlin.math.roundToInt

/**
 * Sanab chiqadigan raqam: birinchi ko'rinishda 0 dan, keyin eski qiymatdan yangisiga.
 * Raqamlar tabular (tnum) — kenglik "sakramaydi". Reduce motion: darhol yakuniy qiymat.
 */
@Composable
fun AnimatedNumber(
    value: Int,
    format: (Int) -> String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: TextStyle = OType.number,
    color: Color = O.c.text,
) {
    val reduce = O.reduceMotion
    val anim = remember { Animatable(if (reduce) value.toFloat() else 0f) }
    LaunchedEffect(value, reduce) {
        if (reduce) anim.snapTo(value.toFloat()) else anim.animateTo(value.toFloat(), tween(Motion.FILL_MS, easing = Motion.EaseOutCubic))
    }
    ValueText(format(anim.value.roundToInt()), unit, style = style, color = color, modifier = modifier)
}
