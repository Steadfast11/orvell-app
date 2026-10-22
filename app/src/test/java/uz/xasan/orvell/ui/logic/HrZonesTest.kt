package uz.xasan.orvell.ui.logic

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.xasan.orvell.data.Sample
import uz.xasan.orvell.data.T

class HrZonesTest {
    @Test fun maxHrNeedsPlausibleAge() {
        assertEquals(190, HrZones.maxHr(30))
        assertNull(HrZones.maxHr(null))
        assertNull(HrZones.maxHr(5))
    }

    @Test fun zoneBoundaries() {
        assertEquals(0, HrZones.zoneOf(94, 190))   // 49%
        assertEquals(1, HrZones.zoneOf(95, 190))   // 50%
        assertEquals(3, HrZones.zoneOf(140, 190))  // 73%
        assertEquals(5, HrZones.zoneOf(180, 190))  // 94%
    }

    @Test fun minutesInZonesCapsGaps() {
        val min = 60_000L
        val s = listOf(
            Sample(T.HR, 0, value = 70.0),            // 10 daq dam
            Sample(T.HR, 10 * min, value = 140.0),    // 5 daq zona 3
            Sample(T.HR, 15 * min, value = 100.0),    // 3 soat bo'shliq -> 15 daq bilan cheklanadi (zona 1)
            Sample(T.HR, 195 * min, value = 175.0),   // oxirgi: 5 daq zona 5
        )
        assertArrayEquals(intArrayOf(10, 15, 0, 5, 0, 5), HrZones.minutesInZones(s.shuffled(), 190))
    }
}
