package com.bikeputer.metrics

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsEngineTest {
    @Test fun snapshot_reflects_latest_instant_values_and_zones() {
        val engine = MetricsEngine(
            powerZones = Zones.powerZonesFromFtp(200),
            hrZones = Zones.hrZonesFromMax(190),
        )
        engine.onPower(PowerSample(0, 100, 90))
        engine.onPower(PowerSample(1000, 301, 95))
        engine.onHeartRate(HeartRateSample(1000, 150))
        engine.onLocation(LocationSample(1000, 0.0, 0.0, 10.0, 10.0f)) // 36 km/h

        val s = engine.snapshot(1000, powerConnected = true, hrConnected = true, gpsAvailable = true)
        assertEquals(301, s.instantPowerW)
        assertEquals(95, s.cadenceRpm)
        assertEquals(150, s.heartRateBpm)
        assertEquals(36.0, s.speedKmh!!, 0.01)
        assertEquals(200, s.sessionAvgPowerW) // (100+301)/2 = 200 (integer division)
        assertEquals(7, s.powerZone)          // 301 > 1.5*200
        assertEquals(3, s.hrZone)             // 150 in (0.7*190=133, 0.8*190=152]
        assertEquals(true, s.powerConnected)
    }

    @Test fun snapshot_carries_bearing_from_location_sample() {
        val engine = MetricsEngine(powerZones = null, hrZones = null)
        engine.onLocation(LocationSample(1000, 50.0, 19.0, 200.0, 8f, bearingDeg = 137.5f))

        val s = engine.snapshot(1000, powerConnected = false, hrConnected = false, gpsAvailable = true)
        assertEquals(137.5f, s.bearingDeg!!, 0.01f)
    }

    @Test fun snapshot_bearing_is_null_when_sample_has_no_bearing() {
        val engine = MetricsEngine(powerZones = null, hrZones = null)
        engine.onLocation(LocationSample(1000, 50.0, 19.0, 200.0, 8f, bearingDeg = null))

        val s = engine.snapshot(1000, powerConnected = false, hrConnected = false, gpsAvailable = true)
        assertEquals(null, s.bearingDeg)
    }
}
