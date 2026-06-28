package com.bikeputer.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateParserTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test fun parses_uint8_heart_rate() {
        assertEquals(72, HeartRateParser.parse(bytes(0x00, 72)))
    }

    @Test fun parses_uint16_heart_rate() {
        // flags=0x01 -> uint16; value=300 (0x012C)
        assertEquals(300, HeartRateParser.parse(bytes(0x01, 0x2C, 0x01)))
    }
}
