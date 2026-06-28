package com.bikeputer.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizedPowerTest {
    @Test fun null_before_any_window() {
        assertNull(NormalizedPower(windowSeconds = 2).value())
    }

    @Test fun constant_power_yields_that_power() {
        val np = NormalizedPower(windowSeconds = 2)
        for (s in 0..9) np.add(s * 1000L, 200)
        assertEquals(200, np.value())
    }

    @Test fun handles_variable_power_with_small_window() {
        // window=1s so each second's rolling mean == that second's value.
        // NP = (mean of v^4)^(1/4) over [100,300] -> ((100^4+300^4)/2)^0.25
        val np = NormalizedPower(windowSeconds = 1)
        np.add(0, 100)
        np.add(1000, 300)
        val expected = Math.pow((Math.pow(100.0, 4.0) + Math.pow(300.0, 4.0)) / 2.0, 0.25)
        assertEquals(Math.round(expected).toInt(), np.value())
    }

    @Test fun window_size_changes_result_on_variable_power() {
        val small = NormalizedPower(windowSeconds = 1)
        val large = NormalizedPower(windowSeconds = 5)
        val series = listOf(100, 100, 100, 100, 100, 500) // spike at the last second
        for (s in series.indices) {
            small.add(s * 1000L, series[s])
            large.add(s * 1000L, series[s])
        }
        org.junit.Assert.assertNotEquals(small.value(), large.value())
    }

    @Test fun value_persists_after_window_eviction_gap() {
        val np = NormalizedPower()
        np.add(0, 100)
        np.add(1000, 100)
        np.add(2000, 100)
        assertEquals(100, np.value()) // three full seconds at 100 W
        // A sample far in the future evicts the rolling window but the cumulative
        // fourth-power sum is retained, so value() stays defined (and non-null).
        np.add(100_000, 200)
        assertNotNull(np.value())
    }
}
