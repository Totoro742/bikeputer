package com.bikeputer.ride

import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.LocationSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RideSessionRouteTest {
    private fun session(scope: CoroutineScope, locations: List<LocationSample>) = RideSession(
        RideRepository(
            power = FakePowerSource(emptyList()),
            heartRate = FakeHeartRateSource(emptyList()),
            location = FakeLocationProvider(locations),
            powerZones = null,
            hrZones = null,
        ),
        SessionTimer(),
        scope,
        now = { 0L },
    )

    @Test fun accumulates_distinct_points_while_recording() = runTest {
        val s = session(backgroundScope, listOf(
            LocationSample(0, 1.0, 2.0, 0.0, 0f),
            LocationSample(0, 1.0, 2.0, 0.0, 0f),   // duplicate -> ignored
            LocationSample(0, 1.1, 2.1, 0.0, 0f),
        ))
        s.start()
        runCurrent()
        assertEquals(listOf(GeoPos(1.0, 2.0), GeoPos(1.1, 2.1)), s.route.value)
    }

    @Test fun does_not_grow_while_paused() = runTest {
        val s = session(backgroundScope, listOf(
            LocationSample(0, 1.0, 2.0, 0.0, 0f),
            LocationSample(0, 1.1, 2.1, 0.0, 0f),
        ))
        s.start()
        s.pause()
        runCurrent()
        assertEquals(emptyList<GeoPos>(), s.route.value)
    }
}
