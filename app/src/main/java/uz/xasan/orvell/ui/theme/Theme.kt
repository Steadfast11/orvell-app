package uz.xasan.orvell.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.xasan.orvell.R

// Dizayn tizimi "Orbit Glass": tokenlar va asoslash — docs/UI-REDESIGN.md

/** Ilova ranglari: fon qatlamlari (bg → surface → surfaceHi → raised), matn darajalari va holat ranglari. */
@Immutable
data class OrvellColors(
    val isDark: Boolean,
    val bg: Color,
    val bgGlow: Color,
    val surface: Color,
    val surfaceHi: Color,
    val raised: Color,
    val surfacePressed: Color,
    val stroke: Color,
    val strokeHi: Color,
    /** "Yorug'lik tepadan": sirtning yuqori qirrasidagi ingichka yorug' chiziq. */
    val highlight: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val onAccent: Color,
    /** To'ldirilgan rangli tugma/badge ustidagi matn (barcha gradientlarda ≥ 6.5:1). */
    val onTone: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
    val track: Color,
    /** Rangli nur kuchi (qorong'ida yorqinroq, yorug'da yumshoq). */
    val glowAlpha: Float,
    /** Yorug' mavzuda kartochka soyasi; qorong'ida 0 (u yerda nur ishlatiladi). */
    val shadow: Color,
)

// Kontrast o'lchangan (WCAG): text3 qorong'ida ≥ 4.9, yorug'da ≥ 4.6 har bir sirtda.
val DarkColors = OrvellColors(
    isDark = true,
    bg = Color(0xFF05060A),
    bgGlow = Color(0xFF151A2E),
    surface = Color(0xFF0E1118),
    surfaceHi = Color(0xFF161A24),
    raised = Color(0xFF1D2230),
    surfacePressed = Color(0xFF222839),
    stroke = Color(0x14FFFFFF),
    strokeHi = Color(0x29FFFFFF),
    highlight = Color(0x24FFFFFF),
    text = Color(0xFFF4F6FA),
    text2 = Color(0xFFAEB6C6),
    text3 = Color(0xFF8890A2),
    accent = Color(0xFFC8F56A),
    onAccent = Color(0xFF0B0F05),
    onTone = Color(0xFF0B0E14),
    danger = Color(0xFFFF6B80),
    warning = Color(0xFFFFB547),
    success = Color(0xFF3BE3A0),
    track = Color(0x14FFFFFF),
    glowAlpha = 0.22f,
    shadow = Color.Transparent,
)

val LightColors = OrvellColors(
    isDark = false,
    bg = Color(0xFFF2F3F7),
    bgGlow = Color(0xFFDDE4FF),
    surface = Color(0xFFFFFFFF),
    surfaceHi = Color(0xFFF7F8FB),
    raised = Color(0xFFEEF0F5),
    surfacePressed = Color(0xFFE6E9F0),
    stroke = Color(0x140B1020),
    strokeHi = Color(0x240B1020),
    highlight = Color(0xCCFFFFFF),
    text = Color(0xFF0A0D13),
    text2 = Color(0xFF4E5768),
    text3 = Color(0xFF646C7F),
    accent = Color(0xFF3B6B00),
    onAccent = Color(0xFFFFFFFF),
    onTone = Color(0xFF0B0E14),
    danger = Color(0xFFC4223D),
    warning = Color(0xFF8F5600),
    success = Color(0xFF0A7A50),
    track = Color(0x140B1020),
    glowAlpha = 0.10f,
    shadow = Color(0x1A0B1020),
)

/**
 * Ma'noli rang: har bir ko'rsatkich o'z "yorug'lik manbai"ga ega.
 * a→b — halqa, ustun va nur uchun gradient; ink — matn uchun (har mavzuda ≥ 4.5:1).
 */
@Immutable
data class Tone(val a: Color, val b: Color, val inkDark: Color = a, val inkLight: Color = a) {
    val brush get() = Brush.linearGradient(listOf(a, b))
    val solid get() = a
    fun soft(alpha: Float = 0.16f) = a.copy(alpha = alpha)

    /** Matn va kichik belgilar uchun o'qiladigan rang (joriy mavzuga qarab). */
    val ink: Color
        @Composable @ReadOnlyComposable get() = if (LocalOrvellColors.current.isDark) inkDark else inkLight
}

