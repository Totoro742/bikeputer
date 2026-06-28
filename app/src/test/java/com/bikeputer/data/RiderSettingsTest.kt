// app/src/test/java/com/bikeputer/data/RiderSettingsTest.kt
package com.bikeputer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RiderSettingsTest {
    @Test fun has_sensible_defaults() {
        val s = RiderSettings()
        assertFalse(s.useImperial)
        assertEquals(200, s.ftp)
        assertEquals(190, s.maxHr)
    }
}
