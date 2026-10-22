package uz.xasan.orvell.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** clearAll qaysi jadvallarni o'chiradi (SQL qismi Android'da, bu yerda faqat tanlov mantig'i). */
class StoreClearTest {
    @Test
    fun clearsBothDataTablesAndNotSystemTables() {
        val existing = setOf("android_metadata", "samples", "sqlite_sequence", "watch_raw")
        assertEquals(listOf("samples", "watch_raw"), tablesToClear(existing))
    }

    @Test
    fun missingTableIsSkipped() {
        // watch_raw yo'q eski baza: xato emas, faqat bor jadval o'chiriladi
        assertEquals(listOf("samples"), tablesToClear(setOf("android_metadata", "samples")))
        assertEquals(emptyList<String>(), tablesToClear(emptySet()))
    }
}
