package com.bikeputer.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RollingAverageTest {
    @Test fun empty_returns_null() {
        assertNull(RollingAverage(3000).average())
    }

    @Test fun averages_values_within_window() {
        val ra = RollingAverage(3000)
        ra.add(0, 100.0)
        ra.add(1000, 200.0)
        ra.add(2000, 300.0)
        assertEquals(200.0, ra.average()!!, 0.0001)
    }

    @Test fun evicts_samples_older_than_window() {
        val ra = RollingAverage(3000)
        ra.add(0, 100.0)      // will be evicted once latest is 4000
        ra.add(2000, 200.0)
        ra.add(4000, 300.0)   // window now [1000,4000] -> keeps 2000 and 4000
        assertEquals(250.0, ra.average()!!, 0.0001)
    }

    @Test fun gap_longer_than_window_evicts_all_but_newest() {
        val ra = RollingAverage(3000)
        ra.add(0, 100.0)
        ra.add(10_000, 300.0) // 10s gap > 3s window -> only the 300 sample remains
        assertEquals(300.0, ra.average()!!, 0.0001)
    }
}
