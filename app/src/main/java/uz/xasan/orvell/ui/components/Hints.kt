package uz.xasan.orvell.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uz.xasan.orvell.R
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Radius
import uz.xasan.orvell.ui.theme.Tones

// ---------------------------------------------------------------- UI'ning o'z sozlamalari

/**
 * Faqat interfeys holati (maslahatlar ko'rsatilganmi). Ma'lumot qatlamiga tegmaydi:
 * alohida SharedPreferences "orvell_ui".
 */
object UiPrefs {
    private const val NAME = "orvell_ui"
    private fun p(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun seen(ctx: Context, key: String) = runCatching { p(ctx).getBoolean("hint_$key", false) }.getOrDefault(false)
    fun markSeen(ctx: Context, key: String) { runCatching { p(ctx).edit().putBoolean("hint_$key", true).apply() } }
    fun reset(ctx: Context) { runCatching { p(ctx).edit().clear().apply() } }
}

@Stable
class HintState(initial: Boolean, private val onSeen: () -> Unit) {
    var visible by mutableStateOf(initial)
        private set
    fun dismiss() { if (visible) { visible = false; onSeen() } }
}

/** Bir martalik maslahat: yopilgach qayta ko'rinmaydi. */
@Composable
fun rememberHint(key: String, enabled: Boolean = true): HintState {
    val ctx = LocalContext.current
    return remember(key) { HintState(enabled && !UiPrefs.seen(ctx, key)) { UiPrefs.markSeen(ctx, key) } }
}

/** Maslahat pufakchasi (coach mark): yumshoq paydo bo'ladi, bosilsa yoki "yopish" bilan yo'qoladi. */
@Composable
fun CoachMark(hint: HintState, text: String, modifier: Modifier = Modifier) {
    val c = O.c
    AnimatedVisibility(hint.visible, modifier, enter = fadeIn() + scaleIn(initialScale = 0.92f), exit = fadeOut()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.row)).background(c.raised)
                .border(1.dp, Brush.horizontalGradient(listOf(Tones.brand.a.copy(alpha = 0.6f), Tones.brand.b.copy(alpha = 0.3f))), RoundedCornerShape(Radius.row))
                .clickable(role = Role.Button) { hint.dismiss() }
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.TipsAndUpdates, null, tint = Tones.brand.ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = OType.caption, color = c.text, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Close, stringResource(R.string.cd_close), tint = c.text2,
                modifier = Modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { hint.dismiss() }.padding(8.dp).size(18.dp))
        }
    }
}

// ---------------------------------------------------------------- snackbar va "Qaytarish"

/** Ilova darajasidagi snackbar va korutina doirasi (ekran yopilsa ham kechiktirilgan amal bajariladi). */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }
val LocalAppScope = staticCompositionLocalOf<CoroutineScope?> { null }

@Composable
fun OrvellSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val c = O.c
    SnackbarHost(state, modifier) { data ->
        Snackbar(data, shape = RoundedCornerShape(Radius.row), containerColor = c.raised, contentColor = c.text, actionColor = Tones.activity.ink)
    }
}

/**
 * Xavfli amalni kechiktiradi: "Qaytarish" bosilmasa, snackbar yopilgach bajariladi.
 * Qaytaradi: (xabar, amal) -> Unit.
 */
@Composable
fun rememberUndoable(): (String, () -> Unit) -> Unit {
    val host = LocalSnackbar.current
    val scope = LocalAppScope.current ?: rememberCoroutineScope()
    val undo = stringResource(R.string.undo)
    return remember(host, scope, undo) {
        { message, perform ->
            scope.launch {
                host.currentSnackbarData?.dismiss()
                val r = host.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Short)
                if (r != SnackbarResult.ActionPerformed) perform()
            }
        }
    }
}
