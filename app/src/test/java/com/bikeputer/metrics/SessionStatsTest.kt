// app/src/test/java/com/bikeputer/metrics/SessionStatsTest.kt
package com.bikeputer.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStatsTest {
    @Test fun no_power_samples_returns_null_avg() {
        assertNull(SessionStats().avgPower())
    }

    @Test fun tracks_avg_power_and_speed_max() {
        val s = SessionStats()
        s.addPower(100); s.addPower(200); s.addPower(300)
        s.addSpeedKmh(10.0); s.addSpeedKmh(40.0); s.addSpeedKmh(25.0)
        assertEquals(200, s.avgPower())
        assertEquals(40.0, s.maxSpeedKmh(), 0.0001)
        assertEquals(25.0, s.avgSpeedKmh(), 0.0001)
    }

    @Test fun avg_hr_is_null_before_any_sample() {
        assertEquals(null, SessionStats().avgHr())
    }

    @Test fun avg_hr_is_running_mean() {
        val s = SessionStats()
        s.addHr(140); s.addHr(160)
        assertEquals(150, s.avgHr())
    }
}
