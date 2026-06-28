package com.bikeputer.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RideServiceDecisionTest {
    @Test fun stop_action_returns_stop() {
        assertEquals(
            ServiceStep.Stop,
            decideStep(RideService.ACTION_STOP, hasSession = true, starting = false),
        )
    }

    @Test fun start_with_no_session_builds() {
        assertEquals(
            ServiceStep.StartAndBuild,
            decideStep(RideService.ACTION_START, hasSession = false, starting = false),
        )
    }

    @Test fun start_with_existing_session_only_foregrounds() {
        assertEquals(
            ServiceStep.StartOnly,
            decideStep(RideService.ACTION_START, hasSession = true, starting = false),
        )
    }

    @Test fun start_while_already_starting_only_foregrounds() {
        assertEquals(
            ServiceStep.StartOnly,
            decideStep(null, hasSession = false, starting = true),
        )
    }
}
