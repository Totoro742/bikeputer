package com.bikeputer.metrics

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import com.bikeputer.domain.RideState
import kotlin.math.roundToInt

class MetricsEngine(
    private val powerZones: ZoneModel?,
    private val hrZones: ZoneModel?,
) {
    private val avg3s = RollingAverage(3000)
    private val avg10s = RollingAverage(10000)
    private val np = NormalizedPower()
    private val session = SessionStats()
    private val distance = DistanceIntegrator()

    private var lastPower: Int? = null
    private var lastCadence: Int? = null
    private var lastHr: Int? = null
    private var lastSpeedKmh: Double? = null
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var lastBearing: Float? = null

    fun onPower(s: PowerSample) {
        lastPower = s.watts
        lastCadence = s.cadenceRpm
        avg3s.add(s.timestampMs, s.watts.toDouble())
        avg10s.add(s.timestampMs, s.watts.toDouble())
        np.add(s.timestampMs, s.watts)
        session.addPower(s.watts)
    }

    fun onHeartRate(s: HeartRateSample) {
        lastHr = s.bpm
        session.addHr(s.bpm)
    }

    fun onLocation(s: LocationSample) {
        val kmh = s.speedMps * 3.6
        lastSpeedKmh = kmh
        lastLat = s.lat
        lastLng = s.lng
        lastBearing = s.bearingDeg
        session.addSpeedKmh(kmh)
        distance.add(s)
    }

    fun snapshot(
        elapsedMs: Long,
        powerConnected: Boolean,
        hrConnected: Boolean,
        gpsAvailable: Boolean,
    ): RideState = RideState(
        instantPowerW = lastPower,
        cadenceRpm = lastCadence,
        heartRateBpm = lastHr,
        avgHeartRateBpm = session.avgHr(),
        speedKmh = lastSpeedKmh,
        avgPower3sW = avg3s.average()?.roundToInt(),
        avgPower10sW = avg10s.average()?.roundToInt(),
        normalizedPowerW = np.value(),
        sessionAvgPowerW = session.avgPower(),
        elapsedMs = elapsedMs,
        distanceM = distance.distanceM(),
        avgSpeedKmh = session.avgSpeedKmh(),
        maxSpeedKmh = session.maxSpeedKmh(),
        elevationGainM = distance.elevationGainM(),
        powerZone = lastPower?.let { p -> powerZones?.zoneOf(p) },
        hrZone = lastHr?.let { h -> hrZones?.zoneOf(h) },
        powerConnected = powerConnected,
        hrConnected = hrConnected,
        gpsAvailable = gpsAvailable,
        latitude = lastLat,
        longitude = lastLng,
        bearingDeg = lastBearing,
    )
}
