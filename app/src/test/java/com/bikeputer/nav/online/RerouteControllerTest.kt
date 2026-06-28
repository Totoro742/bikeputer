package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import com.bikeputer.nav.NavFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RerouteControllerTest {
    private val planned = (0..6).map { GeoPos(0.0, it * 0.001) } // ~667 m straight route
    private val cfg = RerouteConfig(
        debounceMs = 8_000, minIntervalMs = 20_000, maxRequestsPerRide = 2,
        rejoinLookAheadCapM = 2_000.0, offRouteThresholdM = 35.0,
    )
    private val here = GeoPos(0.001, 0.002) // ~111 m north of the route

    private fun onRoute(along: Double) = NavFix(5.0, 0.0, 0f, along)
    private fun offRoute(along: Double) = NavFix(200.0, 0.0, 0f, along)

    // A 2-point detour whose geometry is far from `here`, so the reroute follower reports off-route.
    private fun detour() = RouteDirections(
        steps = listOf(RouteStep(11, "-", GeoPos(0.0, 0.0)), RouteStep(10, "-", GeoPos(0.05, 0.05))),
        geometry = listOf(GeoPos(0.05, 0.05), GeoPos(0.05, 0.051)),
    )

    @Test fun on_route_stays_idle() {
        val c = RerouteController(planned, cfg)
        assertNull(c.onFix(here, onRoute(100.0), onlineActive = true, nowMs = 0))
        assertTrue(c.state is RerouteState.Idle)
    }

    @Test fun brief_off_route_then_rejoin_never_fires() {
        val c = RerouteController(planned, cfg)
        assertNull(c.onFix(here, offRoute(100.0), true, 0))      // → Pending
        assertTrue(c.state is RerouteState.Pending)
        assertNull(c.onFix(here, offRoute(100.0), true, 4_000))  // still within debounce
        assertNull(c.onFix(here, onRoute(120.0), true, 6_000))   // rejoined → Idle, no fire
        assertTrue(c.state is RerouteState.Idle)
    }

    @Test fun sustained_off_route_fires_after_debounce() {
        val c = RerouteController(planned, cfg)
        assertNull(c.onFix(here, offRoute(100.0), true, 0))       // → Pending at 0
        val req = c.onFix(here, offRoute(100.0), true, 8_000)     // debounce elapsed
        assertNotNull(req)
        assertEquals(here, req!!.from)
        assertTrue("rejoin must be ahead of the start", req.to.lng > 0.0)
    }

    @Test fun rate_limit_blocks_a_second_request_within_interval() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)
        assertNotNull(c.onFix(here, offRoute(100.0), true, 8_000))     // 1st fire
        assertNull(c.onFix(here, offRoute(100.0), true, 12_000))       // within 20 s → blocked
        assertNotNull(c.onFix(here, offRoute(100.0), true, 28_000))    // ≥ 20 s later → 2nd fire
    }

    @Test fun per_ride_cap_stops_further_requests() {
        val c = RerouteController(planned, cfg) // cap = 2
        c.onFix(here, offRoute(100.0), true, 0)
        assertNotNull(c.onFix(here, offRoute(100.0), true, 8_000))   // 1
        assertNotNull(c.onFix(here, offRoute(100.0), true, 28_000))  // 2 (cap reached)
        assertNull(c.onFix(here, offRoute(100.0), true, 60_000))     // 3rd blocked by cap
    }

    @Test fun installing_a_detour_goes_active_with_a_guide() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)
        c.onFix(here, offRoute(100.0), true, 8_000)
        c.onRerouteResult(detour())
        assertTrue(c.state is RerouteState.Active)
    }

    @Test fun rejoining_main_route_drops_the_reroute() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)
        c.onFix(here, offRoute(100.0), true, 8_000)
        c.onRerouteResult(detour())
        assertNull(c.onFix(here, onRoute(140.0), true, 30_000)) // back on the GPX
        assertTrue(c.state is RerouteState.Idle)
        assertNull(c.rerouteNavFix)
    }

    @Test fun online_inactive_resets_to_idle() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0) // Pending
        assertNull(c.onFix(here, offRoute(100.0), onlineActive = false, nowMs = 9_000))
        assertTrue(c.state is RerouteState.Idle)
        assertNull(c.rerouteNavFix)
    }

    // Helper: drive the controller to Active state (1 request issued at 8_000 ms).
    private fun driveToActive(): RerouteController {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)          // → Pending
        c.onFix(here, offRoute(100.0), true, 8_000)      // → fires 1st request
        c.onRerouteResult(detour())                       // → Active
        return c
    }

    @Test fun drift_off_active_detour_refires_a_request() {
        val c = driveToActive()
        // `here` (0.001, 0.002) is ~5.5 km from the detour geometry near (0.05, 0.05),
        // so the detour follower reports off-route.  Rate limit: 28_000 - 8_000 = 20_000 ms ≥ minIntervalMs.
        val req = c.onFix(here, offRoute(100.0), true, 28_000)
        assertNotNull("should re-fire when drifted off the active detour", req)
        assertTrue(c.state is RerouteState.Active)
    }

    @Test fun staying_on_active_detour_does_not_refire() {
        val c = driveToActive()
        // A point on the detour line (midpoint of GeoPos(0.05,0.05)..GeoPos(0.05,0.051)).
        val onDetour = GeoPos(0.05, 0.0505)
        val req = c.onFix(onDetour, offRoute(100.0), true, 28_000)
        assertNull("should not re-fire while on the active detour", req)
        assertTrue(c.state is RerouteState.Active)
        assertNotNull(c.rerouteNavFix)
    }

    @Test fun detour_installs_even_after_a_rate_limited_null_fix() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)                      // Pending
        assertNotNull(c.onFix(here, offRoute(100.0), true, 8_000))   // issue (controller stays Pending)
        assertNull(c.onFix(here, offRoute(100.0), true, 9_000))      // rate-limited null while a fetch is in flight
        c.onRerouteResult(detour())                                   // result still arrives and installs
        assertTrue(c.state is RerouteState.Active)
    }

    @Test fun stale_result_after_rejoin_is_ignored() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)                      // Pending
        assertNotNull(c.onFix(here, offRoute(100.0), true, 8_000))   // issue, still Pending
        assertNull(c.onFix(here, onRoute(120.0), true, 9_000))       // rejoined → Idle before the fetch returns
        assertTrue(c.state is RerouteState.Idle)
        c.onRerouteResult(detour())                                  // stale detour arrives late
        assertTrue("stale detour must not reactivate after rejoin", c.state is RerouteState.Idle)
        assertNull(c.rerouteNavFix)
    }

    @Test fun installing_a_detour_seeds_the_projection_immediately() {
        val c = RerouteController(planned, cfg)
        c.onFix(here, offRoute(100.0), true, 0)
        c.onFix(here, offRoute(100.0), true, 8_000)
        c.onRerouteResult(detour())
        assertTrue(c.state is RerouteState.Active)
        // Seeded from the last known position, not blanked — the card shows on the same tick.
        assertNotNull("detour projection should be seeded on install", c.rerouteNavFix)
    }
}
