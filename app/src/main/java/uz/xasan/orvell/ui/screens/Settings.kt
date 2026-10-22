package uz.xasan.orvell.ui.screens

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.xasan.orvell.AppLocale
import uz.xasan.orvell.BuildConfig
import uz.xasan.orvell.Perms
import uz.xasan.orvell.R
import uz.xasan.orvell.data.Units
import uz.xasan.orvell.findActivity
import uz.xasan.orvell.sync.ServerSync
import uz.xasan.orvell.sync.SyncError
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.Art
import uz.xasan.orvell.ui.components.ChoiceDialog
import uz.xasan.orvell.ui.components.ConfirmDialog
import uz.xasan.orvell.ui.components.EmptyState
import uz.xasan.orvell.ui.components.IconBadge
import uz.xasan.orvell.ui.components.Illustration
import uz.xasan.orvell.ui.components.LocalSnackbar
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.OField
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.Pill
import uz.xasan.orvell.ui.components.PrimaryButton
import uz.xasan.orvell.ui.components.SecondaryButton
import uz.xasan.orvell.ui.components.SettingRow
import uz.xasan.orvell.ui.components.SettingsGroup
import uz.xasan.orvell.ui.components.SwitchRow
import uz.xasan.orvell.ui.components.TopBar
import uz.xasan.orvell.ui.components.UiPrefs
import uz.xasan.orvell.ui.components.rememberHaptics
import uz.xasan.orvell.ui.components.rememberUndoable
import uz.xasan.orvell.ui.components.staggered
import uz.xasan.orvell.ui.preview.ComponentPreviews
import uz.xasan.orvell.ui.preview.OrvellPreviews
import uz.xasan.orvell.ui.preview.PreviewSurface
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Space
import uz.xasan.orvell.ui.theme.ThemeMode
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones

/** Til nomlari har doim o'z tilida (foydalanuvchi o'z tilini tanib olsin): values/orvell.xml. */
private val languageNames = listOf("uz" to R.string.lang_uz, "en" to R.string.lang_en, "ru" to R.string.lang_ru)

/** Qidiruv bo'yicha qatorlar: guruh va qator sarlavhalari, izohlari (qiymatlar emas). */
private val searchIds = intArrayOf(
    R.string.settings_group_look, R.string.settings_theme, R.string.settings_language, R.string.settings_units, R.string.settings_clock24,
    R.string.settings_group_notifications, R.string.settings_notifications_blocked,
    R.string.settings_alert_disconnect, R.string.settings_alert_disconnect_sub,
    R.string.settings_system_notifications,
    R.string.settings_group_watch, R.string.settings_background, R.string.settings_background_sub,
    R.string.settings_group_integrations, R.string.settings_own_server, R.string.settings_own_server_sub,
    R.string.settings_group_data, R.string.settings_storage, R.string.settings_storage_local_sub,
    R.string.settings_export, R.string.settings_export_sub, R.string.settings_export_raw, R.string.settings_export_raw_sub,
    R.string.settings_clear, R.string.settings_clear_sub,
    R.string.settings_group_app, R.string.feedback_title, R.string.feedback_subtitle, R.string.settings_privacy, R.string.licenses_title,
    R.string.settings_about, R.string.settings_developer, R.string.settings_developer_sub, R.string.settings_reset, R.string.settings_reset_sub,
)

