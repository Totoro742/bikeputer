// app/src/test/java/com/bikeputer/ride/FakeSourcesTest.kt
package com.bikeputer.ride

import com.bikeputer.domain.PowerSample
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSourcesTest {
    @Test fun fake_power_source_emits_all_samples_and_reports_connected() = runTest {
        val src = FakePowerSource(listOf(PowerSample(0, 100, 80), PowerSample(1000, 110, 81)))
        val out = src.samples.toList()
        assertEquals(2, out.size)
        assertEquals(110, out[1].watts)
        assertEquals(true, src.connected.value)
    }
}
