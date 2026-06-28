package com.bikeputer.nav

import com.bikeputer.domain.GeoMath
import com.bikeputer.domain.GeoPos
import kotlin.math.abs

/** Minimum absolute heading change (degrees) that counts as a turn. */
const val TURN_THRESHOLD_DEG = 30.0
private const val SLIGHT_MAX_DEG = 55.0
private const val NORMAL_MAX_DEG = 110.0
/**
 * A straight run (metres) longer than this between two turning vertices ends the current bend,
 * so two same-direction turns linked by straight road stay separate maneuvers. Below it, closely
 * spaced same-direction vertices still merge into one (a gradual multi-vertex bend).
 */
private const val BEND_GAP_M = 50.0

enum class TurnClass { SlightLeft, Left, SharpLeft, SlightRight, Right, SharpRight }

/** A single derived maneuver: where it is along the route, which way it turns, and an optional street/instruction label. */
data class Maneuver(val distanceAlong: Double, val turn: TurnClass, val name: String? = null)

/** Classify a signed total heading change (positive = right) into a [TurnClass]. */
private fun classify(signedDeg: Double): TurnClass {
    val a = abs(signedDeg)
    val right = signedDeg > 0
    return when {
        a < SLIGHT_MAX_DEG -> if (right) TurnClass.SlightRight else TurnClass.SlightLeft
        a < NORMAL_MAX_DEG -> if (right) TurnClass.Right else TurnClass.Left
        else -> if (right) TurnClass.SharpRight else TurnClass.SharpLeft
    }
}

/** Signed turn at interior vertex [i] of [p], normalised to -180..180 (positive = right). */
private fun signedTurnDeg(p: List<GeoPos>, i: Int): Double {
    val bIn = GeoMath.initialBearing(p[i - 1], p[i])
    val bOut = GeoMath.initialBearing(p[i], p[i + 1])
    val raw = bOut - bIn
    return ((raw % 360.0) + 540.0) % 360.0 - 180.0
}

/**
 * Derives turn maneuvers from a polyline's geometry. Consecutive same-direction
 * turning is accumulated into one maneuver (so a sharp corner and a gradual
 * multi-vertex bend both collapse to a single turn); a group is emitted only if
 * its summed heading change reaches [TURN_THRESHOLD_DEG]. A group also ends when a
 * straight run longer than [BEND_GAP_M] precedes the next turn, so distinct
 * same-direction turns linked by straight road (as on a loop, where the turns all
 * curve the same way) stay separate instead of collapsing into one. Returns
 * maneuvers in route order. Empty for routes with fewer than 3 points.
 */
fun detectManeuvers(planned: List<GeoPos>): List<Maneuver> {
    if (planned.size < 3) return emptyList()
    val cum = DoubleArray(planned.size)
    for (i in 1 until planned.size) cum[i] = cum[i - 1] + GeoMath.haversineMeters(planned[i - 1], planned[i])

    val out = mutableListOf<Maneuver>()
    var sum = 0.0       // accumulated signed angle of the open group
    var peakAbs = 0.0   // largest single-vertex |angle| in the group
    var peakIdx = -1    // vertex index of the peak
    var open = false

    fun close() {
        if (open && abs(sum) >= TURN_THRESHOLD_DEG && peakIdx >= 0) {
            out.add(Maneuver(cum[peakIdx], classify(sum)))
        }
        sum = 0.0; peakAbs = 0.0; peakIdx = -1; open = false
    }

    for (i in 1 until planned.size - 1) {
        val delta = signedTurnDeg(planned, i)
        // A long straight segment leading into this vertex means the previous bend has ended;
        // close it so a far-apart same-direction turn does not merge into one maneuver.
        if (open && cum[i] - cum[i - 1] > BEND_GAP_M) close()
        // Direction flip ends the current group before this vertex joins a fresh one.
        if (open && (delta > 0) != (sum > 0)) close()
        open = true
        sum += delta
        if (abs(delta) > peakAbs) { peakAbs = abs(delta); peakIdx = i }
    }
    close()
    return out
}

/** A turn cue for display: which way, how far ahead (metres from the rider), and an optional street/instruction label. */
data class Cue(val turn: TurnClass, val distanceM: Double, val name: String? = null)

/** The imminent maneuver and the one after it (null when the imminent one is last). */
data class UpcomingTurns(val next: Cue, val then: Cue?)

/**
 * Per-ride turn guide over a precomputed maneuver list: for a given distance-along,
 * reports the next bend and the one after it. Built once per ride.
 */
class TurnGuide(private val maneuvers: List<Maneuver>) {
    fun upcoming(distanceAlong: Double): UpcomingTurns? {
        val idx = maneuvers.indexOfFirst { it.distanceAlong > distanceAlong }
        if (idx < 0) return null
        val next = maneuvers[idx]
        val then = maneuvers.getOrNull(idx + 1)
        return UpcomingTurns(
            Cue(next.turn, next.distanceAlong - distanceAlong, next.name),
            then?.let { Cue(it.turn, it.distanceAlong - distanceAlong, it.name) },
        )
    }
}

/** Convenience: build a guide from a polyline by deriving maneuvers from its geometry. */
fun TurnGuide(planned: List<GeoPos>): TurnGuide = TurnGuide(detectManeuvers(planned))
