// app/src/test/java/com/bikeputer/metrics/ZonesTest.kt
package com.bikeputer.metrics

import org.junit.Assert.assertEquals
import org.junit.Test

class ZonesTest {
    @Test fun zone_of_picks_first_bound_not_exceeded() {
        val m = ZoneModel(listOf(100, 200, 300))
        assertEquals(1, m.zoneOf(50))
        assertEquals(1, m.zoneOf(100))
        assertEquals(2, m.zoneOf(150))
        assertEquals(4, m.zoneOf(999)) // above all bounds -> top zone
    }

    @Test fun power_zones_have_seven_zones_from_ftp() {
        val m = Zones.powerZonesFromFtp(200)
        assertEquals(6, m.upperBounds.size) // 7 zones
        assertEquals(1, m.zoneOf(50))   // < 0.55*200=110
        assertEquals(7, m.zoneOf(400))  // > 1.50*200=300
    }

    @Test fun hr_zones_have_five_zones_from_max() {
        val m = Zones.hrZonesFromMax(190)
        assertEquals(4, m.upperBounds.size) // 5 zones
        assertEquals(5, m.zoneOf(185))
    }

    @Test fun zone_of_exact_bound_is_inclusive_lower_zone() {
        val model = ZoneModel(listOf(100, 200))
        assertEquals(1, model.zoneOf(100)) // value == first bound -> zone 1 (<=)
        assertEquals(2, model.zoneOf(101)) // just over first bound -> zone 2
        assertEquals(2, model.zoneOf(200)) // value == second bound -> zone 2
        assertEquals(3, model.zoneOf(201)) // above all bounds -> top zone
    }
}
