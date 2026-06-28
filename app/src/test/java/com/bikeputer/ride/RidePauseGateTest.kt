package com.bikeputer.ride

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import com.bikeputer.domain.RidePhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RidePauseGateTest {
    @Test fun paused_phase_does_not_accumulate() = runTest {
        val repo = RideRepository(
            power = FakePowerSource(listOf(PowerSample(0, 200, 90))),
            heartRate = FakeHeartRateSource(listOf(HeartRateSample(0, 150))),
            location = FakeLocationProvider(listOf(
                LocationSample(0, 0.0, 0.0, 0.0, 0f),
                LocationSample(0, 0.0, 0.001, 0.0, 0f),
            )),
            powerZones = null,
            hrZones = null,
        )
        val phase = MutableStateFlow(RidePhase.Paused)
        val last = repo.rideState(flowOf(0L), phase).last()
        assertEquals(0.0, last.distanceM, 0.0)
        assertEquals(null, last.sessionAvgPowerW)
    }

    @Test fun resume_after_pause_resumes_accumulation() = runTest {
        val phase = MutableStateFlow(RidePhase.Recording)
        val repo = RideRepository(
            power = FakePowerSource(
                listOf(PowerSample(0, 100, 90), PowerSample(0, 500, 90), PowerSample(0, 100, 90)),
                intervalMs = 1000,
            ),
            heartRate = FakeHeartRateSource(emptyList()),
            location = FakeLocationProvider(emptyList()),
            powerZones = null,
            hrZones = null,
        )
        launch { delay(1500); phase.value = RidePhase.Paused }      // before t=2000 sample
        launch { delay(2500); phase.value = RidePhase.Recording }   // before t=3000 sample
        val last = repo.rideState(flowOf(0L), phase).last()
        // samples counted: 100 (t=1000) + 100 (t=3000); 500 (t=2000) skipped while paused
        assertEquals(100, last.sessionAvgPowerW)
    }
}
