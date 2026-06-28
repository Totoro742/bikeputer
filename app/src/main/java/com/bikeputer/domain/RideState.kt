package com.bikeputer.domain

data class RideState(
    // instant
    val instantPowerW: Int?,
    val cadenceRpm: Int?,
    val heartRateBpm: Int?,
    val speedKmh: Double?,
    // derived power
    val avgPower3sW: Int?,
    val avgPower10sW: Int?,
    val normalizedPowerW: Int?,
    val sessionAvgPowerW: Int?,
    // ride stats
    val elapsedMs: Long,
    val distanceM: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGainM: Double,
    val avgHeartRateBpm: Int? = null,
    // zones (1-based index, null if unknown/unconfigured)
    val powerZone: Int?,
    val hrZone: Int?,
    // connectivity
    val powerConnected: Boolean,
    val hrConnected: Boolean,
    val gpsAvailable: Boolean,
    // last fix (for the map route) — null until GPS produces a position
    val latitude: Double? = null,
    val longitude: Double? = null,
    // GPS course over ground of the last moving fix; null when stationary/unknown
    val bearingDeg: Float? = null,
) {
    companion object {
        val EMPTY = RideState(
            instantPowerW = null, cadenceRpm = null, heartRateBpm = null, speedKmh = null,
            avgPower3sW = null, avgPower10sW = null, normalizedPowerW = null, sessionAvgPowerW = null,
            elapsedMs = 0L, distanceM = 0.0, avgSpeedKmh = 0.0, maxSpeedKmh = 0.0, elevationGainM = 0.0,
            powerZone = null, hrZone = null,
            powerConnected = false, hrConnected = false, gpsAvailable = false,
        )
    }
}
