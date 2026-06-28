package com.bikeputer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun duration_formats_hh_mm_ss() {
        assertEquals("01:02:03", Format.duration(3_723_000))
        assertEquals("00:00:05", Format.duration(5_000))
    }

    @Test fun speed_metric_vs_imperial() {
        assertEquals("36.0 km/h", Format.speed(36.0, imperial = false))
        assertEquals("22.4 mph", Format.speed(36.0, imperial = true))
        assertEquals("-- km/h", Format.speed(null, imperial = false))
    }

    @Test fun distance_metric_vs_imperial() {
        assertEquals("5.00 km", Format.distance(5000.0, imperial = false))
        assertEquals("3.11 mi", Format.distance(5000.0, imperial = true))
    }
}
