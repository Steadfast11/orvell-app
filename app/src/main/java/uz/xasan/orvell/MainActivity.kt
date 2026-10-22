package uz.xasan.orvell

import android.content.Context
import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.xasan.orvell.ui.AppViewModel
import uz.xasan.orvell.ui.components.AuroraBackground
import uz.xasan.orvell.ui.components.LocalAppScope
import uz.xasan.orvell.ui.components.LocalSnackbar
import uz.xasan.orvell.phone.PhoneSteps
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.ui.components.OrvellSnackbarHost
import uz.xasan.orvell.ui.components.rememberHaptics
import uz.xasan.orvell.ui.nav.LocalAuroraTone
import uz.xasan.orvell.ui.nav.LocalNavAnimScope
import uz.xasan.orvell.ui.nav.LocalSharedScope
import uz.xasan.orvell.ui.nav.detailRoute
import uz.xasan.orvell.ui.screens.AboutScreen
import uz.xasan.orvell.ui.screens.DetailScreen
import uz.xasan.orvell.ui.screens.DeviceScreen
import uz.xasan.orvell.ui.screens.FeedbackScreen
import uz.xasan.orvell.ui.screens.HealthScreen
import uz.xasan.orvell.ui.screens.LicensesScreen
import uz.xasan.orvell.ui.screens.OnboardingFlow
import uz.xasan.orvell.ui.screens.PairScreen
import uz.xasan.orvell.ui.screens.PrivacyScreen
import uz.xasan.orvell.ui.screens.ProfileEditScreen
import uz.xasan.orvell.ui.screens.ProfileScreen
import uz.xasan.orvell.ui.screens.ServerScreen
import uz.xasan.orvell.ui.screens.SettingsScreen
import uz.xasan.orvell.ui.screens.TodayScreen
import uz.xasan.orvell.ui.theme.Motion
import uz.xasan.orvell.ui.theme.O
import uz.xasan.orvell.ui.theme.OType
import uz.xasan.orvell.ui.theme.OrvellTheme
import uz.xasan.orvell.ui.theme.ThemeMode
import uz.xasan.orvell.ui.theme.Tone
import uz.xasan.orvell.ui.theme.Tones

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppLocale.syncOnStart(this)
        AppLocale.updateFmt(this)
        Notifications.createChannels(this)
        setContent {
            val vm: AppViewModel = viewModel()
            val s by vm.settings.collectAsState()
            val dark = when (s.theme) { ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT) else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
                enableEdgeToEdge(style, style)
            }
            OrvellTheme(s.theme) {
                val aurora = remember { mutableStateOf<Tone?>(null) }
                val snackbar = remember { SnackbarHostState() }
                val appScope = rememberCoroutineScope()
                val ctx = LocalContext.current
                // Telefon qadam sensori uchun ruxsat (Android 10+): berilmagan bo'lsa so'raladi, berilgach sensor yoqiladi
                val stepsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) PhoneSteps.start(ctx)
                }
                LaunchedEffect(Unit) {
                    if (Perms.activityRecognition(ctx)) PhoneSteps.start(ctx)
                    else stepsPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                // Sinxron natijasi snackbar'da: bir martalik hodisa (holat o'zgarishini kuzatmaydi, tez sinxron o'tib ketmasin).
                // Ulanish uzilib sinxron to'xtasa, natija chiqmaydi.
                LaunchedEffect(Unit) {
                    val scope = this
                    vm.syncResult.collect { r ->
                        val text = if (r.ok) getString(R.string.device_synced_new, Fmt.ago(r.at), r.newRecords) else r.error
                        if (text != null) scope.launch { snackbar.showSnackbar(text) }
                    }
                }
                CompositionLocalProvider(LocalAuroraTone provides aurora, LocalSnackbar provides snackbar, LocalAppScope provides appScope) {
                    AuroraBackground(aurora.value) {
                        AnimatedContent(s.onboarded, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) }, label = "root") { done ->
                            if (done) AppNav(vm) else OnboardingFlow(vm)
                        }
                        OrvellSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 96.dp))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val s = OrvellApp.get(this).settings.value
        // ruxsat yo'q bo'lsa start() o'zi ishga tushirmaydi; Qurilma ekrani ruxsatni qayta so'raydi
        if (s.onboarded && s.watch.paired) WatchService.start(this)
    }
}

