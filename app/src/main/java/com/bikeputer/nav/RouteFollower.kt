package com.bikeputer.nav

import com.bikeputer.domain.GeoMath
import com.bikeputer.domain.GeoPos

/** Default off-route threshold (metres); also the default for new nav params. */
const val DEFAULT_OFF_ROUTE_THRESHOLD_M = 35

data class NavFix(
    val offRouteMeters: Double,   // distance from current to the nearest point on the line
    val remainingMeters: Double,  // route length from the nearest point to the end
    val fractionComplete: Float,  // 0f..1f, = distanceAlong / total
    val distanceAlong: Double,    // metres from the route start to the nearest point
)

/**
 * Projects the live position onto an armed route's polyline. Built once per ride.
 * [project] performs a pure global nearest-point search, used for one-shot / batch
 * projection (e.g. mapping maneuver points) and order-independent callers.
 * [track] is the forward-biased live projection: it keeps a progress cursor and matches
 * within a distance-along window, so a route passing near itself stays on the correct pass.
 * It falls back to global re-acquire on the first fix, after a long GPS gap, or when the
 * in-window match is off-route.
 */
class RouteFollower(
    private val planned: List<GeoPos>,
    private val backSlackM: Double = 30.0,
    private val forwardReachM: Double = 150.0,
) {
    private val cumulative = DoubleArray(planned.size) // distance from start to vertex i
    private val totalMeters: Double

    /** Rider's last matched distance-along (metres); null until the first [track] fix. Constructing a new [RouteFollower] is the only way to reset. */
    private var cursorAlong: Double? = null

    init {
        var acc = 0.0
        for (i in 1 until planned.size) {
            acc += GeoMath.haversineMeters(planned[i - 1], planned[i])
            cumulative[i] = acc
        }
        totalMeters = acc
    }

    /** Nearest point on the route to [current]: the segment it falls on, the foot, the
     *  distance along the route to the foot, and the cross-track distance. */
    private data class Nearest(
        val segIndex: Int,       // foot is on segment [segIndex-1, segIndex]; planned[segIndex] is the next vertex ahead
        val foot: GeoPos,
        val distanceAlong: Double,
        val crossTrackM: Double,
    )

    private fun nearest(current: GeoPos, window: ClosedFloatingPointRange<Double>? = null): Nearest? {
        if (planned.size < 2 || totalMeters <= 0.0) return null
        var best: Nearest? = null
        for (i in 1 until planned.size) {
            val proj = GeoMath.nearestPointOnSegment(current, planned[i - 1], planned[i])
            val along = cumulative[i - 1] + proj.t * (cumulative[i] - cumulative[i - 1])
            // Skip segment if foot is outside window; per-segment granularity assumes reasonably dense vertices.
            if (window != null && along !in window) continue
            if (best == null || proj.distanceM < best.crossTrackM) {
                best = Nearest(i, proj.point, along, proj.distanceM)
            }
        }
        return best
    }

    private fun toFix(n: Nearest): NavFix {
        val remaining = (totalMeters - n.distanceAlong).coerceAtLeast(0.0)
        val fraction = (n.distanceAlong / totalMeters).toFloat().coerceIn(0f, 1f)
        return NavFix(n.crossTrackM, remaining, fraction, n.distanceAlong)
    }

    fun project(current: GeoPos): NavFix? {
        val n = nearest(current) ?: return null
        return toFix(n)
    }

    /**
     * Distance-along of the route's first close approach to [point] at or beyond [fromAlong].
     * Scanning forward from the cursor keeps ordered points — e.g. routing-engine maneuvers,
     * which arrive in route order — on the correct pass where the route revisits itself.
     *
     * We return the route's *first prominent approach* to [point]: the cross-track distance is
     * tracked forward, and the closest-so-far along is committed once the route has since climbed
     * more than [onRouteThresholdM] past it. Using the first approach (rather than the globally
     * nearest, or the first reading under an absolute threshold) matters on a closed loop whose
     * start == end: the closing leg returns through the start area, so where the GPX diverges from
     * the snapped road a *later* pass can sit closer to an early turn than its own (first) pass —
     * an absolute-threshold rule would snap that turn onto the finish. Committing the first
     * prominent trough keeps it early, while the [fromAlong] cursor still drops earlier passes a
     * later maneuver has already moved past. Threading the returned value back as [fromAlong]
     * yields monotonic, non-decreasing projections. Falls back to the closest forward segment when
     * the route only ever recedes from [point], and to [fromAlong] for a degenerate route.
     */
    fun projectForwardAlong(point: GeoPos, fromAlong: Double, onRouteThresholdM: Double = 35.0): Double {
        if (planned.size < 2 || totalMeters <= 0.0) return fromAlong
        var minDist = Double.POSITIVE_INFINITY
        var minAlong = fromAlong
        var found = false
        var minIsTrough = false   // the closest-so-far is a genuine local minimum, not just a receding start
        var prevDist = Double.POSITIVE_INFINITY
        for (i in 1 until planned.size) {
            val proj = GeoMath.nearestPointOnSegment(point, planned[i - 1], planned[i])
            val along = cumulative[i - 1] + proj.t * (cumulative[i] - cumulative[i - 1])
            if (along < fromAlong) continue // foot lies behind the cursor; skip so we advance in route order
            val d = proj.distanceM
            if (d < minDist) {
                minDist = d; minAlong = along; found = true
                // A genuine approach (vs. a clamped boundary foot on the receding side of a trough
                // we have already passed): the route bends closest mid-segment (interior foot), or
                // it is still descending toward the point, or it is simply on-route here.
                minIsTrough = (proj.t > 0.0 && proj.t < 1.0) ||
                    (prevDist.isFinite() && d < prevDist) ||
                    d <= onRouteThresholdM
            }
            // Once the route has climbed clearly past that trough, it was the point's first pass;
            // commit it and ignore any nearer later pass the route reaches further on.
            if (minIsTrough && d > minDist + onRouteThresholdM) return minAlong
            prevDist = d
        }
        return if (found) minAlong else fromAlong
    }

    /**
     * Forward-biased live projection. Matches [current] within a distance-along window
     * `[cursor - backSlackM, cursor + forwardReachM]` around the rider's last position, so a
     * route that passes near itself stays on the correct pass. Falls back to a global search
     * to (re-)acquire on the first fix, after a long GPS gap, or when the in-window match is
     * off route (> [offRouteThresholdM]). Advances the cursor on each call.
     */
    fun track(current: GeoPos, offRouteThresholdM: Double): NavFix? {
        val cur = cursorAlong
        val n = if (cur == null) {
            nearest(current)
        } else {
            val windowed = nearest(current, (cur - backSlackM)..(cur + forwardReachM))
            if (windowed != null && windowed.crossTrackM <= offRouteThresholdM) windowed
            else nearest(current) // re-acquire
        } ?: return null
        cursorAlong = n.distanceAlong
        return toFix(n)
    }

    /**
     * The route polyline from the rider's nearest point forward by [lookAheadMeters]:
     * `[foot, planned[segIndex], planned[segIndex+1], …]`, ending at an interpolated
     * point at exactly [lookAheadMeters] (or the route's last vertex if it ends first).
     * Used by the fit-ahead camera. Empty when the route is degenerate.
     */
    fun lookAhead(current: GeoPos, lookAheadMeters: Double): List<GeoPos> {
        val cur = cursorAlong
        val n = (if (cur == null) nearest(current)
                 else nearest(current, (cur - backSlackM)..(cur + forwardReachM)) ?: nearest(current))
            ?: return emptyList()
        val window = mutableListOf(n.foot)
        var remaining = lookAheadMeters
        var prev = n.foot
        var i = n.segIndex
        while (i < planned.size && remaining > 0.0) {
            val seg = GeoMath.haversineMeters(prev, planned[i])
            if (seg <= remaining) {
                window.add(planned[i])
                remaining -= seg
                prev = planned[i]
                i++
            } else {
                val t = remaining / seg
                window.add(GeoPos(prev.lat + t * (planned[i].lat - prev.lat), prev.lng + t * (planned[i].lng - prev.lng)))
                remaining = 0.0
            }
        }
        return window
    }
}
