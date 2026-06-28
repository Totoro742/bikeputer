package com.bikeputer.metrics

class SessionStats {
    private var powerSum = 0L
    private var powerCount = 0L
    private var speedSum = 0.0
    private var speedCount = 0L
    private var maxSpeed = 0.0
    private var hrSum = 0L
    private var hrCount = 0L

    fun addPower(w: Int) { powerSum += w; powerCount++ }

    fun addHr(bpm: Int) { hrSum += bpm; hrCount++ }

    fun addSpeedKmh(v: Double) {
        speedSum += v; speedCount++
        if (v > maxSpeed) maxSpeed = v
    }

    fun avgPower(): Int? = if (powerCount == 0L) null else (powerSum / powerCount).toInt()
    fun maxSpeedKmh(): Double = maxSpeed
    fun avgSpeedKmh(): Double = if (speedCount == 0L) 0.0 else speedSum / speedCount
    fun avgHr(): Int? = if (hrCount == 0L) null else (hrSum / hrCount).toInt()
}