/** Qidiruv mosligi: so'rov bo'sh bo'lsa hammasi mos; aks holda matnida so'rov bor resurslar. */
@Composable
private fun rememberMatcher(query: String): (Int) -> Boolean {
    val texts = searchIds.map { stringResource(it) }
    return remember(query, texts) {
        val q = query.trim()
        val hits = if (q.isEmpty()) null else searchIds.filterIndexed { i, _ -> texts[i].contains(q, ignoreCase = true) }.toHashSet()
        val match: (Int) -> Boolean = { id -> hits == null || id in hits }
        match
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val host = LocalSnackbar.current
    val haptics = rememberHaptics()
    val undoable = rememberUndoable()
    var dialog by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val m = rememberMatcher(query)
    fun hit(vararg ids: Int) = ids.any(m)
    val exportFailed = stringResource(R.string.settings_export_failed)
    val clearPending = stringResource(R.string.settings_clear_pending)
    // bildirishnomalar tizimda o'chirilgan bo'lsa ogohlantirishlar chiqmaydi — buni ochiq ko'rsatamiz
    var notificationsOn by remember { mutableStateOf(Perms.notificationsOn(ctx)) }
    LaunchedEffect(Unit) { while (true) { notificationsOn = Perms.notificationsOn(ctx); delay(2000) } }
    val openNotificationSettings = {
        runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)) }
        Unit
    }
    val anyHit = query.isBlank() || searchIds.any(m)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings_title), { nav.popBackStack() })
        OField(
            query, { query = it.take(40) }, stringResource(R.string.settings_search),
            Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.s),
            keyboard = KeyboardOptions(imeAction = ImeAction.Search),
            leading = { Icon(Icons.Rounded.Search, null, tint = O.c.text2) },
            trailing = if (query.isNotEmpty()) ({
                IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.cd_clear_search), tint = O.c.text2) }
            }) else null,
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = 40.dp)) {
            if (!anyHit) EmptyState(
                Icons.Rounded.Search, stringResource(R.string.settings_search_empty_t), stringResource(R.string.settings_search_empty, query.trim()),
                art = { Illustration(Art.NOT_FOUND, size = 150.dp) },
            )
            Box(Modifier.staggered(0)) {
                val g = hit(R.string.settings_group_look)
                SettingsGroup(stringResource(R.string.settings_group_look)) {
                    if (g || hit(R.string.settings_theme)) item {
                        SettingRow(Icons.Rounded.DarkMode, Tones.sleep, stringResource(R.string.settings_theme), value = stringResource(when (s.theme) {
                            ThemeMode.SYSTEM -> R.string.settings_theme_system; ThemeMode.DARK -> R.string.settings_theme_dark; ThemeMode.LIGHT -> R.string.settings_theme_light
                        }), onClick = { dialog = "theme" })
                    }
                    if (g || hit(R.string.settings_language)) item {
                        SettingRow(Icons.Rounded.Language, Tones.oxygen, stringResource(R.string.settings_language),
                            value = stringResource(languageNames.firstOrNull { it.first == s.language }?.second ?: languageNames.first().second), onClick = { dialog = "lang" })
                    }
                    if (g || hit(R.string.settings_units)) item {
                        SettingRow(Icons.Rounded.Straighten, Tones.distance, stringResource(R.string.settings_units),
                            value = stringResource(if (s.units == Units.METRIC) R.string.settings_units_metric else R.string.settings_units_imperial), onClick = { dialog = "units" })
                    }
                    if (g || hit(R.string.settings_clock24)) item {
                        SwitchRow(Icons.Rounded.AccessTime, Tones.pressure, stringResource(R.string.settings_clock24), checked = s.clock24) { v -> vm.update { it.copy(clock24 = v) } }
                    }
                }
            }
            Box(Modifier.staggered(1)) {
                val g = hit(R.string.settings_group_notifications)
                SettingsGroup(stringResource(R.string.settings_group_notifications)) {
                    if (!notificationsOn && (g || hit(R.string.settings_notifications_blocked))) item {
                        SettingRow(Icons.Rounded.NotificationsOff, Tones.danger, stringResource(R.string.settings_notifications_blocked), danger = true, onClick = openNotificationSettings)
                    }
                    // batareya ogohlantirishi yo'q: batareya o'qish protokolda tasdiqlanmagan, ogohlantirish hech qachon chiqmasdi
                    if (g || hit(R.string.settings_alert_disconnect, R.string.settings_alert_disconnect_sub)) item {
                        SwitchRow(Icons.Rounded.BluetoothDisabled, Tones.oxygen, stringResource(R.string.settings_alert_disconnect), stringResource(R.string.settings_alert_disconnect_sub), s.alertDisconnect) { v -> vm.update { it.copy(alertDisconnect = v) } }
                    }
                    if (g || hit(R.string.settings_system_notifications)) item {
                        SettingRow(Icons.Rounded.Notifications, Tones.device, stringResource(R.string.settings_system_notifications), onClick = openNotificationSettings)
                    }
                }
            }
            Box(Modifier.staggered(2)) {
                val g = hit(R.string.settings_group_watch)
                SettingsGroup(stringResource(R.string.settings_group_watch)) {
                    if (g || hit(R.string.settings_background, R.string.settings_background_sub)) item {
                        SettingRow(Icons.Rounded.BatterySaver, Tones.activity, stringResource(R.string.settings_background), stringResource(R.string.settings_background_sub),
                            onClick = { runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } })
                    }
                }
            }
            Box(Modifier.staggered(3)) {
                val g = hit(R.string.settings_group_integrations)
                SettingsGroup(stringResource(R.string.settings_group_integrations), stringResource(R.string.settings_integrations_footer).takeIf { query.isBlank() }) {
                    if (g || hit(R.string.settings_own_server, R.string.settings_own_server_sub)) item {
                        SettingRow(Icons.Rounded.Dns, Tones.device, stringResource(R.string.settings_own_server), stringResource(R.string.settings_own_server_sub),
                            value = stringResource(if (s.serverEnabled) R.string.common_on else R.string.common_off), onClick = { nav.navigate("server") })
                    }
                }
            }
            Box(Modifier.staggered(4)) {
                val g = hit(R.string.settings_group_data)
                SettingsGroup(stringResource(R.string.settings_group_data)) {
                    // faqat ma'lumot: bulut (akkaunt) rejimi yo'q, tanlov ham yo'q
                    if (g || hit(R.string.settings_storage, R.string.settings_storage_local_sub)) item {
                        SettingRow(Icons.Rounded.Lock, Tones.activity, stringResource(R.string.settings_storage), stringResource(R.string.settings_storage_local_sub),
                            value = stringResource(R.string.settings_storage_local))
                    }
                    if (g || hit(R.string.settings_export, R.string.settings_export_sub)) item {
                        SettingRow(Icons.Rounded.FileDownload, Tones.oxygen, stringResource(R.string.settings_export), stringResource(R.string.settings_export_sub), onClick = {
                            // eksport xatosi (masalan xotira to'la) ilovani yiqitmasin — qisqa xabar
                            scope.launch {
                                runCatching { ctx.startActivity(vm.exportIntent()) }.onFailure { host.showSnackbar(exportFailed) }
                            }
                        })
                    }
                    if (g || hit(R.string.settings_export_raw, R.string.settings_export_raw_sub)) item {
                        SettingRow(Icons.Rounded.FileDownload, Tones.device, stringResource(R.string.settings_export_raw), stringResource(R.string.settings_export_raw_sub), onClick = {
                            scope.launch {
                                runCatching { ctx.startActivity(vm.exportWatchRawIntent()) }.onFailure { host.showSnackbar(exportFailed) }
                            }
                        })
                    }
                    if (g || hit(R.string.settings_clear, R.string.settings_clear_sub)) item {
                        SettingRow(Icons.Rounded.DeleteForever, Tones.danger, stringResource(R.string.settings_clear), stringResource(R.string.settings_clear_sub), danger = true, onClick = { dialog = "clear" })
                    }
                }
            }
            Box(Modifier.staggered(5)) {
                val g = hit(R.string.settings_group_app)
                SettingsGroup(stringResource(R.string.settings_group_app)) {
                    if (g || hit(R.string.feedback_title, R.string.feedback_subtitle)) item {
                        SettingRow(Icons.Rounded.Feedback, Tones.heart, stringResource(R.string.feedback_title), stringResource(R.string.feedback_subtitle), onClick = { nav.navigate("feedback") })
                    }
                    if (g || hit(R.string.settings_privacy)) item {
                        SettingRow(Icons.Rounded.PrivacyTip, Tones.activity, stringResource(R.string.settings_privacy), onClick = { nav.navigate("privacy") })
                    }
                    if (g || hit(R.string.licenses_title)) item {
                        SettingRow(Icons.Rounded.Gavel, Tones.device, stringResource(R.string.licenses_title), onClick = { nav.navigate("licenses") })
                    }
                    if (g || hit(R.string.settings_about)) item {
                        SettingRow(Icons.Rounded.Info, Tones.oxygen, stringResource(R.string.settings_about), value = BuildConfig.VERSION_NAME, onClick = { nav.navigate("about") })
                    }
                    if (s.developer && (g || hit(R.string.settings_developer, R.string.settings_developer_sub))) item {
                        SwitchRow(Icons.Rounded.Code, Tones.device, stringResource(R.string.settings_developer), stringResource(R.string.settings_developer_sub), true) { v -> vm.update { it.copy(developer = v) } }
                    }
                    if (g || hit(R.string.settings_reset, R.string.settings_reset_sub)) item {
                        SettingRow(Icons.Rounded.RestartAlt, Tones.danger, stringResource(R.string.settings_reset), stringResource(R.string.settings_reset_sub), danger = true, onClick = { dialog = "reset" })
                    }
                }
            }
        }
    }

    when (dialog) {
        "theme" -> ChoiceDialog(stringResource(R.string.settings_theme), listOf(
            ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system_long), ThemeMode.DARK to stringResource(R.string.settings_theme_dark), ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
        ), s.theme, { v -> vm.update { it.copy(theme = v) } }, { dialog = null })
        "units" -> ChoiceDialog(stringResource(R.string.settings_units), listOf(
            Units.METRIC to stringResource(R.string.settings_units_metric_long), Units.IMPERIAL to stringResource(R.string.settings_units_imperial_long),
        ), s.units, { v -> vm.update { it.copy(units = v) } }, { dialog = null })
        "lang" -> ChoiceDialog(stringResource(R.string.settings_language), languageNames.map { it.first to stringResource(it.second) }, s.language, { v ->
            if (v != s.language) {
                vm.update { it.copy(language = v) }  // avval saqlaymiz: qayta yaratilgan Activity shu tilni o'qiydi
                ctx.findActivity()?.let { AppLocale.apply(it, v) }
            }
        }, { dialog = null })
        // o'chirish snackbar yopilgach bajariladi — "Qaytarish" bosilsa hech narsa o'chmaydi
        "clear" -> ConfirmDialog(stringResource(R.string.settings_clear_title), stringResource(R.string.settings_clear_body),
            stringResource(R.string.settings_clear_confirm), danger = true, onConfirm = { haptics.reject(); undoable(clearPending) { vm.clearData() } }, onDismiss = { dialog = null })
        // to'liq tiklash: maslahatlar (coach mark) ham qaytadan ko'rinadi
        "reset" -> ConfirmDialog(stringResource(R.string.settings_reset_title), stringResource(R.string.settings_reset_body),
            stringResource(R.string.settings_reset_confirm), danger = true, onConfirm = { haptics.reject(); UiPrefs.reset(ctx); vm.resetApp() }, onDismiss = { dialog = null })
    }
}

