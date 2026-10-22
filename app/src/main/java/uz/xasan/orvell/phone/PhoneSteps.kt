package uz.xasan.orvell.phone

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.xasan.orvell.Perms
import java.time.LocalDate

/**
 * Telefonning o'z qadam sensori: soat qadami EMAS. Orvell ochilgandan beri, kun bo'yicha qadamlarni hisoblaydi.
 * Kun boshidagi qadamlar bu yerda ko'rinmaydi: hisob shu kunning birinchi o'qishidan boshlanadi.
 */
object PhoneSteps {
    private const val PREFS = "phone_steps"
    private val _today = MutableStateFlow<Int?>(null)

    /** Bugungi telefon qadamlari. Bu kun uchun hali o'qilmagan bo'lsa null. */
    val today: StateFlow<Int?> = _today

    private var tally: StepTally? = null
    private var started = false

    /** Ruxsat bo'lsa sensorni yoqadi. Qayta chaqirilsa e'tiborsiz. Sensor yo'q yoki ruxsat bo'lmasa jim o'tadi. */
    @Synchronized
    fun start(ctx: Context) {
        if (started) return
        val app = ctx.applicationContext
        if (!Perms.activityRecognition(app)) return
        val sm = app.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        tally = load(prefs)
        _today.value = tally?.takeIf { it.date == LocalDate.now() }?.total
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = record(prefs, event.values[0].toLong())
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        started = true
    }

    /** Berilgan kun uchun telefon qadamlari. Shu kun hali o'qilmagan bo'lsa null: kun almashganda eski son qolmaydi. */
    @Synchronized
    fun valueFor(date: LocalDate): Int? = tally?.takeIf { it.date == date }?.total

    @Synchronized
    private fun record(prefs: SharedPreferences, counter: Long) {
        val today = LocalDate.now()
        val t = (tally ?: StepTally(today, 0, counter)).add(counter, today)
        tally = t
        _today.value = t.total
        prefs.edit().putLong("date", t.date.toEpochDay()).putInt("total", t.total).putLong("last", t.last).apply()
    }

    private fun load(prefs: SharedPreferences): StepTally? {
        if (!prefs.contains("date")) return null
        return StepTally(LocalDate.ofEpochDay(prefs.getLong("date", 0)), prefs.getInt("total", 0), prefs.getLong("last", 0))
    }
}
