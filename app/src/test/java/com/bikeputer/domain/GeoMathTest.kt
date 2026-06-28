package com.bikeputer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {
    @Test fun haversine_zero_for_same_point() {
        assertEquals(0.0, GeoMath.haversineMeters(GeoPos(50.0, 19.0), GeoPos(50.0, 19.0)), 1e-6)
    }

    @Test fun haversine_one_degree_lon_at_equator() {
        // ~111.19 km for 1° of longitude at the equator (R = 6_371_000)
        val d = GeoMath.haversineMeters(GeoPos(0.0, 0.0), GeoPos(0.0, 1.0))
        assertEquals(111_194.9, d, 50.0)
    }

    @Test fun nearest_point_midsegment_perpendicular() {
        val a = GeoPos(0.0, 0.0)
        val b = GeoPos(0.0, 0.002)
        val p = GeoPos(0.001, 0.001) // 0.001° north of the segment midpoint
        val proj = GeoMath.nearestPointOnSegment(p, a, b)
        assertEquals(0.5, proj.t, 0.02)
        assertEquals(0.001, proj.point.lng, 1e-5)
        assertEquals(0.0, proj.point.lat, 1e-5)
        assertEquals(111.19, proj.distanceM, 5.0) // 0.001° of latitude
    }

    @Test fun nearest_point_clamps_past_endpoint() {
        val a = GeoPos(0.0, 0.0)
        val b = GeoPos(0.0, 0.002)
        val p = GeoPos(0.0, 0.003) // beyond b
        val proj = GeoMath.nearestPointOnSegment(p, a, b)
        assertEquals(1.0, proj.t, 1e-9)
        assertEquals(0.002, proj.point.lng, 1e-9)
        assertEquals(111.19, proj.distanceM, 5.0) // 0.001° of longitude past b
    }

    @Test fun bearing_north() {
        // Due north: same longitude, higher latitude.
        assertEquals(0.0, GeoMath.initialBearing(GeoPos(0.0, 0.0), GeoPos(0.001, 0.0)), 0.5)
    }

    @Test fun bearing_east() {
        assertEquals(90.0, GeoMath.initialBearing(GeoPos(0.0, 0.0), GeoPos(0.0, 0.001)), 0.5)
    }

    @Test fun bearing_south() {
        assertEquals(180.0, GeoMath.initialBearing(GeoPos(0.0, 0.0), GeoPos(-0.001, 0.0)), 0.5)
    }

    @Test fun bearing_west_normalised_positive() {
        // Due west must come back as 270, not -90.
        assertEquals(270.0, GeoMath.initialBearing(GeoPos(0.0, 0.0), GeoPos(0.0, -0.001)), 0.5)
    }
}