@Composable
fun AboutScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    AboutContent(s.developer, onBack) { vm.update { it.copy(developer = true) } }
}

@Composable
private fun AboutContent(developer: Boolean, onBack: () -> Unit, enableDeveloper: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    val haptics = rememberHaptics()
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings_about), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
                // jonli orbita belgisi (reduce motion'da harakatsiz)
                Box(Modifier.staggered(0)) { Illustration(Art.WELCOME, size = 200.dp) }
                Text(stringResource(R.string.app_name), style = OType.display, color = O.c.text, modifier = Modifier.staggered(1).semantics { heading() })
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = OType.caption, color = O.c.text2,
                    modifier = Modifier.staggered(1).clickable(remember { MutableInteractionSource() }, null) {
                        taps++
                        if (taps >= 7 && !developer) { haptics.confirm(); enableDeveloper() }
                    }.padding(Space.s),
                )
                Text(stringResource(R.string.about_tagline), style = OType.bodyStrong, color = O.c.text2, textAlign = TextAlign.Center, modifier = Modifier.staggered(2))
                if (developer) Pill(stringResource(R.string.about_developer_on), O.c.warning, Modifier.padding(top = 10.dp))
            }
            Box(Modifier.staggered(3)) {
                SettingsGroup {
                    item { SettingRow(Icons.Rounded.Gavel, Tones.device, stringResource(R.string.about_license), stringResource(R.string.about_license_sub), value = stringResource(R.string.about_license_value)) }
                }
            }
            OCard(Modifier.staggered(4)) {
                Overline(stringResource(R.string.about_thanks))
                Spacer(Modifier.height(Space.s))
                Text(stringResource(R.string.about_thanks_text), style = OType.caption, color = O.c.text2)
                Spacer(Modifier.height(Space.m))
                Overline(stringResource(R.string.about_author))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_author_name), style = OType.bodyStrong, color = O.c.text)
            }
        }
    }
}

