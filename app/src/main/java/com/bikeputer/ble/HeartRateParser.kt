package com.bikeputer.ble

object HeartRateParser {
    fun parse(bytes: ByteArray): Int {
        val flags = bytes[0].toInt() and 0xFF
        return if (flags and 0x01 == 0) {
            bytes[1].toInt() and 0xFF
        } else {
            (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        }
    }
}
