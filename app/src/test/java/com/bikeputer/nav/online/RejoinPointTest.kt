package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RejoinPointTest {
    // Straight equator route; nodes at lng 0.000..0.004 (~111.19 m apart, total ~444.76 m).
    private val planned = (0..4).map { GeoPos(0.0, it * 0.001) }

    @Test fun picks_nearest_point_ahead_of_departure() {
        // Rider departed at the start, now ~55 m north of lng 0.0025.
        val target = rejoinTarget(planned, GeoPos(0.0005, 0.0025), departureAlong = 0.0, lookAheadCapM = 2_000.0)
        assertEquals(0.0025, target.lng, 1e-4) // snaps to the nearest forward point
        assertEquals(0.0, target.lat, 1e-4)
    }

    @Test fun never_targets_behind_the_departure_cursor() {
        // Rider physically near lng 0.001 (behind), but departed at ~333 m (node 3, lng 0.003).
        val target = rejoinTarget(planned, GeoPos(0.0005, 0.001), departureAlong = 333.0, lookAheadCapM = 2_000.0)
        assertTrue("must not snap behind the cursor, got lng=${target.lng}", target.lng >= 0.003 - 1e-6)
    }

    @Test fun falls_back_to_endpoint_when_nothing_within_cap() {
        // Departure already past the whole route → no segment in [departureAlong, departureAlong+cap].
        val target = rejoinTarget(planned, GeoPos(0.0, 0.01), departureAlong = 10_000.0, lookAheadCapM = 50.0)
        assertEquals(0.004, target.lng, 1e-9) // planned.last()
    }

    @Test fun degenerate_single_vertex_route_returns_that_vertex() {
        // A route with fewer than 2 points has no segments to project onto; return its only vertex.
        val one = listOf(GeoPos(0.0, 0.002))
        val target = rejoinTarget(one, GeoPos(0.001, 0.0), departureAlong = 0.0, lookAheadCapM = 2_000.0)
        assertEquals(0.002, target.lng, 1e-9)
        assertEquals(0.0, target.lat, 1e-9)
    }
}
