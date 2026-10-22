package uz.xasan.orvell.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import uz.xasan.orvell.AppLocale
import uz.xasan.orvell.ui.theme.ThemeMode
import java.security.SecureRandom
import java.time.LocalDate
import java.time.Period

enum class StorageMode { LOCAL, CLOUD }
enum class Units { METRIC, IMPERIAL }
enum class Sex { MALE, FEMALE, UNSET }

data class Profile(
    val name: String = "",
    val sex: Sex = Sex.UNSET,
    val birthDate: LocalDate? = null,
    val heightCm: Int = 170,
    val weightKg: Float = 65f,
    val avatar: Int = 0,
) {
    val age: Int? get() = birthDate?.let { Period.between(it, LocalDate.now()).years }
    val initials: String get() = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "O" }
    /** Tana massasi indeksi (faqat ma'lumot uchun, tashxis emas). */
    val bmi: Float? get() = if (heightCm in 80..250 && weightKg in 20f..350f) weightKg / ((heightCm / 100f) * (heightCm / 100f)) else null
}

data class Goals(val steps: Int = 8000, val sleepMin: Int = 480, val kcal: Int = 350, val distanceM: Int = 5000)

data class WatchCfg(
    val address: String? = null,
    val name: String? = null,
    /** Soatga beriladigan foydalanuvchi belgisi (bog'lash ID). Har o'rnatishda tasodifiy. */
    val bindId: String = newBindId(),
    val firmware: String? = null,
    /** Oxirgi muvaffaqiyatli tarix sinxroni vaqti (ms), 0 = hali bo'lmagan. Ilova qayta ochilganda tiklanadi. */
    val lastHistoryAt: Long = 0,
) {
    val paired get() = !address.isNullOrBlank()
}

data class AppSettings(
    val onboarded: Boolean = false,
    val storage: StorageMode = StorageMode.LOCAL,
    val profile: Profile = Profile(),
    val goals: Goals = Goals(),
    val units: Units = Units.METRIC,
    val clock24: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "uz",
    val watch: WatchCfg = WatchCfg(),
    val alertLowBattery: Boolean = true,
    val alertDisconnect: Boolean = true,
    val serverEnabled: Boolean = false,
    val serverUrl: String = "",
    val serverToken: String = "",
    val developer: Boolean = false,
)

fun newBindId(): String {
    val b = ByteArray(8).also { SecureRandom().nextBytes(it) }
    return "orvell-" + b.joinToString("") { "%02x".format(it) }
}

/** Saqlangan saqlash joyi. Bulut (CLOUD) hali amalga oshmagan: u ham, eski yoki noma'lum qiymat ham LOCAL o'qiladi. */
internal fun storageModeOf(raw: String?): StorageMode =
    StorageMode.entries.firstOrNull { it.name == raw }?.takeIf { it != StorageMode.CLOUD } ?: StorageMode.LOCAL

/** Sozlamalar: SharedPreferences ichida bitta JSON, o'zgarishlar StateFlow orqali ekranlarga boradi. */
class SettingsRepo(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS = "orvell"

        /** attachBaseContext'da (SettingsRepo hali yaratilmagan bo'lishi mumkin) saqlangan tilni o'qiydi. */
        fun storedLanguage(ctx: Context): String = AppLocale.normalize(runCatching {
            JSONObject(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("settings", null) ?: "{}").optString("language")
        }.getOrNull())
    }
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<AppSettings> = _flow
    val value: AppSettings get() = _flow.value

    @Synchronized
    fun update(f: (AppSettings) -> AppSettings) {
        val next = f(_flow.value)
        prefs.edit().putString("settings", toJson(next).toString()).apply()
        _flow.value = next
    }

    @Synchronized
    fun reset() { prefs.edit().clear().apply(); _flow.value = AppSettings() }

    private fun load(): AppSettings {
        val raw = prefs.getString("settings", null) ?: return AppSettings().also { prefs.edit().putString("settings", toJson(it).toString()).apply() }
        return runCatching { fromJson(JSONObject(raw)) }.getOrElse { AppSettings() }
    }

    private fun toJson(s: AppSettings) = JSONObject().apply {
        put("onboarded", s.onboarded); put("storage", s.storage.name)
        put("profile", JSONObject().apply {
            put("name", s.profile.name); put("sex", s.profile.sex.name); put("birthDate", s.profile.birthDate?.toString() ?: "")
            put("heightCm", s.profile.heightCm); put("weightKg", s.profile.weightKg.toDouble()); put("avatar", s.profile.avatar)
        })
        put("goals", JSONObject().put("steps", s.goals.steps).put("sleepMin", s.goals.sleepMin).put("kcal", s.goals.kcal).put("distanceM", s.goals.distanceM))
        put("units", s.units.name); put("clock24", s.clock24); put("theme", s.theme.name); put("language", s.language)
        put("watch", JSONObject().apply {
            put("address", s.watch.address ?: ""); put("name", s.watch.name ?: ""); put("bindId", s.watch.bindId)
            put("firmware", s.watch.firmware ?: ""); put("lastHistoryAt", s.watch.lastHistoryAt)
        })
        put("alertLowBattery", s.alertLowBattery); put("alertDisconnect", s.alertDisconnect)
        put("serverEnabled", s.serverEnabled); put("serverUrl", s.serverUrl); put("serverToken", s.serverToken); put("developer", s.developer)
    }

    private fun fromJson(j: JSONObject): AppSettings {
        val d = AppSettings()
        val p = j.optJSONObject("profile") ?: JSONObject()
        val g = j.optJSONObject("goals") ?: JSONObject()
        val w = j.optJSONObject("watch") ?: JSONObject()
        return AppSettings(
            onboarded = j.optBoolean("onboarded", false),
            storage = storageModeOf(j.optString("storage")),
            profile = Profile(
                name = p.optString("name", ""),
                sex = runCatching { Sex.valueOf(p.optString("sex")) }.getOrDefault(Sex.UNSET),
                birthDate = p.optString("birthDate").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                heightCm = p.optInt("heightCm", 170), weightKg = p.optDouble("weightKg", 65.0).toFloat(), avatar = p.optInt("avatar", 0),
            ),
            goals = Goals(g.optInt("steps", 8000), g.optInt("sleepMin", 480), g.optInt("kcal", 350), g.optInt("distanceM", 5000)),
            units = runCatching { Units.valueOf(j.optString("units")) }.getOrDefault(d.units),
            clock24 = j.optBoolean("clock24", true),
            theme = runCatching { ThemeMode.valueOf(j.optString("theme")) }.getOrDefault(d.theme),
            language = AppLocale.normalize(j.optString("language")),
            watch = WatchCfg(
                address = w.optString("address").takeIf { it.isNotBlank() },
                name = w.optString("name").takeIf { it.isNotBlank() },
                bindId = w.optString("bindId").takeIf { it.isNotBlank() } ?: newBindId(),
                firmware = w.optString("firmware").takeIf { it.isNotBlank() },
                lastHistoryAt = w.optLong("lastHistoryAt", 0),
            ),
            alertLowBattery = j.optBoolean("alertLowBattery", true),
            alertDisconnect = j.optBoolean("alertDisconnect", true),
            serverEnabled = j.optBoolean("serverEnabled", false),
            serverUrl = j.optString("serverUrl", ""),
            serverToken = j.optString("serverToken", ""),
            developer = j.optBoolean("developer", false),
        )
    }
}
