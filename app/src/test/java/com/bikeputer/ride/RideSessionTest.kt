package com.bikeputer.ride

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import com.bikeputer.metrics.Zones
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RideSessionTest {
    private fun repo() = RideRepository(
        power = FakePowerSource(listOf(PowerSample(0, 200, 90))),
        heartRate = FakeHeartRateSource(listOf(HeartRateSample(0, 140))),
        location = FakeLocationProvider(listOf(LocationSample(0, 0.0, 0.0, 0.0, 5f))),
        powerZones = Zones.powerZonesFromFtp(200),
        hrZones = Zones.hrZonesFromMax(190),
    )

    @Test fun exposes_ride_state_from_sources() = runTest {
        val session = RideSession(repo(), SessionTimer(), backgroundScope, now = { 0L })
        session.start()
        runCurrent()
        assertEquals(200, session.state.value.instantPowerW)
        assertEquals(140, session.state.value.heartRateBpm)
    }

    @Test fun starts_empty_before_any_sample_processed() = runTest {
        val session = RideSession(repo(), SessionTimer(), backgroundScope, now = { 0L })
        // Eagerly seeded with RideState.EMPTY; value is available immediately.
        assertEquals(null, session.state.value.instantPowerW)
    }
}