object Tones {
    val activity = Tone(Color(0xFFC8F56A), Color(0xFF3BE3A0), Color(0xFFC8F56A), Color(0xFF3B6B00))
    val heart = Tone(Color(0xFFFF5D73), Color(0xFFFF9A62), Color(0xFFFF6B80), Color(0xFFC4223D))
    val sleep = Tone(Color(0xFF9B8CFF), Color(0xFF4F8CFF), Color(0xFFA497FF), Color(0xFF5442D6))
    val oxygen = Tone(Color(0xFF45E3F2), Color(0xFF3B8BFF), Color(0xFF45E3F2), Color(0xFF086A82))
    val pressure = Tone(Color(0xFFFFC15A), Color(0xFFFF7A45), Color(0xFFFFC15A), Color(0xFF945300))
    val calories = Tone(Color(0xFFFFB04A), Color(0xFFFF6B4A), Color(0xFFFFB04A), Color(0xFFA84A0A))
    val distance = Tone(Color(0xFF6CD3FF), Color(0xFF4F8CFF), Color(0xFF6CD3FF), Color(0xFF1A62B3))
    val breath = Tone(Color(0xFF7CF0D0), Color(0xFF3FB7E8), Color(0xFF7CF0D0), Color(0xFF0A6F5E))
    val device = Tone(Color(0xFFB9C2D3), Color(0xFF7D8799), Color(0xFFB9C2D3), Color(0xFF5A6374))
    val brand = Tone(Color(0xFFC8F56A), Color(0xFF45E3F2), Color(0xFFC8F56A), Color(0xFF3B6B00))
    val danger = Tone(Color(0xFFFF5D73), Color(0xFFE5324B), Color(0xFFFF6B80), Color(0xFFC4223D))

    // uyqu bosqichlari
    val deep = Color(0xFF5B4BFF)
    val light = Color(0xFF6FA8FF)
    val rem = Color(0xFFC98BFF)
    val awake = Color(0xFFFFB547)
}

/** Oraliqlar shkalasi: 4 / 8 / 12 / 16 / 24 / 32. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Ekran chetidagi oraliq. */
    val gutter = 16.dp
    /** Kartochka ichidagi oraliq. */
    val card = 20.dp
}

/** Burchaklar: ichki burchak = tashqi − padding (konsentrik). */
object Radius {
    val field = 16.dp
    val row = 20.dp
    val card = 28.dp
    val hero = 32.dp
}

/** Harakat tokenlari (docs/UI-REDESIGN.md §3.4). */
object Motion {
    val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val FILL_MS = 1100
    const val ENTER_MS = 380
    const val STAGGER_MS = 45
    const val CONTAINER_MS = 420
    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = 700f)
    fun <T> settle() = spring<T>(dampingRatio = 0.8f, stiffness = 400f)
    fun <T> soft() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}

// Manrope (OFL-1.1, o'zgaruvchan vazn 200–800, lotin + kirill). Litsenziya: res/raw/manrope_ofl.txt
private val Manrope = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold),
    Font(R.font.manrope_variable, FontWeight.ExtraBold),
)

object OType {
    private const val TNUM = "tnum"
    val hero = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-2.4).sp, fontFeatureSettings = TNUM)
    val display = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1.0).sp, fontFeatureSettings = TNUM)
    val title = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp)
    val headline = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val number = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.8).sp, fontFeatureSettings = TNUM)
    val body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodyStrong = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
    val caption = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp)
    val overline = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp)
}

enum class ThemeMode { SYSTEM, DARK, LIGHT }

val LocalOrvellColors = staticCompositionLocalOf { DarkColors }

/** Tizimda animatsiyalar o'chirilgan (animator scale = 0): cheksiz va bezak animatsiyalar to'xtaydi. */
val LocalReduceMotion = staticCompositionLocalOf { false }

object O {
    val c: OrvellColors @Composable @ReadOnlyComposable get() = LocalOrvellColors.current
    val reduceMotion: Boolean @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

/** Tizimning "animatsiyalarni o'chirish" sozlamasi (Accessibility yoki Developer options). */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}

private val MaterialType = Typography(
    displayLarge = OType.hero, displayMedium = OType.display, displaySmall = OType.display,
    headlineLarge = OType.title, headlineMedium = OType.title, headlineSmall = OType.headline,
    titleLarge = OType.title, titleMedium = OType.headline, titleSmall = OType.bodyStrong,
    bodyLarge = OType.body, bodyMedium = OType.body, bodySmall = OType.caption,
    labelLarge = OType.bodyStrong, labelMedium = OType.label, labelSmall = OType.label,
)

@Composable
fun OrvellTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceHi, onSurfaceVariant = c.text2,
        surfaceContainerHigh = c.raised, surfaceContainerHighest = c.raised, inverseSurface = c.text, inverseOnSurface = c.bg,
        outline = c.strokeHi, error = c.danger,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceHi, onSurfaceVariant = c.text2,
        surfaceContainerHigh = c.raised, surfaceContainerHighest = c.raised, inverseSurface = c.text, inverseOnSurface = c.bg,
        outline = c.strokeHi, error = c.danger,
    )
    val reduce = rememberSystemReduceMotion()
    MaterialTheme(colorScheme = scheme, typography = MaterialType) {
        CompositionLocalProvider(LocalOrvellColors provides c, LocalReduceMotion provides reduce, content = content)
    }
}
