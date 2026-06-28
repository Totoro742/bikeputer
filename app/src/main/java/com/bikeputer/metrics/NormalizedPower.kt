package com.bikeputer.metrics

import kotlin.math.pow
import kotlin.math.roundToInt

class NormalizedPower(private val windowSeconds: Int = 30) {
    private val window = RollingAverage(windowSeconds * 1000L - 1)
    private var fourthPowerSum = 0.0
    private var count = 0L
    private var lastSecond: Long? = null

    fun add(timestampMs: Long, watts: Int) {
        window.add(timestampMs, watts.toDouble())
        val second = timestampMs / 1000L
        if (second != lastSecond) {
            lastSecond = second
            val avg = window.average() ?: return
            fourthPowerSum += avg * avg * avg * avg
            count++
        }
    }

    fun value(): Int? {
        if (count == 0L) return null
        val mean = fourthPowerSum / count
        return mean.pow(0.25).roundToInt()
    }
}
