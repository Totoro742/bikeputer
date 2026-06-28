package com.bikeputer.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class MapZoomTest {

    @Test
    fun coerce_clamps_to_setting_range() {
        assertEquals(13, MapZoom.coerce(10))
        assertEquals(13, MapZoom.coerce(13))
        assertEquals(15, MapZoom.coerce(15))
        assertEquals(18, MapZoom.coerce(18))
        assertEquals(18, MapZoom.coerce(25))
    }

    @Test
    fun adaptive_window_matches_legacy_clamp_at_default() {
        // Back-compat: default 16 must reproduce today's [14.0, 17.0] clamp.
        assertEquals(14.0, MapZoom.adaptiveMin(16), 0.0)
        assertEquals(17.0, MapZoom.adaptiveMax(16), 0.0)
    }

    @Test
    fun adaptive_window_shifts_with_default() {
        assertEquals(11.0, MapZoom.adaptiveMin(13), 0.0)
        assertEquals(14.0, MapZoom.adaptiveMax(13), 0.0)
        assertEquals(16.0, MapZoom.adaptiveMin(18), 0.0)
        assertEquals(19.0, MapZoom.adaptiveMax(18), 0.0)
    }
}
