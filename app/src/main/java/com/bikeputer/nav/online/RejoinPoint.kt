package com.bikeputer.nav.online

import com.bikeputer.domain.GeoMath
import com.bikeputer.domain.GeoPos

/**
 * The point on [planned] to route the rider back to after going off-route: the nearest point
 * to [current] whose distance-along lies in `[departureAlong, departureAlong + lookAheadCapM]`
 * (ahead of where the rider left the route, never behind, never further than the cap).
 * Falls back to the route's last vertex when nothing qualifies (rider past the route, or a
 * degenerate route).
 */
fun rejoinTarget(
    planned: List<GeoPos>,
    current: GeoPos,
    departureAlong: Double,
    lookAheadCapM: Double,
): GeoPos {
    if (planned.size < 2) return planned.lastOrNull() ?: current
    val cum = DoubleArray(planned.size)
    for (i in 1 until planned.size) cum[i] = cum[i - 1] + GeoMath.haversineMeters(planned[i - 1], planned[i])
    val maxAlong = departureAlong + lookAheadCapM
    var best: GeoPos? = null
    var bestDist = Double.MAX_VALUE
    for (i in 1 until planned.size) {
        val proj = GeoMath.nearestPointOnSegment(current, planned[i - 1], planned[i])
        val along = cum[i - 1] + proj.t * (cum[i] - cum[i - 1])
        if (along < departureAlong || along > maxAlong) continue
        if (proj.distanceM < bestDist) {
            bestDist = proj.distanceM
            best = proj.point
        }
    }
    return best ?: planned.last()
}