private class PrivacySection(val icon: ImageVector, val tone: Tone, val title: Int)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val sections = listOf(
        PrivacySection(Icons.Rounded.Shield, Tones.activity, R.string.privacy_short_t) to stringResource(R.string.privacy_short),
        PrivacySection(Icons.Rounded.Visibility, Tones.oxygen, R.string.privacy_collect_t) to stringResource(R.string.privacy_collect),
        PrivacySection(Icons.Rounded.PhoneAndroid, Tones.distance, R.string.privacy_store_t) to stringResource(R.string.privacy_store),
        PrivacySection(Icons.Rounded.Share, Tones.sleep, R.string.privacy_share_t) to stringResource(R.string.privacy_share),
        PrivacySection(Icons.Rounded.Key, Tones.pressure, R.string.privacy_permissions_t) to stringResource(R.string.privacy_permissions),
        PrivacySection(Icons.Rounded.Tune, Tones.breath, R.string.privacy_control_t) to stringResource(R.string.privacy_control),
        PrivacySection(Icons.Rounded.MedicalServices, Tones.heart, R.string.privacy_medical_t) to stringResource(R.string.privacy_medical),
        PrivacySection(Icons.Rounded.ChildCare, Tones.calories, R.string.privacy_children_t) to stringResource(R.string.privacy_children),
        // loyiha aloqa manzili hali yo'q: ilova ichidagi "Taklif yuborish"ga yo'naltiriladi
        PrivacySection(Icons.Rounded.Mail, Tones.device, R.string.privacy_contact_t) to stringResource(R.string.privacy_contact),
    )
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings_privacy), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            sections.forEachIndexed { i, (sec, body) ->
                // birinchi bo'lim — qisqa xulosa: ajratib ko'rsatiladi
                OCard(Modifier.staggered(i), glow = if (i == 0) sec.tone.a else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(sec.icon, sec.tone, 36.dp, filled = i == 0)
                        Spacer(Modifier.width(Space.m))
                        Text(stringResource(sec.title), style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f).semantics { heading() })
                    }
                    Spacer(Modifier.height(Space.s))
                    Text(body, style = if (i == 0) OType.bodyStrong else OType.body, color = if (i == 0) O.c.text else O.c.text2)
                }
            }
            Text(stringResource(R.string.privacy_updated), style = OType.caption, color = O.c.text2, modifier = Modifier.padding(Space.s))
        }
    }
}

