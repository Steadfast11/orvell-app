package uz.xasan.orvell.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.AuroraBackground
import uz.xasan.orvell.ui.fmtTextFrom
import uz.xasan.orvell.ui.theme.OrvellTheme
import uz.xasan.orvell.ui.theme.ThemeMode
import uz.xasan.orvell.ui.theme.Tone

/** Har ekran/komponent uchun 4 holat: qorong'i, yorug', 200% shrift, ruscha 360 dp (docs/UI-REDESIGN.md §9). */
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 393, heightDp = 852)
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 393, heightDp = 852)
@Preview(name = "Dark · 200% font", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, fontScale = 2f, widthDp = 393, heightDp = 852)
@Preview(name = "RU · 360 dp", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, locale = "ru", widthDp = 360, heightDp = 780)
annotation class OrvellPreviews

/** Komponentlar uchun ixcham variant. */
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 393)
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL, widthDp = 393)
@Preview(name = "Dark · 200% font", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL, fontScale = 2f, widthDp = 393)
annotation class ComponentPreviews

/** Preview o'rami: mavzu (tizim kun/tun rejimidan), Fmt so'zlari va aurora fon. */
@Composable
fun PreviewSurface(tone: Tone? = null, content: @Composable () -> Unit) {
    val res = LocalContext.current.resources
    Fmt.text = fmtTextFrom(res)
    OrvellTheme(ThemeMode.SYSTEM) {
        AuroraBackground(tone) { Box { content() } }
    }
}
