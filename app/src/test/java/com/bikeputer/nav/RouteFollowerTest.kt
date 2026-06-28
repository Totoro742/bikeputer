package com.bikeputer.nav

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteFollowerTest {
    // Straight east-west route along the equator: 0.000 → 0.001 → 0.002 lon.
    // Each 0.001° ≈ 111.19 m, so the total is ≈ 222.39 m.
    private val route = listOf(GeoPos(0.0, 0.0), GeoPos(0.0, 0.001), GeoPos(0.0, 0.002))

    @Test fun returns_null_for_degenerate_route() {
        assertNull(RouteFollower(emptyList()).project(GeoPos(0.0, 0.0)))
        assertNull(RouteFollower(listOf(GeoPos(0.0, 0.0))).project(GeoPos(0.0, 0.0)))
        assertNull(RouteFollower(listOf(GeoPos(0.0, 0.0), GeoPos(0.0, 0.0))).project(GeoPos(0.0, 0.0)))
    }

    @Test fun at_start_fraction_zero_remaining_full() {
        val fix = RouteFollower(route).project(GeoPos(0.0, 0.0))!!
        assertTrue(fix.offRouteMeters < 1.0)
        assertEquals(0f, fix.fractionComplete, 0.01f)
        assertEquals(222.39, fix.remainingMeters, 5.0)
    }

    @Test fun at_end_fraction_one_remaining_zero() {
        val fix = RouteFollower(route).project(GeoPos(0.0, 0.002))!!
        assertTrue(fix.offRouteMeters < 1.0)
        assertEquals(1f, fix.fractionComplete, 0.01f)
        assertEquals(0.0, fix.remainingMeters, 5.0)
    }

    @Test fun at_midpoint_fraction_half() {
        val fix = RouteFollower(route).project(GeoPos(0.0, 0.001))!!
        assertEquals(0.5f, fix.fractionComplete, 0.02f)
        assertEquals(111.19, fix.remainingMeters, 5.0)
    }

    @Test fun off_route_reports_cross_track_distance() {
        // 0.001° of latitude north of the midpoint ≈ 111.19 m off route.
        val fix = RouteFollower(route).project(GeoPos(0.001, 0.001))!!
        assertEquals(111.19, fix.offRouteMeters, 5.0)
        assertEquals(0.5f, fix.fractionComplete, 0.02f)
        assertEquals(111.19, fix.remainingMeters, 5.0)
    }

    private fun pathLength(pts: List<GeoPos>): Double {
        var d = 0.0
        for (i in 1 until pts.size) d += com.bikeputer.domain.GeoMath.haversineMeters(pts[i - 1], pts[i])
        return d
    }

    @Test fun lookAhead_from_start_caps_at_distance() {
        val window = RouteFollower(route).lookAhead(GeoPos(0.0, 0.0), 100.0)
        assertEquals(0.0, window.first().lat, 1e-4)
        assertEquals(0.0, window.first().lng, 1e-4)
        assertEquals(100.0, pathLength(window), 5.0)
    }

    @Test fun lookAhead_beyond_route_stops_at_end() {
        val window = RouteFollower(route).lookAhead(GeoPos(0.0, 0.0), 10_000.0)
        assertEquals(0.002, window.last().lng, 1e-5)
        assertEquals(222.39, pathLength(window), 5.0)
    }

    @Test fun lookAhead_empty_for_degenerate_route() {
        assertTrue(RouteFollower(listOf(GeoPos(0.0, 0.0))).lookAhead(GeoPos(0.0, 0.0), 100.0).isEmpty())
    }

    @Test fun lookAhead_off_route_starts_at_nearest_point() {
        // ~55 m north of the segment near lng 0.0005; the foot lies on the line (lat ≈ 0).
        val window = RouteFollower(route).lookAhead(GeoPos(0.001, 0.0005), 100.0)
        assertTrue(window.isNotEmpty())
        assertEquals(0.0, window.first().lat, 1e-3)
    }

    @Test fun distanceAlong_tracks_progress() {
        assertEquals(0.0, RouteFollower(route).project(GeoPos(0.0, 0.0))!!.distanceAlong, 5.0)
        assertEquals(111.19, RouteFollower(route).project(GeoPos(0.0, 0.001))!!.distanceAlong, 5.0)
        assertEquals(222.39, RouteFollower(route).project(GeoPos(0.0, 0.002))!!.distanceAlong, 5.0)
    }

    // Outbound east along the equator, then a parallel return ~2.2 m to the north.
    // Outbound spans distanceAlong 0..~222 m; the return spans ~224..~446 m — the two
    // passes are within a few metres physically but >100 m apart along the route.
    private val northOffset = 0.00002 // ~2.2 m
    private val loop = listOf(
        GeoPos(0.0, 0.0),            // ~0 m
        GeoPos(0.0, 0.001),          // ~111 m
        GeoPos(0.0, 0.002),          // ~222 m  (east end)
        GeoPos(northOffset, 0.002),  // ~224 m  (step north)
        GeoPos(northOffset, 0.001),  // ~335 m
        GeoPos(northOffset, 0.0),    // ~446 m
    )
    // Tight reach so the return pass sits outside the forward window from the outbound leg.
    private fun loopFollower() = RouteFollower(loop, backSlackM = 10.0, forwardReachM = 50.0)

    @Test fun track_returns_null_for_degenerate_route() {
        assertNull(RouteFollower(emptyList()).track(GeoPos(0.0, 0.0), 35.0))
    }

    @Test fun global_project_jumps_to_wrong_pass_at_overlap() {
        // Documents the bug track() fixes: global search picks the return pass here.
        val fix = RouteFollower(loop).project(GeoPos(0.000018, 0.001))!!
        assertTrue("global picks the return pass", fix.distanceAlong > 222.0)
    }

    @Test fun track_stays_on_first_pass_through_overlap() {
        val f = loopFollower()
        f.track(GeoPos(0.000003, 0.0002), 35.0)  // acquire on outbound, ~22 m
        f.track(GeoPos(0.000003, 0.0008), 35.0)  // ~89 m, still outbound
        // Physically nearest the RETURN pass (~0.2 m) but the rider is on the outbound leg.
        val fix = f.track(GeoPos(0.000018, 0.001), 35.0)!!
        assertTrue("expected outbound pass, got ${fix.distanceAlong}", fix.distanceAlong < 222.0)
        assertEquals(111.19, fix.distanceAlong, 20.0)
    }

    @Test fun track_absorbs_small_backward_jitter() {
        val f = loopFollower()
        f.track(GeoPos(0.0, 0.0005), 35.0)  // ~55 m
        f.track(GeoPos(0.0, 0.001), 35.0)   // ~111 m
        // Jitter nudges ~6 m back — stay on outbound, do not re-acquire.
        val fix = f.track(GeoPos(0.0, 0.00095), 35.0)!!
        assertTrue(fix.distanceAlong < 222.0)
        assertEquals(105.6, fix.distanceAlong, 15.0)
    }

    @Test fun track_reacquires_after_jump_beyond_window() {
        val f = loopFollower()
        f.track(GeoPos(0.000003, 0.0002), 35.0)
        f.track(GeoPos(0.0, 0.001), 35.0)        // ~111 m outbound, cursor ~111
        // Teleport onto the return pass near lng 0.0005 (~391 m), far outside the window.
        val fix = f.track(GeoPos(northOffset, 0.0005), 35.0)!!
        assertTrue("expected return pass, got ${fix.distanceAlong}", fix.distanceAlong > 222.0)
    }

    @Test fun lookAhead_follows_tracked_pass_through_overlap() {
        val f = loopFollower()
        f.track(GeoPos(0.000003, 0.0002), 35.0)
        f.track(GeoPos(0.000003, 0.0008), 35.0)
        f.track(GeoPos(0.000018, 0.001), 35.0)   // locked on the outbound leg
        val window = f.lookAhead(GeoPos(0.000018, 0.001), 80.0)
        // Forward along outbound heads EAST (increasing lng); the return leg would head west.
        assertTrue(window.size >= 2)
        assertTrue("expected eastward continuation, got $window", window.last().lng > window.first().lng)
    }

    @Test fun projectForward_picks_near_pass_not_far_overlap() {
        val f = RouteFollower(loop)
        // Global project() snaps this point to the return pass (~335 m).
        assertTrue(f.project(GeoPos(0.000018, 0.001))!!.distanceAlong > 222.0)
        // Forward projection from the start keeps it on the near (outbound) pass.
        assertEquals(111.19, f.projectForwardAlong(GeoPos(0.000018, 0.001), 0.0), 25.0)
    }

    @Test fun projectForward_is_monotonic_and_advances_to_far_pass_in_order() {
        val f = RouteFollower(loop)
        val a = f.projectForwardAlong(GeoPos(0.0, 0.0005), 0.0)       // ~55 m, outbound
        val b = f.projectForwardAlong(GeoPos(0.000018, 0.001), a)     // ~111 m, outbound
        val c = f.projectForwardAlong(GeoPos(northOffset, 0.0005), b) // return pass, ~391 m
        assertTrue("a<=b ($a,$b)", a <= b)
        assertTrue("b<=c ($b,$c)", b <= c)
        assertTrue("c on return pass, got $c", c > 222.0)
    }
}
