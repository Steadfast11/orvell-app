package uz.xasan.orvell.ui.screens

import uz.xasan.orvell.ui.preview.PreviewSurface
import uz.xasan.orvell.ui.preview.PreviewData
import uz.xasan.orvell.ui.preview.ComponentPreviews
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import uz.xasan.orvell.ui.components.Art
import uz.xasan.orvell.ui.components.Illustration
import uz.xasan.orvell.ui.components.OrbitRings
import uz.xasan.orvell.ui.components.Ring
import uz.xasan.orvell.ui.components.WatchRender
import uz.xasan.orvell.ui.components.rememberHaptics
import uz.xasan.orvell.ui.components.rememberUndoable
import uz.xasan.orvell.ui.components.staggered
import uz.xasan.orvell.ui.theme.Space
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.xasan.orvell.Perms
import uz.xasan.orvell.R
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.ConfirmDialog
import uz.xasan.orvell.ui.components.EmptyState
import uz.xasan.orvell.ui.components.IconBadge
import uz.xasan.orvell.ui.components.LargeTitle
import uz.xasan.orvell.ui.components.LocalAppScope
import uz.xasan.orvell.ui.components.LocalSnackbar
import uz.xasan.orvell.ui.components.OCard
import uz.xasan.orvell.ui.components.Overline
import uz.xasan.orvell.ui.components.Pill
import uz.xasan.orvell.ui.components.PrimaryButton
import uz.xasan.orvell.ui.components.ProgressLine
import uz.xasan.orvell.ui.components.SecondaryButton
import uz.xasan.orvell.ui.components.SettingRow
import uz.xasan.orvell.ui.components.SettingsGroup
import uz.xasan.orvell.ui.components.TopBar
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.Tones
import uz.xasan.orvell.watch.Conn
import uz.xasan.orvell.watch.FoundWatch
import uz.xasan.orvell.watch.WatchState
import kotlin.math.cos
import kotlin.math.sin

// ================================================================ ruxsatlar

fun btPermissions(): Array<String> = Perms.bluetoothScan()

fun hasPermissions(ctx: Context, perms: Array<String>) = Perms.has(ctx, perms)

fun bluetoothEnabled(ctx: Context) = Perms.bluetoothOn(ctx)

/** Bluetooth'ni yoqish oynasi (Android 12+ da BLUETOOTH_CONNECT ruxsati bo'lsa — canConnect tekshiradi). */
@android.annotation.SuppressLint("MissingPermission")
private fun enableBluetoothIntent(ctx: Context): Intent? =
    if (Perms.canConnect(ctx)) Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE) else null

// ================================================================ 3D soat tasviri

/** Canvas'da chizilgan soat: metall korpus, tasma, yaltiroq ekran va ekranda vaqt + batareya halqasi. */
@Composable
fun WatchIllustration(modifier: Modifier = Modifier, battery: Int = -1, glowing: Boolean = true, size: Dp = 128.dp, stateColor: Color = Tones.oxygen.a) =
    WatchRender(modifier, battery = battery, glowing = glowing, size = size, stateColor = stateColor)

// ================================================================ Qurilma tabi

/**
 * Sinxron tugmasi amali (Bugun va Qurilma ekranlari bir xil ishlatadi): soat ulangan bo'lsa tarix sinxroni boshlanadi,
 * aks holda qisqa xabar chiqadi — tugma hech qachon "jim" qolmaydi.
 */
@Composable
fun rememberSyncAction(vm: AppViewModel): () -> Unit {
    val host = LocalSnackbar.current
    val scope = LocalAppScope.current ?: rememberCoroutineScope()
    val needConnection = stringResource(R.string.device_sync_need_connection)
    return remember(vm, host, scope, needConnection) {
        {
            if (vm.watch.value.conn == Conn.CONNECTED) vm.syncNow()
            else scope.launch {
                host.currentSnackbarData?.dismiss()
                host.showSnackbar(needConnection)
            }
        }
    }
}

