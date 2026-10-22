package uz.xasan.orvell

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Ruxsatlar va tizim holatlari bitta joyda (xizmat, Activity va ekranlar uchun bir xil qoida). */
object Perms {
    /** Soat qidirish va ulanish uchun so'raladigan ruxsatlar. Android 11 va eskida BLE qidiruvi joylashuv ruxsatini talab qiladi. */
    fun bluetoothScan(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun has(ctx: Context, perms: Array<String>) = perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

    /** Bog'langan soatga ulanish (xizmat) uchun: Android 12+ da BLUETOOTH_CONNECT, eskisida o'rnatishda beriladi. */
    fun canConnect(ctx: Context) = Build.VERSION.SDK_INT < 31 || has(ctx, arrayOf(Manifest.permission.BLUETOOTH_CONNECT))

    fun bluetoothOn(ctx: Context) = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true

    /** Telefon qadam sensori uchun ruxsat (Android 10+). Eskisida ruxsat talab qilinmaydi. */
    fun activityRecognition(ctx: Context) =
        Build.VERSION.SDK_INT < 29 || has(ctx, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))

    /** Android 11 va eskida joylashuv xizmati o'chiq bo'lsa, BLE qidiruvi hech narsa topmaydi. */
    fun needsLocationServices() = Build.VERSION.SDK_INT < 31

    fun locationOn(ctx: Context) = (ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.isLocationEnabled == true

    /** Ilova bildirishnomalari yoqilganmi (Android 13+ ruxsat yoki tizim sozlamasida o'chirilgan). */
    fun notificationsOn(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true

    fun appSettingsIntent(ctx: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
