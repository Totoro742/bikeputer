package com.bikeputer.ble

data class CyclingPowerMeasurement(
    val powerW: Int,
    val crankRevs: Int?,
    val crankEventTime1024: Int?,
)

object CyclingPowerParser {
    private fun u8(b: ByteArray, i: Int) = b[i].toInt() and 0xFF
    private fun u16(b: ByteArray, i: Int) = u8(b, i) or (u8(b, i + 1) shl 8)
    private fun s16(b: ByteArray, i: Int): Int {
        val v = u16(b, i)
        return if (v >= 0x8000) v - 0x10000 else v
    }

    fun parse(bytes: ByteArray): CyclingPowerMeasurement {
        val flags = u16(bytes, 0)
        val powerW = s16(bytes, 2)
        var offset = 4
        if (flags and 0x01 != 0) offset += 1   // pedal power balance
        if (flags and 0x04 != 0) offset += 2   // accumulated torque
        if (flags and 0x10 != 0) offset += 6   // wheel revolution data
        var crankRevs: Int? = null
        var crankTime: Int? = null
        if (flags and 0x20 != 0) {              // crank revolution data
            crankRevs = u16(bytes, offset)
            crankTime = u16(bytes, offset + 2)
        }
        return CyclingPowerMeasurement(powerW, crankRevs, crankTime)
    }
}
