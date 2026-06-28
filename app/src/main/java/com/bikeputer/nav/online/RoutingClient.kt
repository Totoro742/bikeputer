package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import com.bikeputer.nav.Maneuver
import com.bikeputer.nav.RouteFollower
import com.bikeputer.nav.detectManeuvers
import kotlin.math.abs

/** One instruction from the engine: its ORS maneuver [typeCode], street [name], and turn location [point]. */
data class RouteStep(val typeCode: Int, val name: String, val point: GeoPos)

/** Parsed directions: the ordered instruction steps plus the snapped road geometry. */
data class RouteDirections(val steps: List<RouteStep>, val geometry: List<GeoPos> = emptyList())

/** Online routing engine. The single seam that touches the network; mockable in tests. */
interface RoutingClient {
    /** Cycling directions through [waypoints] in route order. Throws on any failure. */
    suspend fun directions(waypoints: List<GeoPos>): RouteDirections
}

/** A street name is attached to a geometry turn only if a named ORS step projects within this
 * distance (m) of it; beyond that the turn stays unnamed rather than borrow a distant road. */
private const val NAME_MATCH_M = 60.0

private fun isRealName(name: String?) = !name.isNullOrBlank() && name != "-"

/**
 * Turn-by-turn maneuvers for an armed route, online-enriched.
 *
 * Turn *class* and *location* come from the planned polyline's own geometry
 * ([detectManeuvers]) so every arrow matches the line the rider actually follows. The engine
 * [directions] supply only the street *name*: each named step is projected onto [planned] (in
 * route order, forward-monotonic, so a self-overlapping route attaches a name to the correct
 * pass) and the nearest such name within [NAME_MATCH_M] is attached to each geometry turn.
 *
 * We deliberately ignore ORS's own turn `type`: because a fetch routes through the whole track
 * as via-points, ORS can report a gentle bend as a sharp turn, so its classification is
 * unreliable for an imported route. Geometry is the source of truth for *which way* to turn.
 */
fun onlineManeuvers(directions: RouteDirections, planned: List<GeoPos>): List<Maneuver> {
    val turns = detectManeuvers(planned)
    if (turns.isEmpty() || planned.size < 2) return turns
    val follower = RouteFollower(planned)
    val namedAlong = ArrayList<Pair<Double, String>>(directions.steps.size)
    var cursor = 0.0
    for (step in directions.steps) {
        val along = follower.projectForwardAlong(step.point, cursor)
        cursor = along
        if (isRealName(step.name)) namedAlong.add(along to step.name)
    }
    return turns.map { turn ->
        val nearest = namedAlong.minByOrNull { abs(it.first - turn.distanceAlong) }
        if (nearest != null && abs(nearest.first - turn.distanceAlong) <= NAME_MATCH_M) {
            turn.copy(name = nearest.second)
        } else {
            turn
        }
    }
}
