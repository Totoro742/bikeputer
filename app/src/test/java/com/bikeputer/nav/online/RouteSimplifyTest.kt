package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSimplifyTest {
    @Test fun short_route_is_unchanged() {
        val r = listOf(GeoPos(0.0, 0.0), GeoPos(0.0, 0.001), GeoPos(0.0, 0.002))
        assertEquals(r, simplifyRoute(r, maxPoints = 50))
    }

    @Test fun collinear_dense_route_collapses_to_endpoints() {
        // 100 points on a straight equator line; a straight line needs only its two ends.
        val r = (0..99).map { GeoPos(0.0, it * 0.0001) }
        val out = simplifyRoute(r, maxPoints = 50)
        assertEquals(2, out.size)
        assertEquals(r.first(), out.first())
        assertEquals(r.last(), out.last())
    }

    @Test fun caps_waypoint_count_and_keeps_endpoints() {
        // A noisy zig-zag of 400 points: every vertex is a real corner, but we must cap at 50.
        val r = (0..399).map { GeoPos(if (it % 2 == 0) 0.0 else 0.0009, it * 0.001) }
        val out = simplifyRoute(r, maxPoints = 50)
        assertTrue("size=${out.size}", out.size <= 50)
        assertEquals(r.first(), out.first())
        assertEquals(r.last(), out.last())
    }

    @Test fun decimation_fallback_respects_cap_at_exact_multiple() {
        // Force the fallback: a tiny maxPoints with a degenerate route DP can't thin below the cap.
        // n = 2*maxPoints exercises the off-by-one boundary; result must stay within the cap.
        val n = 20
        val maxPoints = 10
        val r = (0 until n).map { GeoPos(0.0, it * 0.001) }
        val out = simplifyRoute(r, maxPoints = maxPoints)
        assertTrue("size=${out.size} must be <= $maxPoints", out.size <= maxPoints)
        assertEquals(r.first(), out.first())
        assertEquals(r.last(), out.last())
    }
}
