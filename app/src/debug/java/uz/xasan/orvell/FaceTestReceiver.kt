package uz.xasan.orvell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import uz.xasan.orvell.watch.WatchManager
import java.io.File

/**
 * Faqat debug: siferblat uzatish protokolini qurilmada sinash (tayyor fayl).
 * adb push face.bin /sdcard/Android/data/uz.xasan.orvell.dev/files/face.bin
 * adb shell am broadcast -n uz.xasan.orvell.dev/uz.xasan.orvell.FaceTestReceiver
 */
class FaceTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val f = File(context.getExternalFilesDir(null), "face.bin")
        if (!f.isFile) { Log.w("OrvellWatch", "face test: ${f.path} yo'q"); return }
        val ok = WatchManager.uploadFace(f.readBytes())
        Log.i("OrvellWatch", "face test: ${f.length()} bayt, boshlandi=$ok")
    }
}
