package uz.xasan.orvell

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xasan.orvell.sync.ServerSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.xasan.orvell.data.Health
import uz.xasan.orvell.data.SettingsRepo
import uz.xasan.orvell.data.Store
import uz.xasan.orvell.ui.Fmt
import uz.xasan.orvell.watch.Conn
import uz.xasan.orvell.watch.WatchManager
import java.time.LocalDate

class OrvellApp : Application() {
    lateinit var store: Store; private set
    lateinit var settings: SettingsRepo; private set
    lateinit var health: Health; private set

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        settings = SettingsRepo(this)
        health = Health(store)
        // Telefon qadam sensori: ruxsat bo'lsa yoqiladi (soat ma'lumotidan mustaqil)
        uz.xasan.orvell.phone.PhoneSteps.start(this)
        AppLocale.updateFmt(AppLocale.localized(this))  // xizmat Activity'siz ishga tushganda ham Fmt tayyor bo'lsin
        WatchManager.init(this, store, settings)
        Notifications.createChannels(this)
    }

    companion object {
        fun get(context: Context) = context.applicationContext as OrvellApp
    }
}

object Notifications {
    const val CH_SERVICE = "watch_link"
    const val CH_ALERTS = "alerts"
    const val ID_SERVICE = 1
    const val ID_BATTERY = 10
    const val ID_DISCONNECT = 11
    const val ID_GOAL = 12

    /** Kanallar tanlangan tilda; til almashganda qayta chaqiriladi (nomlar yangilanadi). */
    fun createChannels(ctx: Context) {
        val l = AppLocale.localized(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_SERVICE, l.getString(R.string.notif_ch_service), NotificationManager.IMPORTANCE_MIN).apply {
            description = l.getString(R.string.notif_ch_service_desc); setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALERTS, l.getString(R.string.notif_ch_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = l.getString(R.string.notif_ch_alerts_desc)
        })
    }

    fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
    )

    fun alert(ctx: Context, id: Int, title: String, text: String) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_orvell).setContentTitle(title).setContentText(text)
            .setContentIntent(openApp(ctx)).setAutoCancel(true).build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}

/**
 * Oldingi plan xizmati: soat bilan ulanishni fonda ushlab turadi (Android ilovani o'ldirmasligi uchun)
 * va ogohlantirishlarni chiqaradi.
 */
class WatchService : LifecycleService() {
    private var lowBatteryShown = false
    private var disconnectedSince = 0L
    private var disconnectShown = false

    override fun onCreate() {
        super.onCreate()
        val app = OrvellApp.get(this)
        // Android 14+: connectedDevice turidagi xizmat BLUETOOTH_CONNECT ruxsatisiz boshlanmaydi (SecurityException).
        // start() buni oldindan tekshiradi; bu yerda ruxsat shu oraliqda olib qo'yilgan holat uchun himoya.
        try {
            startInForeground(notification(str(R.string.notif_connecting)))
        } catch (e: Exception) {
            stopSelf()
            return
        }
        WatchManager.start()
        // ixtiyoriy server sinxroni: soat tarix sinxroni tugaganda va har 15 daqiqada (qayta urinish)
        lifecycleScope.launch(Dispatchers.IO) {
            WatchManager.state.map { it.historyAt }.distinctUntilChanged().collect { ServerSync.push(app.store, app.settings.value) }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) { delay(15 * 60_000L); ServerSync.push(app.store, app.settings.value) }
        }
        // serverning "hozir yangila" buyrug'ini tinglash (AI refresh_now)
        uz.xasan.orvell.sync.DeviceStream.run(lifecycleScope, app)
        lifecycleScope.launch {
            combine(WatchManager.state, app.settings.flow) { s, cfg -> s to cfg }
                .distinctUntilChanged()
                .collect { (s, cfg) ->
                    val text = when (s.conn) {
                        Conn.CONNECTED -> if (s.battery >= 0) str(R.string.notif_connected_battery, s.battery) else str(R.string.notif_connected)
                        Conn.CONNECTING -> str(R.string.notif_state_connecting)
                        Conn.CONFIRM_ON_WATCH -> str(R.string.notif_state_confirm)
                        Conn.BLUETOOTH_OFF -> str(R.string.notif_state_bt_off)
                        Conn.DISCONNECTED -> str(R.string.notif_state_disconnected)
                        Conn.UNPAIRED -> str(R.string.notif_state_unpaired)
                    }
                    getSystemService(NotificationManager::class.java).notify(Notifications.ID_SERVICE, notification(text))
                    checkAlerts(s.conn, s.battery, s.charging, cfg.alertLowBattery, cfg.alertDisconnect)
                }
        }
    }

    private fun checkAlerts(conn: Conn, battery: Int, charging: Boolean, lowOn: Boolean, discOn: Boolean) {
        if (battery in 0..15 && !charging) {
            if (lowOn && !lowBatteryShown) Notifications.alert(this, Notifications.ID_BATTERY, str(R.string.notif_battery_title), str(R.string.notif_battery_text, battery))
            lowBatteryShown = true
        } else if (battery > 20 || charging) lowBatteryShown = false

        val now = System.currentTimeMillis()
        if (conn == Conn.CONNECTED) { disconnectedSince = 0; disconnectShown = false }
        else if (conn == Conn.DISCONNECTED || conn == Conn.BLUETOOTH_OFF) {
            if (disconnectedSince == 0L) disconnectedSince = now
            if (discOn && !disconnectShown && now - disconnectedSince > 30 * 60_000L) {
                Notifications.alert(this, Notifications.ID_DISCONNECT, str(R.string.notif_disconnect_title), str(R.string.notif_disconnect_text))
                disconnectShown = true
            }
        }
    }

    /** Har safar joriy tilda (til xizmat ishlayotganda almashishi mumkin). */
    private fun str(id: Int, vararg args: Any): String = AppLocale.localized(this).getString(id, *args)

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, Notifications.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_orvell).setContentTitle("Orvell").setContentText(text)
            .setOngoing(true).setSilent(true).setContentIntent(Notifications.openApp(this))
            .setPriority(NotificationCompat.PRIORITY_MIN).build()

    private fun startInForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, Notifications.ID_SERVICE, n, type)
    }

    override fun onDestroy() {
        WatchManager.stop()
        super.onDestroy()
    }

    companion object {
        /**
         * Soat bog'langan va Bluetooth ruxsati bo'lsa xizmatni ishga tushiradi.
         * Ruxsat olib qo'yilgan bo'lsa ishga tushirmaydi (aks holda xizmat yiqiladi); telefon qayta yonganda buni bildiradi.
         */
        fun start(ctx: Context, fromBoot: Boolean = false) {
            if (!OrvellApp.get(ctx).settings.value.watch.paired) return
            if (!Perms.canConnect(ctx)) {
                if (fromBoot) {
                    val l = AppLocale.localized(ctx)
                    Notifications.alert(ctx, Notifications.ID_DISCONNECT, l.getString(R.string.notif_permission_title), l.getString(R.string.notif_permission_text))
                }
                return
            }
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, WatchService::class.java)) }
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, WatchService::class.java)) }
    }
}

/** Telefon qayta yonganda yoki ilova yangilanganda ulanishni tiklaydi. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) WatchService.start(context, fromBoot = true)
    }
}