@Composable
fun DeviceScreen(vm: AppViewModel, openPair: () -> Unit, openSettings: () -> Unit) {
    val ctx = LocalContext.current
    val w by vm.watch.collectAsState()
    val s by vm.settings.collectAsState()
    val historyTotal by vm.historyTotal.collectAsState()
    val sync = rememberSyncAction(vm)
    var unpairDialog by remember { mutableStateOf(false) }
    // Ruxsat ish vaqtida olib qo'yilishi yoki tizim sozlamasida qaytarilishi mumkin: davriy tekshiramiz
    var canConnect by remember { mutableStateOf(Perms.canConnect(ctx)) }
    var askedPerm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { canConnect = Perms.canConnect(ctx); delay(2000) } }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askedPerm = true
        canConnect = Perms.canConnect(ctx)
        if (canConnect) vm.startService()
    }
    val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    // Qayta ulash: ruxsat yo'q bo'lsa ruxsat so'raydi, Bluetooth o'chiq bo'lsa yoqish oynasini ochadi, aks holda ulanadi
    val reconnect: () -> Unit = {
        when {
            !canConnect && !askedPerm -> permLauncher.launch(Perms.bluetoothScan())
            !canConnect -> runCatching { ctx.startActivity(Perms.appSettingsIntent(ctx)) }
            w.conn == Conn.BLUETOOTH_OFF -> enableBluetoothIntent(ctx)?.let { runCatching { btLauncher.launch(it) } }
            else -> vm.reconnect()
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LargeTitle(stringResource(R.string.device_title), if (s.watch.paired) stringResource(R.string.device_your_watch) else null) }
        if (!s.watch.paired) {
            item {
                OCard(Modifier.staggered(1), glow = Tones.oxygen.a) {
                    EmptyState(Icons.Rounded.Bluetooth, stringResource(R.string.device_not_paired), stringResource(R.string.device_not_paired_body), Tones.oxygen,
                        stringResource(R.string.device_pair), openPair, art = { Illustration(Art.NO_WATCH, size = 170.dp) })
                }
            }
            return@LazyColumn
        }
        if (!canConnect) item {
            OCard(glow = O.c.danger) {
                Text(stringResource(R.string.device_perm_banner), style = OType.bodyStrong, color = O.c.text)
                if (askedPerm) Text(stringResource(R.string.pair_perm_denied), style = OType.caption, color = O.c.text2, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                if (!askedPerm) PrimaryButton(stringResource(R.string.common_grant), { permLauncher.launch(Perms.bluetoothScan()) }, Modifier.fillMaxWidth(), tone = Tones.oxygen)
                else SecondaryButton(stringResource(R.string.device_open_app_settings), { runCatching { ctx.startActivity(Perms.appSettingsIntent(ctx)) } }, Modifier.fillMaxWidth())
            }
        } else if (w.conn == Conn.BLUETOOTH_OFF) item {
            OCard(glow = O.c.warning) {
                Text(stringResource(R.string.device_bt_off_banner), style = OType.bodyStrong, color = O.c.text)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.device_bt_turn_on), { enableBluetoothIntent(ctx)?.let { runCatching { btLauncher.launch(it) } } },
                    Modifier.fillMaxWidth(), tone = Tones.oxygen, icon = Icons.Rounded.Bluetooth)
            }
        }
        item { WatchHero(w, s.watch.name ?: stringResource(R.string.device_watch_default), sync, reconnect, modifier = Modifier.staggered(1), historyTotal = historyTotal) }
        if (w.error != null) item {
            OCard(glow = O.c.danger) {
                Text(stringResource(R.string.device_error_title), style = OType.headline, color = O.c.danger)
                Text(w.error ?: "", style = OType.caption, color = O.c.text2)
                Text(stringResource(R.string.device_error_hint), style = OType.caption, color = O.c.text3, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_group_watch)) {
                item { SettingRow(Icons.Rounded.Schedule, Tones.device, stringResource(R.string.device_time), stringResource(R.string.device_time_sub)) }
                item { SettingRow(Icons.Rounded.Bluetooth, Tones.oxygen, stringResource(R.string.device_bt_address), value = s.watch.address ?: "—") }
                if (s.developer) item { SettingRow(Icons.Rounded.Badge, Tones.device, stringResource(R.string.device_bind_id), value = s.watch.bindId) }
                item { SettingRow(Icons.Rounded.Settings, Tones.device, stringResource(R.string.device_app_settings), onClick = openSettings) }
            }
        }
        item {
            SettingsGroup {
                item { SettingRow(Icons.Rounded.LinkOff, Tones.danger, stringResource(R.string.device_unpair), stringResource(R.string.device_unpair_sub), danger = true, onClick = { unpairDialog = true }) }
            }
        }
    }

    val undoable = rememberUndoable()
    val pending = stringResource(R.string.device_unpair_pending)
    val haptics = rememberHaptics()
    if (unpairDialog) ConfirmDialog(stringResource(R.string.device_unpair_title), stringResource(R.string.device_unpair_body),
        stringResource(R.string.device_unpair_confirm), danger = true, onConfirm = { haptics.reject(); undoable(pending) { vm.unpair() } }, onDismiss = { unpairDialog = false })
}

