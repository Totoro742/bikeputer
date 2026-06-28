// app/src/test/java/com/bikeputer/ble/CadenceCalculatorTest.kt
package com.bikeputer.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CadenceCalculatorTest {
    private fun m(revs: Int?, time: Int?) = CyclingPowerMeasurement(0, revs, time)

    @Test fun first_reading_returns_null() {
        assertNull(CadenceCalculator().update(m(10, 1024)))
    }

    @Test fun computes_sixty_rpm_for_one_rev_per_second() {
        val c = CadenceCalculator()
        c.update(m(10, 0))
        assertEquals(60, c.update(m(11, 1024)))
    }

    @Test fun handles_event_time_wraparound() {
        val c = CadenceCalculator()
        c.update(m(20, 65000))
        // wrap: (1000 - 65000) & 0xFFFF = 1536 ticks for 1 rev
        // rpm = 1 * 60 * 1024 / 1536 = 40
        assertEquals(40, c.update(m(21, 1000)))
    }

    @Test fun absent_crank_data_returns_null() {
        assertNull(CadenceCalculator().update(m(null, null)))
    }

    @Test fun does_not_overflow_int_on_large_rev_delta() {
        val calc = CadenceCalculator()
        // First update only establishes the baseline -> null.
        assertNull(calc.update(CyclingPowerMeasurement(powerW = 0, crankRevs = 0, crankEventTime1024 = 0)))
        // deltaRevs = 60000, deltaTicks = 1024 (1s). 60000 * 60 * 1024 = 3_686_400_000,
        // which overflows a signed Int; with a Long intermediate the result is correct.
        val cadence = calc.update(CyclingPowerMeasurement(powerW = 0, crankRevs = 60000, crankEventTime1024 = 1024))
        assertEquals(3_600_000, cadence)
    }
}
