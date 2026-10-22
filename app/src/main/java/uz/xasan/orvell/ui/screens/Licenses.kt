package uz.xasan.orvell.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.xasan.orvell.R
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.SecondaryButton
import uz.xasan.orvell.ui.components.SettingRow
import uz.xasan.orvell.ui.components.SettingsGroup
import uz.xasan.orvell.ui.components.TopBar
import uz.xasan.orvell.ui.openUrl
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Tones

/** Ilovada haqiqatda ishlatilgan kutubxonalar (app/build.gradle.kts bilan mos tutilsin). */
private data class Library(val name: String, val artifact: String, val url: String)

/** Ilovaga qo'shilgan shrift (res/font) — SIL OFL 1.1, litsenziya matni res/raw/manrope_ofl.txt. */
private const val MANROPE_URL = "https://github.com/googlefonts/manrope"

private val libraries = listOf(
    Library("Jetpack Compose", "androidx.compose.ui · foundation · animation", "https://developer.android.com/jetpack/androidx/releases/compose"),
    Library("Compose Material 3", "androidx.compose.material3:material3", "https://developer.android.com/jetpack/androidx/releases/compose-material3"),
    Library("Material Icons Extended", "androidx.compose.material:material-icons-extended", "https://developer.android.com/jetpack/androidx/releases/compose-material"),
    Library("AndroidX Activity", "androidx.activity:activity-compose", "https://developer.android.com/jetpack/androidx/releases/activity"),
    Library("AndroidX Navigation", "androidx.navigation:navigation-compose", "https://developer.android.com/jetpack/androidx/releases/navigation"),
    Library("AndroidX Lifecycle", "androidx.lifecycle:lifecycle-runtime-compose · viewmodel-compose · service", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    Library("AndroidX Core", "androidx.core:core-ktx", "https://developer.android.com/jetpack/androidx/releases/core"),
    Library("AndroidX Core SplashScreen", "androidx.core:core-splashscreen", "https://developer.android.com/jetpack/androidx/releases/core"),
    Library("Kotlin Standard Library", "org.jetbrains.kotlin:kotlin-stdlib", "https://kotlinlang.org"),
    Library("kotlinx.coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-android", "https://github.com/Kotlin/kotlinx.coroutines"),
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var showText by rememberSaveable { mutableStateOf(false) }
    var showOfl by rememberSaveable { mutableStateOf(false) }
    // Litsenziya matnlari ilova ichida (Apache 2.0 va OFL nusxasini berishni talab qiladi), tarmoq kerak emas
    val licenseText = remember { runCatching { ctx.resources.openRawResource(R.raw.apache_license_2_0).bufferedReader().use { it.readText() } }.getOrDefault("") }
    val oflText = remember { runCatching { ctx.resources.openRawResource(R.raw.manrope_ofl).bufferedReader().use { it.readText() } }.getOrDefault("") }
    val mono = OType.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.licenses_title), onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OCard(padding = 18.dp) { Text(stringResource(R.string.licenses_intro), style = OType.body, color = O.c.text2) }
            }
            item {
                SettingsGroup(stringResource(R.string.licenses_group), stringResource(R.string.licenses_footer)) {
                    libraries.forEach { lib ->
                        item { SettingRow(Icons.Rounded.Code, Tones.device, lib.name, lib.artifact, value = "Apache 2.0", onClick = { ctx.openUrl(lib.url) }) }
                    }
                }
            }
            item {
                SecondaryButton(stringResource(if (showText) R.string.licenses_hide_text else R.string.licenses_show_text), { showText = !showText })
            }
            if (showText) item {
                OCard(padding = 16.dp) { Text(licenseText, style = mono, color = O.c.text2) }
            }
            item {
                SettingsGroup(stringResource(R.string.licenses_fonts)) {
                    item { SettingRow(Icons.Rounded.TextFields, Tones.sleep, "Manrope", stringResource(R.string.licenses_font_sub), value = "OFL 1.1", onClick = { ctx.openUrl(MANROPE_URL) }) }
                }
            }
            item {
                SecondaryButton(stringResource(if (showOfl) R.string.licenses_hide_text else R.string.licenses_show_ofl), { showOfl = !showOfl })
            }
            if (showOfl) item {
                OCard(padding = 16.dp) { Text(oflText, style = mono, color = O.c.text2) }
            }
        }
    }
}

@uz.xasan.orvell.ui.preview.OrvellPreviews
@Composable
private fun LicensesPreview() = uz.xasan.orvell.ui.preview.PreviewSurface { LicensesScreen {} }
