package com.bikeputer.nav

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TurnGuideTest {
    private val earthR = 6_371_000.0

    /** A point [meters] from [from] along true bearing [bearingDeg] (great-circle). */
    private fun step(from: GeoPos, bearingDeg: Double, meters: Double): GeoPos {
        val d = meters / earthR
        val br = Math.toRadians(bearingDeg)
        val lat1 = Math.toRadians(from.lat)
        val lon1 = Math.toRadians(from.lng)
        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(br))
        val lon2 = lon1 + atan2(sin(br) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))
        return GeoPos(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    /** Build a polyline by walking [legs] of (bearingDeg, meters) from [start]. */
    private fun walk(start: GeoPos, vararg legs: Pair<Double, Double>): List<GeoPos> {
        val pts = mutableListOf(start)
        for ((bearing, meters) in legs) pts.add(step(pts.last(), bearing, meters))
        return pts
    }

    @Test fun straight_line_has_no_maneuvers() {
        val route = walk(GeoPos(0.0, 0.0), 90.0 to 100.0, 90.0 to 100.0, 90.0 to 100.0)
        assertTrue(detectManeuvers(route).isEmpty())
    }

    @Test fun right_angle_right_turn() {
        // Go north, then east: a 90° right turn.
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 90.0 to 100.0)
        val m = detectManeuvers(route)
        assertEquals(1, m.size)
        assertEquals(TurnClass.Right, m[0].turn)
        assertEquals(100.0, m[0].distanceAlong, 5.0)
    }

    @Test fun right_angle_left_turn() {
        // Go north, then west: a 90° left turn.
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 270.0 to 100.0)
        assertEquals(TurnClass.Left, detectManeuvers(route).single().turn)
    }

    @Test fun slight_turn_classified_slight() {
        // North, then heading 40° (a 40° right turn).
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 40.0 to 100.0)
        assertEquals(TurnClass.SlightRight, detectManeuvers(route).single().turn)
    }

    @Test fun sharp_turn_classified_sharp() {
        // North, then heading 135° (a 135° right turn).
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 135.0 to 100.0)
        assertEquals(TurnClass.SharpRight, detectManeuvers(route).single().turn)
    }

    // Straddle the slight/normal cutoff (55°). Exact-55° geometry is unreliable
    // (sub-millidegree great-circle fuzz flips the result), so test 1° either side:
    // 54° must stay slight, 56° must become normal.
    @Test fun just_below_normal_cutoff_is_slight() {
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 54.0 to 100.0)
        assertEquals(TurnClass.SlightRight, detectManeuvers(route).single().turn)
    }

    @Test fun just_above_normal_cutoff_is_normal() {
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 56.0 to 100.0)
        assertEquals(TurnClass.Right, detectManeuvers(route).single().turn)
    }

    // Straddle the normal/sharp cutoff (110°): 109° stays normal, 111° becomes sharp.
    @Test fun just_below_sharp_cutoff_is_normal() {
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 109.0 to 100.0)
        assertEquals(TurnClass.Right, detectManeuvers(route).single().turn)
    }

    @Test fun just_above_sharp_cutoff_is_sharp() {
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 111.0 to 100.0)
        assertEquals(TurnClass.SharpRight, detectManeuvers(route).single().turn)
    }

    @Test fun below_threshold_is_not_a_maneuver() {
        // North, then heading 20° (only a 20° bend, under TURN_THRESHOLD_DEG).
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 20.0 to 100.0)
        assertTrue(detectManeuvers(route).isEmpty())
    }

    @Test fun gradual_bend_accumulates_to_one_maneuver() {
        // Seven 10°-right steps (heading 0,10,20,...70): a single ~70° right bend.
        val legs = (0..7).map { (it * 10.0) to 30.0 }.toTypedArray()
        val route = walk(GeoPos(0.0, 0.0), *legs)
        val m = detectManeuvers(route)
        assertEquals(1, m.size)
        assertEquals(TurnClass.Right, m[0].turn) // ~70° total → normal
    }

    @Test fun loop_keeps_each_same_direction_turn_separate() {
        // A square ridden back to its start: four ~90° LEFT turns. They must stay distinct
        // (the closing turn at the start/finish is not a cue the rider executes), not collapse
        // into one maneuver shown at the last/sharpest corner.
        val route = walk(GeoPos(0.0, 0.0), 90.0 to 100.0, 0.0 to 100.0, 270.0 to 100.0, 180.0 to 100.0)
        val m = detectManeuvers(route)
        assertEquals(3, m.size)
        m.forEach { assertEquals(TurnClass.Left, it.turn) }
        assertEquals(100.0, m[0].distanceAlong, 5.0)
        assertEquals(200.0, m[1].distanceAlong, 5.0)
        assertEquals(300.0, m[2].distanceAlong, 5.0)
    }

    @Test fun same_direction_turns_far_apart_stay_separate() {
        // North, right onto east, straight east, right onto south: two right turns ~200 m apart
        // with a straight run between. They must stay two maneuvers, not collapse into one.
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 90.0 to 100.0, 90.0 to 100.0, 180.0 to 100.0)
        val m = detectManeuvers(route)
        assertEquals(2, m.size)
        m.forEach { assertEquals(TurnClass.Right, it.turn) }
        assertEquals(100.0, m[0].distanceAlong, 5.0)
        assertEquals(300.0, m[1].distanceAlong, 5.0)
    }

    @Test fun loop_first_turn_is_announced_first() {
        // The reported symptom: on a same-start-end loop the FIRST turn was shown as the last.
        val route = walk(GeoPos(0.0, 0.0), 90.0 to 100.0, 0.0 to 100.0, 270.0 to 100.0, 180.0 to 100.0)
        val u = TurnGuide(route).upcoming(0.0)!!
        assertEquals(TurnClass.Left, u.next.turn)
        assertEquals(100.0, u.next.distanceM, 5.0)
    }

    @Test fun degenerate_route_has_no_maneuvers() {
        assertTrue(detectManeuvers(emptyList()).isEmpty())
        assertTrue(detectManeuvers(listOf(GeoPos(0.0, 0.0), GeoPos(0.0, 0.001))).isEmpty())
    }

    // S-curve: north 100 m, east 100 m (right turn), north 100 m (left turn).
    // Opposite-direction turns stay separate (same-direction turns would merge),
    // giving a Right at ~100 m and a Left at ~200 m.
    private val twoTurns = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 90.0 to 100.0, 0.0 to 100.0)

    @Test fun upcoming_before_any_turn_returns_next_and_then() {
        val u = TurnGuide(twoTurns).upcoming(0.0)!!
        assertEquals(TurnClass.Right, u.next.turn)
        assertEquals(100.0, u.next.distanceM, 5.0)
        assertEquals(TurnClass.Left, u.then!!.turn)
        assertEquals(200.0, u.then!!.distanceM, 5.0)
    }

    @Test fun upcoming_after_first_turn_advances() {
        val u = TurnGuide(twoTurns).upcoming(150.0)!!
        assertEquals(TurnClass.Left, u.next.turn)
        assertEquals(50.0, u.next.distanceM, 5.0)
        assertEquals(null, u.then)
    }

    @Test fun upcoming_past_last_turn_is_null() {
        assertEquals(null, TurnGuide(twoTurns).upcoming(250.0))
    }

    @Test fun upcoming_single_turn_has_no_then() {
        val oneTurn = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 90.0 to 100.0)
        assertEquals(null, TurnGuide(oneTurn).upcoming(0.0)!!.then)
    }

    @Test fun upcoming_threads_street_name_from_maneuver() {
        val m = listOf(
            Maneuver(100.0, TurnClass.Right, "Main Street"),
            Maneuver(200.0, TurnClass.Left, "Oak Avenue"),
        )
        val u = TurnGuide(m).upcoming(0.0)!!
        assertEquals("Main Street", u.next.name)
        assertEquals(TurnClass.Right, u.next.turn)
        assertEquals(100.0, u.next.distanceM, 0.001)
        assertEquals("Oak Avenue", u.then!!.name)
    }

    @Test fun upcoming_from_geometry_has_null_name() {
        // Convenience constructor path: geometry-derived maneuvers carry no name.
        val route = walk(GeoPos(0.0, 0.0), 0.0 to 100.0, 90.0 to 100.0)
        assertEquals(null, TurnGuide(route).upcoming(0.0)!!.next.name)
    }
}
