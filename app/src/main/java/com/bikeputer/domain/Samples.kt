package com.bikeputer.domain

data class PowerSample(val timestampMs: Long, val watts: Int, val cadenceRpm: Int?)
data class HeartRateSample(val timestampMs: Long, val bpm: Int)
data class LocationSample(
    val timestampMs: Long,
    val lat: Double,
    val lng: Double,
    val altitudeM: Double,
    val speedMps: Float,
    val bearingDeg: Float? = null,
)
