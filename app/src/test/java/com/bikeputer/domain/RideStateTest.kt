package com.bikeputer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideStateTest {
    @Test fun empty_state_has_null_instant_values_and_zero_session() {
        val s = RideState.EMPTY
        assertNull(s.instantPowerW)
        assertNull(s.heartRateBpm)
        assertEquals(0.0, s.distanceM, 0.0)
        assertEquals(0L, s.elapsedMs)
        assertEquals(false, s.powerConnected)
    }
}
