// app/src/test/java/com/bikeputer/ble/CyclingPowerParserTest.kt
package com.bikeputer.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CyclingPowerParserTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test fun parses_power_only_when_no_optional_flags() {
        // flags=0x0000, power=250 (0x00FA)
        val m = CyclingPowerParser.parse(bytes(0x00, 0x00, 0xFA, 0x00))
        assertEquals(250, m.powerW)
        assertNull(m.crankRevs)
    }

    @Test fun parses_crank_rev_data_when_flag_set() {
        // flags=0x0020 (crank rev present), power=200 (0xC8),
        // crankRevs=10 (0x000A), crankEventTime=1024 (0x0400)
        val m = CyclingPowerParser.parse(
            bytes(0x20, 0x00, 0xC8, 0x00, 0x0A, 0x00, 0x00, 0x04)
        )
        assertEquals(200, m.powerW)
        assertEquals(10, m.crankRevs)
        assertEquals(1024, m.crankEventTime1024)
    }

    @Test fun skips_earlier_optional_fields_to_locate_crank_data() {
        // flags=0x0021: bit0 (balance, 1 byte) + bit5 (crank rev).
        // power=100 (0x64), balance=0x32, crankRevs=5 (0x0005), time=512 (0x0200)
        val m = CyclingPowerParser.parse(
            bytes(0x21, 0x00, 0x64, 0x00, 0x32, 0x05, 0x00, 0x00, 0x02)
        )
        assertEquals(100, m.powerW)
        assertEquals(5, m.crankRevs)
        assertEquals(512, m.crankEventTime1024)
    }
}
