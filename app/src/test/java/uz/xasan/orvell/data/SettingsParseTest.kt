package uz.xasan.orvell.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Saqlangan sozlamani o'qish: eski yoki noma'lum qiymat ilovani yiqitmaydi. */
class SettingsParseTest {
    @Test
    fun cloudStorageReadsBackAsLocal() {
        // bulut hali amalga oshmagan: saqlangan CLOUD ham mahalliy deb o'qiladi
        assertEquals(StorageMode.LOCAL, storageModeOf("CLOUD"))
    }

    @Test
    fun localStorageReadsBack() {
        assertEquals(StorageMode.LOCAL, storageModeOf("LOCAL"))
    }

    @Test
    fun unknownOrMissingStorageFallsBackToLocal() {
        listOf(null, "", "local", "REMOTE").forEach { assertEquals(StorageMode.LOCAL, storageModeOf(it)) }
    }
}
