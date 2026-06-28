package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import com.bikeputer.nav.TurnClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineManeuversTest {

    // An L: east ~222 m, then south ~222 m — a real ~90° RIGHT turn at the corner (~222 m).
    private val lTurn = listOf(GeoPos(0.0, 0.0), GeoPos(0.0, 0.002), GeoPos(-0.002, 0.002))

    @Test fun turn_class_and_location_come_from_geometry_not_ors() {
        // ORS reports the corner as a SHARP LEFT (type 2) — wrong for this route, the way a
        // via-point-snapped fetch can. The cue must follow the actual geometry (a right turn).
        val directions = RouteDirections(
            steps = listOf(
                RouteStep(11, "-", GeoPos(0.0, 0.0)),          // depart
                RouteStep(2, "Sienna", GeoPos(0.0, 0.002)),    // ORS: sharp-left onto Sienna
                RouteStep(10, "-", GeoPos(-0.002, 0.002)),     // arrive
            ),
        )
        val m = onlineManeuvers(directions, lTurn)
        assertEquals(1, m.size)
        assertEquals(TurnClass.Right, m[0].turn)          // geometry wins over ORS's sharp-left
        assertEquals(222.4, m[0].distanceAlong, 5.0)      // geometry's corner location
        assertEquals("Sienna", m[0].name)                 // ORS supplies only the name
    }

    @Test fun turn_is_unnamed_when_no_ors_step_is_near_it() {
        // The only named ORS step sits ~33 m in, far from the ~222 m corner → no name attaches.
        val directions = RouteDirections(
            steps = listOf(
                RouteStep(11, "-", GeoPos(0.0, 0.0)),
                RouteStep(1, "Far Road", GeoPos(0.0, 0.0003)),
                RouteStep(10, "-", GeoPos(-0.002, 0.002)),
            ),
        )
        val m = onlineManeuvers(directions, lTurn)
        assertEquals(1, m.size)
        assertEquals(TurnClass.Right, m[0].turn)
        assertNull(m[0].name)
    }

    @Test fun each_geometry_turn_gets_its_own_nearest_street_name() {
        // Two corners: right at ~222 m, left at ~445 m.
        val route = listOf(
            GeoPos(0.0, 0.0), GeoPos(0.0, 0.002),       // east
            GeoPos(-0.002, 0.002), GeoPos(-0.002, 0.004), // south, then east
        )
        val directions = RouteDirections(
            steps = listOf(
                RouteStep(11, "-", GeoPos(0.0, 0.0)),
                RouteStep(1, "First St", GeoPos(0.0, 0.002)),    // at the ~222 m corner
                RouteStep(0, "Second St", GeoPos(-0.002, 0.002)), // at the ~445 m corner
                RouteStep(10, "-", GeoPos(-0.002, 0.004)),
            ),
        )
        val m = onlineManeuvers(directions, route)
        assertEquals(2, m.size)
        assertEquals(TurnClass.Right, m[0].turn)
        assertEquals("First St", m[0].name)
        assertEquals(TurnClass.Left, m[1].turn)
        assertEquals("Second St", m[1].name)
    }

    @Test fun straight_route_has_no_maneuvers_even_with_ors_steps() {
        val straight = (0..4).map { GeoPos(0.0, it * 0.001) } // no corners
        val directions = RouteDirections(
            steps = listOf(
                RouteStep(11, "-", GeoPos(0.0, 0.0)),
                RouteStep(1, "Phantom St", GeoPos(0.0, 0.002)),
                RouteStep(10, "-", GeoPos(0.0, 0.004)),
            ),
        )
        assertTrue(onlineManeuvers(directions, straight).isEmpty())
    }

    @Test fun parses_ors_geojson_into_steps() {
        val json = """
        {"type":"FeatureCollection","features":[{"type":"Feature",
          "geometry":{"type":"LineString","coordinates":[[0.0,0.0],[0.001,0.0],[0.003,0.0]]},
          "properties":{"segments":[{"steps":[
            {"type":11,"name":"-","way_points":[0,0]},
            {"type":1,"name":"Main Street","way_points":[1,2]},
            {"type":10,"name":"-","way_points":[2,2]}
          ]}]}}]}
        """.trimIndent()
        val d = parseOrsDirections(json)
        assertEquals(3, d.steps.size)
        assertEquals(1, d.steps[1].typeCode)
        assertEquals("Main Street", d.steps[1].name)
        // way_points[0] = 1 → coordinate [0.001,0.0] → GeoPos(lat=0.0, lng=0.001)
        assertEquals(0.001, d.steps[1].point.lng, 1e-9)
        assertEquals(0.0, d.steps[1].point.lat, 1e-9)
    }

    @Test fun parses_geometry_polyline() {
        val json = """
        {"type":"FeatureCollection","features":[{"type":"Feature",
          "geometry":{"type":"LineString","coordinates":[[0.0,0.0],[0.001,0.0],[0.003,0.0]]},
          "properties":{"segments":[{"steps":[
            {"type":11,"name":"-","way_points":[0,0]},
            {"type":10,"name":"-","way_points":[2,2]}
          ]}]}}]}
        """.trimIndent()
        val d = parseOrsDirections(json)
        assertEquals(3, d.geometry.size)
        assertEquals(0.001, d.geometry[1].lng, 1e-9)
        assertEquals(0.003, d.geometry[2].lng, 1e-9)
        assertEquals(0.0, d.geometry[2].lat, 1e-9)
    }
}