/** Sinx xatosi joriy tilda. */
@Composable
fun syncErrorText(e: SyncError): String = when (e) {
    SyncError.HttpsOnly -> stringResource(R.string.server_err_https)
    is SyncError.BadToken -> stringResource(R.string.server_err_token, e.code)
    is SyncError.Http -> stringResource(R.string.server_err_http, e.code, e.body)
    is SyncError.Network -> stringResource(R.string.server_err_network, e.detail)
}

@Composable
fun ServerScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val st by ServerSync.status.collectAsState()
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var url by remember { mutableStateOf(s.serverUrl) }
    var token by remember { mutableStateOf(s.serverToken) }
    var tested by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<SyncError?>(null) }
    var testing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Long?>(null) }
    var rejected by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(st) { pending = vm.pendingUploads(); rejected = vm.rejectedUploads() }
    val filled = url.isNotBlank() && token.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.server_title), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            OCard(Modifier.staggered(0), glow = Tones.oxygen.a) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Dns, Tones.oxygen, 44.dp, filled = s.serverEnabled)
                    Spacer(Modifier.width(Space.m))
                    Text(stringResource(R.string.server_intro_t), style = OType.headline, color = O.c.text, modifier = Modifier.weight(1f).semantics { heading() })
                }
                Spacer(Modifier.height(Space.s))
                Text(stringResource(R.string.server_intro), style = OType.caption, color = O.c.text2)
            }
            Column(Modifier.staggered(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OField(url, { url = it; tested = false }, stringResource(if (BuildConfig.DEBUG) R.string.server_url_hint_debug else R.string.server_url_hint))
                OField(token, { token = it; tested = false }, stringResource(R.string.server_token), isPassword = true)
                if (tested) {
                    val e = testError
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (e == null) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null, tint = if (e == null) O.c.success else O.c.danger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (e == null) stringResource(R.string.server_test_ok) else syncErrorText(e), style = OType.caption, color = if (e == null) O.c.success else O.c.danger)
                    }
                }
                // tugmalar ustma-ust, to'liq kenglikda (ruscha uzun matn sig'sin)
                PrimaryButton(stringResource(R.string.server_save), {
                    haptics.confirm()
                    vm.update { it.copy(serverUrl = url.trim(), serverToken = token.trim(), serverEnabled = true) }
                    vm.pushNow()
                }, Modifier.fillMaxWidth(), enabled = filled, tone = Tones.oxygen)
                SecondaryButton(stringResource(if (testing) R.string.server_testing else R.string.server_test), {
                    scope.launch {
                        testing = true
                        testError = vm.testServer(url, token)
                        tested = true; testing = false
                        if (testError == null) haptics.confirm() else haptics.reject()
                    }
                }, Modifier.fillMaxWidth(), enabled = filled && !testing)
            }
            Box(Modifier.staggered(2)) {
                SettingsGroup(stringResource(R.string.server_status)) {
                    item { SwitchRow(Icons.Rounded.Dns, Tones.device, stringResource(R.string.server_enabled), checked = s.serverEnabled, enabled = s.serverUrl.isNotBlank() && s.serverToken.isNotBlank()) { v ->
                        vm.update { it.copy(serverEnabled = v) }; if (v) vm.pushNow()
                    } }
                    item { SettingRow(Icons.Rounded.AccessTime, Tones.oxygen, stringResource(R.string.server_last), value = if (st.lastOk > 0) Fmt.ago(st.lastOk) else stringResource(R.string.server_last_none)) }
                    item { SettingRow(Icons.Rounded.FileDownload, Tones.activity, stringResource(R.string.server_pending), value = pending?.let { Fmt.int(it.toInt()) } ?: "…") }
                    item { SettingRow(Icons.Rounded.Block, Tones.danger, stringResource(R.string.server_rejected), value = rejected?.let { Fmt.int(it.toInt()) } ?: "…") }
                    st.lastError?.let { e -> item { SettingRow(Icons.Rounded.Info, Tones.danger, stringResource(R.string.server_last_error), syncErrorText(e), danger = true) } }
                    // yuborish o'chiq bo'lsa "Hozir yuborish" ko'rsatilmaydi (bosilganda hech narsa qilmasdi)
                    if (s.serverEnabled) item { SettingRow(Icons.Rounded.RestartAlt, Tones.device, stringResource(if (st.running) R.string.server_sending else R.string.server_send_now),
                        onClick = if (!st.running) ({ vm.pushNow() }) else null) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- previewlar

@OrvellPreviews
@Composable
private fun AboutPreview() = PreviewSurface(Tones.brand) { AboutContent(developer = false, onBack = {}) {} }

@OrvellPreviews
@Composable
private fun PrivacyPreview() = PreviewSurface(Tones.activity) { PrivacyScreen {} }

@ComponentPreviews
@Composable
private fun SettingsRowsPreview() = PreviewSurface {
    Column(Modifier.padding(Space.gutter)) {
        SettingsGroup(stringResource(R.string.settings_group_data)) {
            item { SettingRow(Icons.Rounded.FileDownload, Tones.oxygen, stringResource(R.string.settings_export), stringResource(R.string.settings_export_sub), onClick = {}) }
            item { SettingRow(Icons.Rounded.DeleteForever, Tones.danger, stringResource(R.string.settings_clear), stringResource(R.string.settings_clear_sub), danger = true, onClick = {}) }
        }
    }
}