@Composable
private fun stateOf(w: WatchState): Pair<Color, String> {
    val c = O.c
    return when (w.conn) {
        Conn.CONNECTED -> c.success to stringResource(R.string.device_state_connected)
        Conn.CONNECTING -> c.warning to stringResource(R.string.device_state_connecting)
        Conn.CONFIRM_ON_WATCH -> c.warning to stringResource(R.string.device_state_confirm)
        Conn.BLUETOOTH_OFF -> c.danger to stringResource(R.string.device_state_bt_off)
        else -> c.text3 to stringResource(R.string.device_state_disconnected)
    }
}

/** Soat "vitrinasi": katta premium tasvir (nur — ulanish rangida), holat, batareya halqasi, sinx va amallar. */
@Composable
private fun WatchHero(w: WatchState, name: String, onSync: () -> Unit, onReconnect: () -> Unit, modifier: Modifier = Modifier, historyTotal: Long = 0) {
    val c = O.c
    val (stateColor, stateText) = stateOf(w)
    val battTone = if (w.battery <= 15) Tones.danger else if (w.battery <= 30) Tones.pressure else Tones.activity
    OCard(modifier, glow = Tones.oxygen.a) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WatchRender(battery = w.battery, stateColor = stateColor, size = 150.dp,
                contentDescription = stringResource(R.string.device_watch_cd, stateText, if (w.battery >= 0) "${w.battery}%" else "—"))
        }
        Spacer(Modifier.height(Space.m))
        Text(name, style = OType.title, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Space.s))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Pill(stateText, stateColor)
            if (w.charging) Pill(stringResource(R.string.device_charging), Tones.activity.ink, icon = Icons.Rounded.BatteryChargingFull)
        }
        // batareya faqat soatdan haqiqiy qiymat kelsa ko'rsatiladi (hozircha protokolda tasdiqlanmagan)
        if (w.battery >= 0) {
            Spacer(Modifier.height(Space.l))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrbitRings(listOf(Ring(w.battery / 100f, battTone)), Modifier.size(52.dp), stroke = 6.dp, gap = 0.dp)
                Spacer(Modifier.width(Space.m))
                Text(stringResource(R.string.device_battery, w.battery), style = OType.bodyStrong, color = c.text, modifier = Modifier.weight(1f))
            }
        }
        // oxirgi sinxron doim ko'rinadi: tarix sinxroni vaqti va yangi yozuvlar soni (qiymatlari hali tahlil qilinmagan)
        val syncLine = if (w.historyAt > 0) stringResource(R.string.device_synced_new, Fmt.ago(w.historyAt), w.historyNew) else stringResource(R.string.device_not_synced)
        Text(syncLine, style = OType.caption, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = Space.s))
        if (historyTotal > 0) Text(stringResource(R.string.device_history_total, historyTotal), style = OType.caption, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Space.l))
        // uzun matnli tillarda (ruscha) bir qatorga sig'masligi mumkin: tugmalar ustma-ust, to'liq kenglikda
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // soat ulanmagan bo'lsa ham bosiladi: onSync qisqa xabar ko'rsatadi
            PrimaryButton(
                stringResource(if (w.syncing) R.string.device_syncing else R.string.device_sync), onSync, Modifier.fillMaxWidth(),
                enabled = !w.syncing, tone = Tones.oxygen, icon = Icons.Rounded.Sync,
            )
            SecondaryButton(stringResource(R.string.device_reconnect), onReconnect, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
        }
    }
}

// ================================================================ soat ulash

@Composable
fun PairScreen(vm: AppViewModel, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.pair_title), onDone)
        Box(Modifier.padding(horizontal = 16.dp)) { PairContent(vm, onDone, skip = null) }
    }
}

