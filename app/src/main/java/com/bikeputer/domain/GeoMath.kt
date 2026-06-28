package com.bikeputer.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Foot of the perpendicular from a point to a segment, clamped to its endpoints. */
data class SegmentProjection(val point: GeoPos, val t: Double, val distanceM: Double)

/** Pure geo helpers (no Android types). Distances in metres. */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val DEG = PI / 180.0

    fun haversineMeters(a: GeoPos, b: GeoPos): Double {
        val dLat = (b.lat - a.lat) * DEG
        val dLon = (b.lng - a.lng) * DEG
        val rLat1 = a.lat * DEG
        val rLat2 = b.lat * DEG
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(h), sqrt(1 - h))
    }

    /**
     * True initial bearing from [a] to [b], in degrees normalised to 0..360
     * (N=0, E=90, S=180, W=270).
     */
    fun initialBearing(a: GeoPos, b: GeoPos): Double {
        val rLat1 = a.lat * DEG
        val rLat2 = b.lat * DEG
        val dLon = (b.lng - a.lng) * DEG
        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val deg = atan2(y, x) / DEG
        return (deg + 360.0) % 360.0
    }

    /**
     * Projects [p] onto segment [a]→[b] with a local equirectangular approximation
     * (metres per degree, longitude scaled by cos(lat)) to find the clamped along
     * fraction t, then reports the true great-circle cross-track distance to the foot.
     */
    fun nearestPointOnSegment(p: GeoPos, a: GeoPos, b: GeoPos): SegmentProjection {
        val latRef = (a.lat + b.lat) / 2.0 * DEG
        val kx = EARTH_RADIUS_M * DEG * cos(latRef) // metres per degree of longitude
        val ky = EARTH_RADIUS_M * DEG               // metres per degree of latitude
        val ax = a.lng * kx; val ay = a.lat * ky
        val bx = b.lng * kx; val by = b.lat * ky
        val px = p.lng * kx; val py = p.lat * ky
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0
                else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
        val foot = GeoPos(a.lat + t * (b.lat - a.lat), a.lng + t * (b.lng - a.lng))
        return SegmentProjection(foot, t, haversineMeters(p, foot))
    }
}
