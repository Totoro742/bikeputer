package com.bikeputer.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashFormatTest {

    @Test fun metric_distance_under_half_km_reads_in_whole_metres() {
        assertEquals("86", DashFormat.distanceValue(86.0, imperial = false))
        assertEquals("m", DashFormat.distanceUnit(86.0, imperial = false).lowercase())
        assertEquals("499", DashFormat.distanceValue(499.4, imperial = false)) // rounds to whole metres
    }

    @Test fun metric_distance_at_or_above_half_km_reads_in_km() {
        assertEquals("0.5", DashFormat.distanceValue(500.0, imperial = false)) // 500 is not "under" 0.5 km
        assertEquals("km", DashFormat.distanceUnit(500.0, imperial = false).lowercase())
        assertEquals("1.2", DashFormat.distanceValue(1234.0, imperial = false))
        assertEquals("km", DashFormat.distanceUnit(1234.0, imperial = false).lowercase())
    }

    @Test fun imperial_distance_mirrors_with_feet_close_in_then_miles() {
        // Under 0.1 mi (~161 m) reads in whole feet; beyond that, miles.
        assertEquals("328", DashFormat.distanceValue(100.0, imperial = true)) // 100 m ≈ 328 ft
        assertEquals("ft", DashFormat.distanceUnit(100.0, imperial = true).lowercase())
        assertEquals("0.2", DashFormat.distanceValue(300.0, imperial = true)) // 300 m ≈ 0.19 mi
        assertEquals("mi", DashFormat.distanceUnit(300.0, imperial = true).lowercase())
    }
}
