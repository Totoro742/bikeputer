package com.bikeputer.nav.online

import com.bikeputer.domain.GeoMath
import com.bikeputer.domain.GeoPos
import kotlin.math.abs

/**
 * Reduce [points] to at most [maxPoints] while preserving shape and both endpoints.
 * Runs Douglas-Peucker, raising the tolerance until the result fits the cap. Returns
 * the input unchanged when it already fits. Perpendicular distance is approximated by
 * the triangle-area method over haversine edge lengths (fine for the short legs here).
 */
fun simplifyRoute(points: List<GeoPos>, maxPoints: Int = 50): List<GeoPos> {
    if (points.size <= maxPoints) return points
    var tolerance = 5.0 // metres
    var result = douglasPeucker(points, tolerance)
    // Escalate tolerance geometrically until within the cap (bounded iterations).
    var guard = 0
    while (result.size > maxPoints && guard < 40) {
        tolerance *= 1.6
        result = douglasPeucker(points, tolerance)
        guard++
    }
    // Hard fallback: even-stride decimation that keeps both endpoints, capped at maxPoints.
    if (result.size > maxPoints) {
        val stride = (points.size + maxPoints - 1) / maxPoints
        val decimated = points.filterIndexed { i, _ -> i % stride == 0 }.toMutableList()
        if (decimated.last() != points.last()) decimated.add(points.last())
        // Decimation can yield maxPoints+1; trim an interior point to hold the cap, keeping endpoints.
        result = if (decimated.size <= maxPoints) decimated
                 else decimated.take(maxPoints - 1) + listOf(points.last())
    }
    return result
}

private fun douglasPeucker(pts: List<GeoPos>, tol: Double): List<GeoPos> {
    if (pts.size < 3) return pts
    var maxDist = 0.0
    var index = 0
    for (i in 1 until pts.size - 1) {
        val d = perpDistanceM(pts[i], pts.first(), pts.last())
        if (d > maxDist) { maxDist = d; index = i }
    }
    return if (maxDist > tol) {
        val left = douglasPeucker(pts.subList(0, index + 1), tol)
        val right = douglasPeucker(pts.subList(index, pts.size), tol)
        left.dropLast(1) + right
    } else {
        listOf(pts.first(), pts.last())
    }
}

/** Distance (m) from [p] to segment [a]-[b], via triangle area / base length. */
private fun perpDistanceM(p: GeoPos, a: GeoPos, b: GeoPos): Double {
    val ab = GeoMath.haversineMeters(a, b)
    if (ab < 1e-6) return GeoMath.haversineMeters(p, a)
    val ap = GeoMath.haversineMeters(a, p)
    val bp = GeoMath.haversineMeters(b, p)
    val s = (ab + ap + bp) / 2.0
    val area = kotlin.math.sqrt(abs(s * (s - ab) * (s - ap) * (s - bp)))
    return 2.0 * area / ab
}