/** Qidiruv → tanlash → soatda tasdiqlash → tayyor. Onboarding'da ham ishlatiladi. */
@Composable
fun PairContent(vm: AppViewModel, onPaired: () -> Unit, skip: (() -> Unit)?) {
    val ctx = LocalContext.current
    val w by vm.watch.collectAsState()
    var granted by remember { mutableStateOf(hasPermissions(ctx, btPermissions())) }
    var asked by remember { mutableStateOf(false) }
    var btOn by remember { mutableStateOf(bluetoothEnabled(ctx)) }
    var locOn by remember { mutableStateOf(!Perms.needsLocationServices() || Perms.locationOn(ctx)) }
    var chosen by remember { mutableStateOf<FoundWatch?>(null) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { asked = true; granted = hasPermissions(ctx, btPermissions()) }
    val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { btOn = bluetoothEnabled(ctx) }
    // tizim sozlamalaridan qaytganda ham holat yangilansin (ruxsat, Bluetooth, joylashuv)
    LaunchedEffect(Unit) {
        while (true) {
            granted = hasPermissions(ctx, btPermissions())
            btOn = bluetoothEnabled(ctx)
            locOn = !Perms.needsLocationServices() || Perms.locationOn(ctx)
            delay(1500)
        }
    }

    val step = when {
        !granted -> 0
        !btOn -> 1
        !locOn -> 2
        chosen == null -> 3
        w.conn == Conn.CONNECTED -> 5
        else -> 4
    }
    AnimatedContent(step, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "pair") { st ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (st) {
                0 -> {
                    PairHeader(stringResource(R.string.pair_perm_title), art = true, body =
                        stringResource(if (Perms.needsLocationServices()) R.string.pair_perm_body_location else R.string.pair_perm_body))
                    PrimaryButton(stringResource(R.string.common_grant), { permLauncher.launch(btPermissions()) }, Modifier.fillMaxWidth())
                    // "Boshqa so'ramang" tanlangan bo'lsa tizim oynasi chiqmaydi: sozlamalarga yo'l ko'rsatamiz
                    if (asked) {
                        Text(stringResource(R.string.pair_perm_denied), style = OType.caption, color = O.c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        SecondaryButton(stringResource(R.string.device_open_app_settings), { runCatching { ctx.startActivity(Perms.appSettingsIntent(ctx)) } }, Modifier.fillMaxWidth())
                    }
                }
                1 -> {
                    PairHeader(stringResource(R.string.pair_bt_title), stringResource(R.string.pair_bt_body), art = true)
                    PrimaryButton(stringResource(R.string.pair_bt_button), { enableBluetoothIntent(ctx)?.let { runCatching { btLauncher.launch(it) } } }, Modifier.fillMaxWidth())
                }
                2 -> {
                    PairHeader(stringResource(R.string.pair_location_title), stringResource(R.string.pair_location_body), art = true)
                    PrimaryButton(stringResource(R.string.pair_location_button), {
                        runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    }, Modifier.fillMaxWidth())
                }
                3 -> ScanList(vm) { chosen = it; vm.pair(it.address, it.name) }
                4 -> {
                    val confirm = w.conn == Conn.CONFIRM_ON_WATCH
                    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                        if (w.error != null) Illustration(Art.ERROR, size = 180.dp)
                        else Radar(if (confirm) Tones.pressure.a else Tones.oxygen.a) { WatchRender(size = 96.dp, glowing = false, stateColor = if (confirm) Tones.pressure.a else Tones.oxygen.a) }
                    }
                    PairHeader(
                        if (confirm) stringResource(R.string.pair_confirm_title) else stringResource(R.string.pair_connecting_title, chosen?.name ?: stringResource(R.string.device_watch_default)),
                        stringResource(if (confirm) R.string.pair_confirm_body else R.string.pair_connecting_body),
                    )
                    if (w.error != null) {
                        Text(w.error ?: "", style = OType.caption, color = O.c.danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.pair_other_app_hint),
                            style = OType.caption, color = O.c.text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    SecondaryButton(stringResource(R.string.pair_choose_other), { vm.unpair(); chosen = null }, Modifier.fillMaxWidth())
                }
                else -> {
                    val haptics = rememberHaptics()
                    LaunchedEffect(Unit) { haptics.celebrate() }
                    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { Illustration(Art.DONE, size = 200.dp) }
                    PairHeader(stringResource(R.string.pair_done_title), stringResource(R.string.pair_done_body))
                    PrimaryButton(stringResource(R.string.common_continue), onPaired, Modifier.fillMaxWidth())
                }
            }
            if (skip != null && st < 5) SecondaryButton(stringResource(R.string.pair_later), skip, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PairHeader(title: String, body: String, art: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // ruxsat / Bluetooth / joylashuv bosqichlarida: soat va telefon bog'lanishi sahnasi
        if (art) { Illustration(Art.PAIR, size = 150.dp); Spacer(Modifier.height(4.dp)) }
        Text(title, style = OType.title, color = O.c.text, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(6.dp))
        Text(body, style = OType.body, color = O.c.text2, textAlign = TextAlign.Center)
    }
}

/**
 * Radar: markazdan tarqaluvchi to'lqinlar, aylanuvchi nur va topilgan qurilmalar nuqtalari (blips).
 * Cho'tkalar keshda; reduce motion bo'lsa harakatsiz.
 */
@Composable
fun Radar(color: Color, size: Dp = 200.dp, blips: Int = 0, content: @Composable () -> Unit = {}) {
    val t = if (O.reduceMotion) remember { mutableFloatStateOf(0.3f) }
        else rememberInfiniteTransition(label = "radar").animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "r")
    Box(Modifier.size(size).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Spacer(Modifier.fillMaxSize().drawWithCache {
            val r = this.size.minDimension / 2
            val beam = Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, color.copy(alpha = 0.35f)), center = this.size.center)
            val blipR = 14.dp.toPx()
            val halo = Brush.radialGradient(listOf(color, Color.Transparent), center = Offset.Zero, radius = blipR)
            val ring = Stroke(2.dp.toPx())
            onDrawBehind {
                for (k in 0..2) {
                    val p = (t.value + k / 3f) % 1f
                    drawCircle(color.copy(alpha = (1f - p) * 0.45f), radius = r * (0.3f + 0.7f * p), style = ring)
                }
                rotate(t.value * 360f) { drawCircle(beam, r, center) }
                for (i in 0 until blips.coerceAtMost(8)) {
                    val a = (i * 2.4f) + 0.6f
                    val d = r * (0.45f + (i % 3) * 0.17f)
                    val p = Offset(center.x + d * cos(a), center.y + d * sin(a))
                    translate(p.x, p.y) { drawCircle(halo, blipR, Offset.Zero) }
                    drawCircle(Color.White, 3.dp.toPx(), p)
                }
            }
        })
        content()
    }
}

@Composable
private fun ScanList(vm: AppViewModel, onPick: (FoundWatch) -> Unit) {
    val found by vm.scanner.found.collectAsState()
    val scanning by vm.scanner.scanning.collectAsState()
    var round by remember { mutableStateOf(0) }
    DisposableEffect(round) {
        vm.scanner.start()
        onDispose { vm.scanner.stop() }
    }
    LaunchedEffect(round) { delay(25_000); vm.scanner.stop() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (!scanning && found.isEmpty()) Illustration(Art.NOT_FOUND, size = 180.dp)
            else Radar(if (scanning) Tones.oxygen.a else O.c.text3, 180.dp, blips = found.size) { IconBadge(Icons.Rounded.Watch, Tones.oxygen, 56.dp, filled = true) }
        }
        PairHeader(stringResource(if (scanning) R.string.scan_searching else R.string.scan_done), stringResource(R.string.scan_hint))
        if (found.isNotEmpty()) {
            Overline(stringResource(R.string.scan_found), modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            OCard(padding = 0.dp, radius = 22.dp) {
                // mos keladigan soatlar birinchi, keyin signal kuchi bo'yicha
                found.sortedWith(compareByDescending<FoundWatch> { it.fitCloud }.thenByDescending { it.rssi }).take(12).forEach { f ->
                    SettingRow(Icons.Rounded.Watch, if (f.fitCloud) Tones.activity else Tones.device, f.name,
                        subtitle = (if (f.fitCloud) stringResource(R.string.scan_compatible) else "") + stringResource(R.string.scan_signal, stringResource(signal(f.rssi))), onClick = { onPick(f) })
                }
            }
        } else if (!scanning) Text(stringResource(R.string.scan_none), style = OType.caption, color = O.c.text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (!scanning) SecondaryButton(stringResource(R.string.scan_again), { round++ }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
    }
}

private fun signal(rssi: Int) = when { rssi > -60 -> R.string.signal_excellent; rssi > -75 -> R.string.signal_good; rssi > -88 -> R.string.signal_fair; else -> R.string.signal_weak }


// ---------------------------------------------------------------- previewlar

@ComponentPreviews
@Composable
private fun WatchHeroPreview() = PreviewSurface(Tones.oxygen) {
    Column(Modifier.padding(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        WatchHero(PreviewData.watch, "GL-SW26", {}, {})
        WatchHero(PreviewData.watch.copy(conn = Conn.DISCONNECTED, battery = 18), "GL-SW26", {}, {})
    }
}

@ComponentPreviews
@Composable
private fun RadarPreview() = PreviewSurface(Tones.oxygen) {
    Box(Modifier.fillMaxWidth().padding(Space.gutter), contentAlignment = Alignment.Center) {
        Radar(Tones.oxygen.a, 180.dp, blips = 3) { IconBadge(Icons.Rounded.Watch, Tones.oxygen, 56.dp, filled = true) }
    }
}
