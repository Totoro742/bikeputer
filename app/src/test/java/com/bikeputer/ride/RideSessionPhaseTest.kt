package com.bikeputer.ride

import com.bikeputer.domain.RidePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RideSessionPhaseTest {
    private fun session(scope: CoroutineScope) = RideSession(
        RideRepository(
            power = FakePowerSource(emptyList()),
            heartRate = FakeHeartRateSource(emptyList()),
            location = FakeLocationProvider(emptyList()),
            powerZones = null,
            hrZones = null,
        ),
        SessionTimer(),
        scope,
        now = { 0L },
    )

    @Test fun starts_recording() = runTest {
        assertEquals(RidePhase.Recording, session(backgroundScope).phase.value)
    }

    @Test fun pause_then_resume() = runTest {
        val s = session(backgroundScope)
        s.start(); s.pause()
        assertEquals(RidePhase.Paused, s.phase.value)
        s.resume()
        assertEquals(RidePhase.Recording, s.phase.value)
    }

    @Test fun resume_ignored_when_recording() = runTest {
        val s = session(backgroundScope)
        s.start()
        s.resume() // no-op: already Recording
        assertEquals(RidePhase.Recording, s.phase.value)
    }

    @Test fun finish_is_terminal() = runTest {
        val s = session(backgroundScope)
        s.finish()
        assertEquals(RidePhase.Finished, s.phase.value)
        s.start(); s.pause(); s.resume()
        assertEquals(RidePhase.Finished, s.phase.value)
    }
}
