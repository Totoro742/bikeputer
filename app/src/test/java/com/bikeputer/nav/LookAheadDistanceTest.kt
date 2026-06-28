package com.bikeputer.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class LookAheadDistanceTest {
    @Test fun fixed_ignores_speed() {
        assertEquals(400.0, lookAheadMeters(null, adaptive = false), 1e-9)
        assertEquals(400.0, lookAheadMeters(30.0, adaptive = false), 1e-9)
    }

    @Test fun adaptive_scales_with_speed() {
        // 36 km/h = 10 m/s; 10 m/s × 20 s = 200 m (within the clamp band).
        assertEquals(200.0, lookAheadMeters(36.0, adaptive = true), 1e-9)
    }

    @Test fun adaptive_clamps_low_for_slow_or_stopped() {
        assertEquals(150.0, lookAheadMeters(null, adaptive = true), 1e-9)
        assertEquals(150.0, lookAheadMeters(0.0, adaptive = true), 1e-9)
    }

    @Test fun adaptive_clamps_high_for_fast() {
        // 200 km/h ≈ 55.6 m/s × 20 s ≈ 1111 m → clamped to 800.
        assertEquals(800.0, lookAheadMeters(200.0, adaptive = true), 1e-9)
    }
}
