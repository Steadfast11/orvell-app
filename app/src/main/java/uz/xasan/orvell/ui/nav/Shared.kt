package uz.xasan.orvell.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.Radius
import uz.xasan.orvell.ui.theme.Tone

/** Kartochka -> batafsil ekran "konteyner" o'tishi uchun doiralar (MainActivity beradi). */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Joriy kontekst rangi (aurora fon uchun). */
val LocalAuroraTone = staticCompositionLocalOf { mutableStateOf<Tone?>(null) }

/** Kartochka va batafsil ekran bir xil kalit bilan belgilansa, kartochka "shisha"si ekranga kengayadi. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedContainer(key: String): Modifier {
    val shared = LocalSharedScope.current
    val anim = LocalNavAnimScope.current
    if (shared == null || anim == null || O.reduceMotion) return this
    return with(shared) {
        this@sharedContainer.sharedBounds(
            rememberSharedContentState(key),
            animatedVisibilityScope = anim,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(Radius.card)),
        )
    }
}

/** Batafsil ekran manzili: qaysi ko'rsatkich va qaysi ekrandan ochildi (o'tish kaliti uchun). */
fun detailRoute(kind: String, from: String) = "detail/$kind/$from"
fun sharedKey(kind: String, from: String) = "metric-$from-$kind"