/** Har bir manzil o'z animatsiya doirasini kartochka o'tishlari uchun beradi. */
@Composable
private fun AnimatedContentScope.Scoped(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavAnimScope provides this, content = content)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val reduce = O.reduceMotion
    val back: () -> Unit = { nav.popBackStack() }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedScope provides this) {
            NavHost(
                nav, "main",
                enterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(320)) + slideInHorizontally(tween(380, easing = Motion.Emphasized)) { it / 8 } },
                exitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(200)) },
                popEnterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(300)) },
                popExitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(220)) + slideOutHorizontally(tween(300)) { it / 8 } },
            ) {
                composable("main") { Scoped { MainTabs(vm, nav) } }
                composable("detail/{kind}/{from}") {
                    Scoped { DetailScreen(vm, it.arguments?.getString("kind") ?: "activity", it.arguments?.getString("from") ?: "today", back) }
                }
                composable("settings") { Scoped { SettingsScreen(vm, nav) } }
                composable("pair") { Scoped { PairScreen(vm, back) } }
                composable("profile/edit") { Scoped { ProfileEditScreen(vm, back) } }
                composable("about") { Scoped { AboutScreen(vm, back) } }
                composable("privacy") { Scoped { PrivacyScreen(back) } }
                composable("server") { Scoped { ServerScreen(vm, back) } }
                composable("licenses") { Scoped { LicensesScreen(back) } }
                composable("feedback") { Scoped { FeedbackScreen(back) } }
            }
        }
    }
}

private data class Tab(@StringRes val label: Int, val icon: ImageVector, val tone: Tone)

private val tabs = listOf(
    Tab(R.string.tab_today, Icons.Rounded.SpaceDashboard, Tones.activity),
    Tab(R.string.tab_health, Icons.Rounded.Insights, Tones.heart),
    Tab(R.string.tab_device, Icons.Rounded.Watch, Tones.oxygen),
    Tab(R.string.tab_profile, Icons.Rounded.Person, Tones.sleep),
)

@Composable
private fun MainTabs(vm: AppViewModel, nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val aurora = LocalAuroraTone.current
    LaunchedEffect(tab) { aurora.value = if (tab == 0) Tones.brand else tabs[tab].tone }
    val haptics = rememberHaptics()
    val reduce = O.reduceMotion
    val open: (String) -> Unit = { nav.navigate(it) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            tab,
            transitionSpec = {
                if (reduce) EnterTransition.None togetherWith ExitTransition.None
                else {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(240)) + slideInHorizontally(tween(300, easing = Motion.Emphasized)) { dir * it / 30 }) togetherWith fadeOut(tween(160))
                }
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> TodayScreen(vm, openDetail = { open(detailRoute(it, "today")) }, openDevice = { tab = 2 }, openProfile = { tab = 3 })
                1 -> HealthScreen(vm) { open(detailRoute(it, "health")) }
                2 -> DeviceScreen(vm, openPair = { open("pair") }, openSettings = { open("settings") })
                else -> ProfileScreen(vm, open)
            }
        }
        // Surilgan kontent soat va belgilar ostida o'qilmay qolmasin
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Brush.verticalGradient(listOf(O.c.bg, O.c.bg.copy(alpha = 0.85f)))),
        )
        TabBar(tab, { if (it != tab) { haptics.tick(); tab = it } }, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Suzuvchi pastki menyu: 4 teng joy, belgi va nom doim ko'rinadi (accessibility),
 * tanlangan joy ostidagi rangli kapsula prujina bilan suriladi va bo'lim rangini oladi.
 */
@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = O.c
    val shape = RoundedCornerShape(28.dp)
    val tone by animateColorAsState(tabs[selected].tone.a, tween(300), label = "tabTone")
    BoxWithConstraints(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(if (c.isDark) 24.dp else 12.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.raised.copy(alpha = 0.97f), c.surface.copy(alpha = 0.97f))))
            .border(1.dp, Brush.verticalGradient(listOf(c.highlight, c.stroke)), shape)
            .padding(6.dp),
    ) {
        val slot = constraints.maxWidth / tabs.size.toFloat()
        val x by animateFloatAsState(slot * selected, Motion.settle(), label = "tabX")
        Box(Modifier.fillMaxWidth()) {
            // kapsula: chizish fazasida suriladi (kompozitsiyasiz)
            Box(
                Modifier.matchParentSize().drawWithCache {
                    val r = CornerRadius(22.dp.toPx())
                    val edge = Stroke(1.dp.toPx())
                    onDrawBehind {
                        drawRoundRect(tone.copy(alpha = if (c.isDark) 0.18f else 0.14f), Offset(x, 0f), Size(slot, size.height), r)
                        drawRoundRect(tone.copy(alpha = 0.35f), Offset(x, 0f), Size(slot, size.height), r, style = edge)
                    }
                },
            )
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                tabs.forEachIndexed { i, t ->
                    val sel = i == selected
                    Column(
                        Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(22.dp))
                            .selectable(selected = sel, role = Role.Tab) { onSelect(i) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(t.icon, null, tint = if (sel) t.tone.ink else c.text2, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(stringResource(t.label), style = OType.label, color = if (sel) c.text else c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
