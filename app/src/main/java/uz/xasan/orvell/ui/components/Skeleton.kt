package uz.xasan.orvell.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.Radius

/** Yuklanayotgan joy: kartochka shaklidagi bo'sh blok, ustidan yumshoq nur o'tadi (reduce motion: harakatsiz). */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    val c = O.c
    val reduce = O.reduceMotion
    val t = if (reduce) remember { mutableFloatStateOf(0.5f) }
        else rememberInfiniteTransition(label = "shimmer").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val base = c.track
    val shine = if (c.isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.7f)
    Box(
        modifier.clip(shape).background(base).drawWithCache {
            val w = size.width * 0.6f
            val brush = Brush.horizontalGradient(listOf(Color.Transparent, shine, Color.Transparent), startX = 0f, endX = w)
            onDrawBehind {
                translate(left = -w + (size.width + w) * t.value) { drawRect(brush, topLeft = Offset.Zero, size = size.copy(width = w)) }
            }
        },
    )
}

/** Kartochka skeleti: sarlavha, katta qiymat va chiziqlar (haqiqiy kartochka o'lchamida). */
@Composable
fun SkeletonCard(height: Dp = 160.dp, modifier: Modifier = Modifier) {
    OCard(modifier.clearAndSetSemantics { }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(36.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.width(12.dp))
            SkeletonBlock(Modifier.width(120.dp).height(14.dp), CircleShape)
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBlock(Modifier.width(140.dp).height(32.dp), RoundedCornerShape(10.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height((height - 110.dp).coerceAtLeast(12.dp)), RoundedCornerShape(Radius.field))
        }
    }
}
