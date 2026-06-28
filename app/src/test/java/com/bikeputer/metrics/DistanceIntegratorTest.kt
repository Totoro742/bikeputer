// app/src/test/java/com/bikeputer/metrics/DistanceIntegratorTest.kt
package com.bikeputer.metrics

import com.bikeputer.domain.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceIntegratorTest {
    private fun loc(t: Long, lat: Double, lng: Double, alt: Double) =
        LocationSample(t, lat, lng, alt, 0f)

    @Test fun single_point_has_zero_distance() {
        val d = DistanceIntegrator()
        d.add(loc(0, 50.0, 19.0, 200.0))
        assertEquals(0.0, d.distanceM(), 0.0001)
    }

    @Test fun accumulates_haversine_distance() {
        val d = DistanceIntegrator()
        d.add(loc(0, 0.0, 0.0, 0.0))
        d.add(loc(1000, 0.0, 1.0, 0.0)) // 1 degree lng at equator ~111.19 km
        assertEquals(111195.0, d.distanceM(), 500.0)
    }

    @Test fun elevation_gain_ignores_noise_below_threshold_and_descents() {
        val d = DistanceIntegrator(elevationThresholdM = 1.0)
        d.add(loc(0, 0.0, 0.0, 100.0))
        d.add(loc(1000, 0.0, 0.0, 100.4)) // noise, below threshold
        d.add(loc(2000, 0.0, 0.0, 102.0)) // +2 from committed 100 -> gain 2
        d.add(loc(3000, 0.0, 0.0, 101.0)) // descent -> ignored
        assertTrue(d.elevationGainM() in 1.9..2.1)
    }

    @Test fun gradual_ascent_crosses_threshold_cumulatively() {
        val di = DistanceIntegrator(elevationThresholdM = 1.0)
        // Same lat/lng so distance stays 0; altitude rises in 0.4 m steps.
        // committedAlt starts at 100.0; delta is measured from the commit, not the
        // previous sample, so 0.4 m steps accumulate until they cross 1.0 m.
        di.add(LocationSample(0, 0.0, 0.0, 100.0, 0f))
        di.add(LocationSample(1000, 0.0, 0.0, 100.4, 0f)) // delta 0.4 -> no gain
        di.add(LocationSample(2000, 0.0, 0.0, 100.8, 0f)) // delta 0.8 -> no gain
        di.add(LocationSample(3000, 0.0, 0.0, 101.2, 0f)) // delta 1.2 -> gain += 1.2
        assertEquals(1.2, di.elevationGainM(), 1e-6)
        assertEquals(0.0, di.distanceM(), 1e-6)
    }
}
