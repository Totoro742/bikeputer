package com.bikeputer.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsStalenessTrackerTest {
    @Test fun unavailable_before_any_sample() {
        assertFalse(GpsStalenessTracker(staleMs = 3000).isAvailable(0))
    }

    @Test fun available_immediately_after_a_sample() {
        val t = GpsStalenessTracker(staleMs = 3000)
        t.onSample(1000)
        assertTrue(t.isAvailable(1000))
    }

    @Test fun available_at_exactly_the_threshold() {
        val t = GpsStalenessTracker(staleMs = 3000)
        t.onSample(1000)
        assertTrue(t.isAvailable(4000)) // 3000ms gap == staleMs, still fresh
    }

    @Test fun unavailable_once_past_the_threshold() {
        val t = GpsStalenessTracker(staleMs = 3000)
        t.onSample(1000)
        assertFalse(t.isAvailable(4001)) // 3001ms gap > staleMs
    }

    @Test fun a_new_sample_restores_availability() {
        val t = GpsStalenessTracker(staleMs = 3000)
        t.onSample(1000)
        assertFalse(t.isAvailable(5000))
        t.onSample(5000)
        assertTrue(t.isAvailable(5000))
    }
}
