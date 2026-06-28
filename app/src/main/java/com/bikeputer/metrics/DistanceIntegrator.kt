package com.bikeputer.metrics

import com.bikeputer.domain.GeoMath
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.LocationSample

class DistanceIntegrator(private val elevationThresholdM: Double = 1.0) {
    private var distance = 0.0
    private var gain = 0.0
    private var prev: LocationSample? = null
    private var committedAlt: Double? = null

    fun add(sample: LocationSample) {
        prev?.let { distance += GeoMath.haversineMeters(GeoPos(it.lat, it.lng), GeoPos(sample.lat, sample.lng)) }
        prev = sample

        val committed = committedAlt
        if (committed == null) {
            committedAlt = sample.altitudeM
        } else {
            val delta = sample.altitudeM - committed
            if (delta >= elevationThresholdM) {
                gain += delta
                committedAlt = sample.altitudeM
            } else if (delta <= -elevationThresholdM) {
                committedAlt = sample.altitudeM
            }
        }
    }

    fun distanceM(): Double = distance
    fun elevationGainM(): Double = gain

}
