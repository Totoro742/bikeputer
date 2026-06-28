package com.bikeputer.ble

class CadenceCalculator {
    private var lastRevs: Int? = null
    private var lastTime: Int? = null

    fun update(m: CyclingPowerMeasurement): Int? {
        val revs = m.crankRevs ?: return null
        val time = m.crankEventTime1024 ?: return null
        val pr = lastRevs
        val pt = lastTime
        lastRevs = revs
        lastTime = time
        if (pr == null || pt == null) return null
        val deltaRevs = (revs - pr) and 0xFFFF
        val deltaTicks = (time - pt) and 0xFFFF
        if (deltaTicks == 0) return null
        return ((deltaRevs.toLong() * 60 * 1024) / deltaTicks).toInt()
    }
}
