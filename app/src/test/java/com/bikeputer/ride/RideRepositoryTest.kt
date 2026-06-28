// app/src/test/java/com/bikeputer/ride/RideRepositoryTest.kt
package com.bikeputer.ride

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import com.bikeputer.metrics.Zones
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RideRepositoryTest {
    @Test fun emits_ridestate_combining_all_sources() = runTest {
        val repo = RideRepository(
            power = FakePowerSource(listOf(PowerSample(0, 200, 90))),
            heartRate = FakeHeartRateSource(listOf(HeartRateSample(0, 140))),
            location = FakeLocationProvider(listOf(LocationSample(0, 0.0, 0.0, 0.0, 5.0f))),
            powerZones = Zones.powerZonesFromFtp(200),
            hrZones = Zones.hrZonesFromMax(190),
        )
        val last = repo.rideState(flowOf(0L, 1000L)).last()
        assertEquals(200, last.instantPowerW)
        assertEquals(140, last.heartRateBpm)
        assertEquals(18.0, last.speedKmh!!, 0.01) // 5 m/s
        assertEquals(true, last.powerConnected)
    }

    @Test fun final_snapshot_accumulates_power_and_distance_across_sources() = runTest {
        val repo = RideRepository(
            power = FakePowerSource(listOf(PowerSample(0, 100, 90), PowerSample(1000, 300, 90))),
            heartRate = FakeHeartRateSource(listOf(HeartRateSample(0, 140))),
            // (0,0) -> (0, 0.001 deg lng) at the equator ~= 111.19 m apart.
            location = FakeLocationProvider(listOf(
                LocationSample(0, 0.0, 0.0, 0.0, 0f),
                LocationSample(1000, 0.0, 0.001, 0.0, 0f),
            )),
            powerZones = Zones.powerZonesFromFtp(200),
            hrZones = Zones.hrZonesFromMax(190),
        )
        val last = repo.rideState(flowOf(0L)).last()
        // session-average power == (100 + 300) / 2, regardless of merge order.
        assertEquals(200, last.sessionAvgPowerW)
        assertEquals(111.19, last.distanceM, 0.5)
    }
}
