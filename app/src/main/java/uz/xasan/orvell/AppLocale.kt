package uz.xasan.orvell

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import uz.xasan.orvell.data.SettingsRepo
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.fmtTextFrom
import java.util.Locale

/**
 * Ilova tili (uz/en/ru), tizim tilidan mustaqil.
 * - Android 13+ (API 33): tizimning per-app tili (LocaleManager) — tizim sozlamalarida ham ko'rinadi.
 * - Android 9–12 (API 28–32): Activity/Service konteksti tanlangan til bilan o'raladi, Activity qayta yaratiladi.
 * Tanlov sozlamalarda ham saqlanadi (ikkala yo'l bir xil manbadan o'qisin).
 */
object AppLocale {
    val supported = listOf("uz", "en", "ru")
    const val DEFAULT = "uz"

    fun normalize(tag: String?): String = tag?.substringBefore('-')?.lowercase()?.takeIf { it in supported } ?: DEFAULT

    /** attachBaseContext uchun: API 28–32 da tanlangan tilga o'raydi, 33+ da tizim o'zi qo'llaydi. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val locale = Locale.forLanguageTag(SettingsRepo.storedLanguage(base))
        Locale.setDefault(locale)  // DatePicker va boshqa tizim formatlari ham shu tilda bo'lsin
        val cfg = Configuration(base.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) }
        return base.createConfigurationContext(cfg)
    }

    /** Til tanlanganda: saqlangan qiymat yangilangandan keyin chaqiriladi. */
    fun apply(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            // tizim tilni saqlaydi va Activity'ni o'zi qayta yaratadi
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            activity.recreate()
        }
    }

    /**
     * Ilova ochilganda (API 33+): tizimdagi per-app til bilan sozlamani moslash.
     * Foydalanuvchi tilni tizim sozlamalarida o'zgartirgan bo'lsa — sozlamaga yozamiz;
     * hali hech narsa tanlanmagan bo'lsa — saqlangan tilni (sukut bo'yicha o'zbekcha) tizimga beramiz
     * (tizim Activity'ni o'sha tilda qayta yaratadi).
     */
    fun syncOnStart(activity: Activity) {
        if (Build.VERSION.SDK_INT < 33) return
        val app = OrvellApp.get(activity)
        val lm = activity.getSystemService(LocaleManager::class.java)
        val system = lm.applicationLocales
        if (system.isEmpty) {
            lm.applicationLocales = LocaleList.forLanguageTags(app.settings.value.language)
        } else {
            val tag = normalize(system[0].language)
            if (tag != app.settings.value.language) app.settings.update { it.copy(language = tag) }
        }
    }

    /** Fmt so'zlarini berilgan kontekst tiliga o'rnatadi. */
    fun updateFmt(ctx: Context) { Fmt.text = fmtTextFrom(ctx.resources) }

    /** Ilova konteksti uchun (bildirishnomalar): tanlangan tildagi resurslar. */
    fun localized(ctx: Context): Context = wrap(ctx)
}

/** Compose LocalContext'dan Activity'ni topadi (ContextWrapper zanjiri orqali). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
