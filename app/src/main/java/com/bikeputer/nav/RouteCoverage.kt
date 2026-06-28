package com.bikeputer.nav

import com.bikeputer.domain.GeoMath
import com.bikeputer.domain.GeoPos

/**
 * Fraction (0f..1f) of [planned] vertices that some point on the [ridden] track
 * passed within [thresholdM]. The summary's "route followed" recap. Returns 0f
 * when either list is empty. O(planned × ridden) — fine at personal-route sizes.
 */
fun routeCoverage(
    planned: List<GeoPos>,
    ridden: List<GeoPos>,
    thresholdM: Double = DEFAULT_OFF_ROUTE_THRESHOLD_M.toDouble(),
): Float {
    if (planned.isEmpty() || ridden.isEmpty()) return 0f
    val covered = planned.count { p ->
        ridden.any { GeoMath.haversineMeters(p, it) <= thresholdM }
    }
    return covered.toFloat() / planned.size
}
