package uz.xasan.orvell.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.Writer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Telefondagi yagona haqiqat manbai. Ekranlar faqat shu yerdan o'qiydi, shuning uchun ilova
 * internet va soatsiz ham to'liq ishlaydi. Bir xil yozuv (tur, vaqt, manba) qayta kelsa yangilanadi.
 */
class Store(context: Context, private val zone: () -> ZoneId = { ZoneId.systemDefault() }) : SQLiteOpenHelper(context, "orvell.db", null, 6) {
    private val _changes = MutableStateFlow(0L)
    /** Har yozuvdan keyin o'zgaradi: ekranlar shunga qarab qayta hisoblaydi. */
    val changes: StateFlow<Long> = _changes

    init {
        // xizmat yozayotganda ekranlar o'qishda kutib qolmasin
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE samples(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL, start INTEGER NOT NULL, "end" INTEGER NOT NULL,
                value REAL NOT NULL, extra REAL, source TEXT NOT NULL, synced INTEGER NOT NULL DEFAULT 0,
                UNIQUE(type, start, "end", source))"""
        )
        db.execSQL("CREATE INDEX samples_type_start ON samples(type, start)")
        createIndexesV2(db)
        createWatchRaw(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createIndexesV2(db)
        when {
            oldVersion < 3 -> createWatchRaw(db)
            oldVersion == 3 -> rebuildWatchRaw(db, keepSync = false)
            oldVersion < 6 -> rebuildWatchRaw(db, keepSync = true)
        }
        if (oldVersion < 5) decodeAllRaw(db)
    }

    /** v5: avval faqat xom saqlangan tarix yozuvlari talqin qilinadi (hammasi bitta bo'lakli; eski tartibda: keyingisi ustun). */
    private fun decodeAllRaw(db: SQLiteDatabase) {
        db.rawQuery("SELECT counter, payload FROM watch_raw ORDER BY id", null).use { cur ->
            while (cur.moveToNext()) writeSamples(db, watchSamples(cur.getInt(0), cur.getBlob(1), zone()))
        }
    }

    /**
     * Ko'p yillik ma'lumotda ham tez bo'lishi uchun (v2):
     * - uyqu tuni tugash vaqti bo'yicha qidiriladi (rangeByEnd);
     * - sinx navbati faqat yuborilmaganlarni ko'radi (qisman indeks, kichik bo'lib qoladi);
     * - eng birinchi yozuv va eksport vaqt bo'yicha.
     */
    private fun createIndexesV2(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS samples_type_end ON samples(type, \"end\")")
        db.execSQL("CREATE INDEX IF NOT EXISTS samples_unsynced ON samples(start) WHERE synced=0")
        db.execSQL("CREATE INDEX IF NOT EXISTS samples_start ON samples(start)")
    }

    /**
     * Soat tarixi: xom bo'laklar. Bitta hisoblagich yozuvi bir nechta 0x0030 bo'lagida keladi (part = tartib, v6).
     * Yozuv to'lganda bo'laklar birlashtirilib talqin qilinadi (watchSamples); xom bayt diagnostika va qayta talqin uchun qoladi.
     * UNIQUE(counter, payload, sync, part): bitta sinxron ichida takror yuborilgan bo'lak bitta qator bo'ladi;
     * bir xil baytli boshqa bo'lak (masalan kechasi nol qadamlar) part bilan ajraladi.
     */
    private fun createWatchRaw(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS watch_raw(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                received INTEGER NOT NULL, counter INTEGER NOT NULL, payload BLOB NOT NULL,
                sync INTEGER NOT NULL DEFAULT 0, part INTEGER NOT NULL DEFAULT 0,
                UNIQUE(counter, payload, sync, part))"""
        )
    }

    /**
     * UNIQUE'ni jadvalda o'zgartirib bo'lmaydi: jadval qayta yaratiladi, qatorlar ko'chiriladi (part = 0: eski
     * yozuvlar bitta bo'lakli). v3 dan: sync = 0. onUpgrade tranzaksiyada ishlaydi: yarim o'tkazish qolmaydi.
     */
    private fun rebuildWatchRaw(db: SQLiteDatabase, keepSync: Boolean) {
        db.execSQL("ALTER TABLE watch_raw RENAME TO watch_raw_old")
        createWatchRaw(db)
        val sync = if (keepSync) "sync" else "0"
        db.execSQL("INSERT INTO watch_raw(id, received, counter, payload, sync) SELECT id, received, counter, payload, $sync FROM watch_raw_old")
        db.execSQL("DROP TABLE watch_raw_old")
    }

    fun insert(items: List<Sample>) {
        if (items.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            writeSamples(db, items)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _changes.value = System.currentTimeMillis()
    }

    private fun writeSamples(db: SQLiteDatabase, items: List<Sample>) {
        for (s in items) {
            val cv = ContentValues().apply {
                put("type", s.type); put("start", s.start); put("\"end\"", s.end); put("value", s.value)
                if (s.extra != null) put("extra", s.extra) else putNull("extra")
                put("source", s.source); put("synced", 0)
            }
            db.insertWithOnConflict("samples", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    /** Tarix sinxroni tugagach (muvaffaqiyatli yoki yo'q) saqlangan yozuvlar soni yangilanishi uchun. */
    fun notifyChanged() { _changes.value = System.currentTimeMillis() }

    /**
     * Soat tarix yozuvi bo'lagini xom bayt sifatida saqlaydi. complete = true: shu sinxrondagi hisoblagich bo'laklari
     * tartib bilan birlashtirilib talqin qilinadi (qadam, puls) va bitta tranzaksiyada yoziladi.
     * True: yangi qator. False: shu bo'lak avval saqlangan. DB xatosi istisno: chaqiruvchi soatga ACK yubormaydi.
     */
    fun insertWatchRecord(counter: Int, payload: ByteArray, received: Long, sync: Long, part: Int = 0, complete: Boolean = true): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val fresh = db.compileStatement(
                "INSERT OR IGNORE INTO watch_raw(received, counter, payload, sync, part) VALUES (?, ?, ?, ?, ?)",
            ).use { st ->
                st.bindLong(1, received)
                st.bindLong(2, counter.toLong())
                st.bindBlob(3, payload)
                st.bindLong(4, sync)
                st.bindLong(5, part.toLong())
                st.executeInsert() != -1L
            }
            if (complete) writeSamples(db, watchSamples(counter, recordParts(db, counter, sync, part), zone()))
            db.setTransactionSuccessful()
            return fresh
        } finally {
            db.endTransaction()
        }
    }

    /** Sinxrondagi hisoblagich bo'laklari 0..last, birlashtirilgan. Har part uchun oxirgi saqlangani olinadi. */
    private fun recordParts(db: SQLiteDatabase, counter: Int, sync: Long, last: Int): ByteArray {
        val parts = arrayOfNulls<ByteArray>(last + 1)
        db.rawQuery(
            "SELECT part, payload FROM watch_raw WHERE counter=? AND sync=? AND part<=? ORDER BY id",
            arrayOf(counter.toString(), sync.toString(), last.toString()),
        ).use { cur -> while (cur.moveToNext()) parts[cur.getInt(0)] = cur.getBlob(1) }
        if (parts.any { it == null }) return ByteArray(0)
        return parts.fold(ByteArray(0)) { acc, p -> acc + p!! }
    }

    fun range(type: String, from: Long, to: Long): List<Sample> =
        readableDatabase.rawQuery(
            "SELECT type, start, \"end\", value, extra, source FROM samples WHERE type=? AND start>=? AND start<? ORDER BY start",
            arrayOf(type, from.toString(), to.toString()),
        ).use { cur ->
            buildList {
                while (cur.moveToNext()) add(Sample(cur.getString(0), cur.getLong(1), cur.getLong(2), cur.getDouble(3),
                    if (cur.isNull(4)) null else cur.getDouble(4), cur.getString(5)))
            }
        }

    /** Tugashi [from, to) oralig'ida bo'lgan yozuvlar (uyqu tuni uchun). */
    fun rangeByEnd(type: String, from: Long, to: Long): List<Sample> =
        readableDatabase.rawQuery(
            "SELECT type, start, \"end\", value, extra, source FROM samples WHERE type=? AND \"end\">? AND \"end\"<=? ORDER BY start",
            arrayOf(type, from.toString(), to.toString()),
        ).use { cur ->
            buildList {
                while (cur.moveToNext()) add(Sample(cur.getString(0), cur.getLong(1), cur.getLong(2), cur.getDouble(3),
                    if (cur.isNull(4)) null else cur.getDouble(4), cur.getString(5)))
            }
        }

    fun latest(type: String): Sample? =
        readableDatabase.rawQuery(
            "SELECT type, start, \"end\", value, extra, source FROM samples WHERE type=? ORDER BY start DESC LIMIT 1", arrayOf(type),
        ).use { cur -> if (cur.moveToFirst()) Sample(cur.getString(0), cur.getLong(1), cur.getLong(2), cur.getDouble(3), if (cur.isNull(4)) null else cur.getDouble(4), cur.getString(5)) else null }

    /** Serverga hali yuborilmagan yozuvlar (id bilan). Yozuv yangilansa (REPLACE) yana yuborilmagan bo'ladi. */
    fun unsynced(limit: Int = 500): List<Pair<Long, Sample>> =
        readableDatabase.rawQuery(
            "SELECT id, type, start, \"end\", value, extra, source FROM samples WHERE synced=0 ORDER BY start LIMIT ?", arrayOf(limit.toString()),
        ).use { cur ->
            buildList {
                while (cur.moveToNext()) add(cur.getLong(0) to Sample(cur.getString(1), cur.getLong(2), cur.getLong(3), cur.getDouble(4),
                    if (cur.isNull(5)) null else cur.getDouble(5), cur.getString(6)))
            }
        }

    fun markSynced(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.chunked(500).forEach { chunk -> db.execSQL("UPDATE samples SET synced=1 WHERE id IN (${chunk.joinToString(",")})") }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    /**
     * Server 422 (yaroqsiz) bilan rad etgan yozuvlar: navbatdan chiqadi (synced = 2), lekin DB dan o'chirilmaydi.
     * Yozuv soat tomonidan qayta o'qilib yangilansa (insert REPLACE), u yana yuborilmagan holatga qaytadi.
     */
    fun markRejected(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.chunked(500).forEach { chunk -> db.execSQL("UPDATE samples SET synced=2 WHERE id IN (${chunk.joinToString(",")})") }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun unsyncedCount(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM samples WHERE synced=0", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }

    /** Server rad etgan yozuvlar soni (synced = 2). */
    fun rejectedCount(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM samples WHERE synced=2", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }

    fun count(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM samples", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }

    fun firstTime(): Long? = readableDatabase.rawQuery("SELECT MIN(start) FROM samples", null).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }

    /** Hamma yozuv o'chadi (samples va watch_raw), bitta tranzaksiyada: yarim o'chirilgan holat qolmaydi. Bazada yo'q jadval o'tkazib yuboriladi. */
    fun clearAll() {
        writableDatabase.transaction {
            val existing = rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cur ->
                buildSet { while (cur.moveToNext()) add(cur.getString(0)) }
            }
            for (t in tablesToClear(existing)) execSQL("DELETE FROM $t")
        }
        _changes.value = System.currentTimeMillis()
    }

    /** Barcha ma'lumotni CSV ko'rinishida yozadi (vaqtlar ISO-8601, UTC). */
    fun exportCsv(out: Writer) {
        out.write("type,start,end,value,extra,source\n")
        readableDatabase.rawQuery("SELECT type, start, \"end\", value, extra, source FROM samples ORDER BY start", null).use { cur ->
            while (cur.moveToNext()) {
                out.write("${cur.getString(0)},${Instant.ofEpochMilli(cur.getLong(1))},${Instant.ofEpochMilli(cur.getLong(2))},${cur.getDouble(3)},${if (cur.isNull(4)) "" else cur.getDouble(4)},${cur.getString(5)}\n")
            }
        }
    }

    /**
     * Eski versiyadan qolgan namunalar soni (talqini tasdiqlanmagan). Hozirgi kod bunday namunalarni yozmaydi;
     * ekranlar shu son noldan katta bo'lsa ogohlantirish ko'rsatadi.
     */
    fun legacySampleCount(from: Long = 0, to: Long = Long.MAX_VALUE): Long =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM samples WHERE source IN ('watch', 'watch_live') AND start>=? AND start<?",
            arrayOf(from.toString(), to.toString()),
        ).use { if (it.moveToFirst()) it.getLong(0) else 0 }

    /** Soatdan saqlangan xom tarix yozuvlari soni (qiymatlari hali talqin qilinmagan). */
    fun watchRawCount(): Long =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM watch_raw", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }

    /** Xom tarix diagnostikasi: id, vaqt, sinxron (0 = v3 dan qolgan), hisoblagich, uzunlik va baytlar hex ko'rinishida. */
    fun exportWatchRawCsv(out: Writer) {
        out.write("id,received,sync,counter,length,payload_hex\n")
        readableDatabase.rawQuery("SELECT id, received, counter, payload, sync FROM watch_raw ORDER BY id", null).use { cur ->
            while (cur.moveToNext()) {
                val payload = cur.getBlob(3)
                val hex = payload.joinToString("") { String.format("%02X", it.toInt() and 0xFF) }
                out.write("${cur.getLong(0)},${Instant.ofEpochMilli(cur.getLong(1))},${cur.getLong(4)},${cur.getInt(2)},${payload.size},$hex\n")
            }
        }
    }
}

/** Foydalanuvchi yozuvlari jadvallari: clearAll shularni o'chiradi. */
private val DATA_TABLES = listOf("samples", "watch_raw")

/** clearAll o'chiradigan jadvallar: faqat bazada borlari (yo'q jadval xato bermasin). Tizim jadvallariga tegilmaydi. */
internal fun tablesToClear(existing: Set<String>): List<String> = DATA_TABLES.filter { it in existing }

// ================================================================ hisob-kitoblar (sof mantiq: HealthMath.kt)

class Health(private val store: Store, private val zone: () -> ZoneId = { ZoneId.systemDefault() }) {

    private fun dayStart(d: LocalDate) = d.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun day(d: LocalDate): DayStats {
        val a = dayStart(d); val b = dayStart(d.plusDays(1))
        val buckets = store.range(T.STEPS_BUCKET, a, b)
        val totals = store.range(T.STEPS_TOTAL, a, b)
        val hourly = HealthMath.hourlySteps(buckets, zone())
        val hr = HealthMath.validHeartRate(store.range(T.HR, a, b))
        val sleep = sleepNight(d)
        return DayStats(
            date = d,
            steps = HealthMath.dailySteps(totals, hourly),
            hasSteps = totals.isNotEmpty() || buckets.isNotEmpty(),
            distanceM = HealthMath.dailyDistance(store.range(T.DIST_TOTAL, a, b), buckets),
            kcal = maxOf(HealthMath.dailyMax(store.range(T.KCAL_TOTAL, a, b)), store.range(T.KCAL_BUCKET, a, b).sumOf { it.value }.toInt()),
            hourlySteps = hourly,
            hr = hr, hrAvg = hr.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.toInt(),
            hrMin = hr.minOfOrNull { it.value }?.toInt(), hrMax = hr.maxOfOrNull { it.value }?.toInt(),
            restingHr = HealthMath.restingHeartRate(hr, sleep),
            spo2 = HealthMath.validSpo2(store.range(T.SPO2, a, b)),
            bp = store.range(T.BP, a, b), resp = store.range(T.RESP, a, b), sleep = sleep,
        )
    }

    /**
     * Shu kun ertalab tugagan tun: tugashi kechagi 18:00 dan bugungi 18:00 gacha bo'lgan bosqichlar.
     * Soat tunni bo'laklab yozadi — 2 soatgacha bo'shliq bitta tun hisoblanadi, eng uzun guruh olinadi.
     */
    fun sleepNight(d: LocalDate): SleepNight? {
        val (from, to) = HealthMath.sleepWindow(d, zone())
        return HealthMath.sleepNight(store.rangeByEnd(T.SLEEP, from, to))
    }

    data class DayPoint(
        val date: LocalDate, val steps: Int, val distanceM: Int, val kcal: Int,
        val sleepMin: Int, val inBedMin: Int, val hrAvg: Int?, val restingHr: Int?, val spo2Avg: Int?, val respAvg: Int?,
    )

    fun series(endDay: LocalDate, days: Int): List<DayPoint> =
        (days - 1 downTo 0).map { i ->
            val s = day(endDay.minusDays(i.toLong()))
            DayPoint(
                s.date, s.steps, s.distanceM, s.kcal, s.sleep?.asleepMin ?: 0, s.sleep?.inBedMin ?: 0, s.hrAvg, s.restingHr,
                s.spo2.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.toInt(),
                s.resp.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.toInt(),
            )
        }
}
