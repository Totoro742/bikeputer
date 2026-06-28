package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import com.bikeputer.nav.NavFix
import com.bikeputer.nav.RouteFollower
import com.bikeputer.nav.TurnGuide

/** Tunables for live rerouting. Defaults are the design's "Balanced" profile. */
data class RerouteConfig(
    val debounceMs: Long = 8_000,
    val minIntervalMs: Long = 20_000,
    val maxRequestsPerRide: Int = 15,
    val rejoinLookAheadCapM: Double = 2_000.0,
    val offRouteThresholdM: Double = 35.0,
)

/** A fetch the driver should perform: cycling directions [from] → [to]. */
data class RerouteRequest(val from: GeoPos, val to: GeoPos)

/** Live-reroute state. [Active] carries the throwaway route built from the detour geometry. */
sealed interface RerouteState {
    object Idle : RerouteState
    data class Pending(val sinceMs: Long) : RerouteState
    data class Active(
        val follower: RouteFollower,
        val guide: TurnGuide,
        val polyline: List<GeoPos>,
    ) : RerouteState
}

/**
 * Decides when to route the rider back to the GPX and tracks them along the detour.
 * Advance it once per GPS fix with [onFix]; when it returns a [RerouteRequest], the driver
 * fetches it via the [RoutingClient] and reports back with [onRerouteResult] / [onRerouteFailed].
 * All timing decisions take an explicit clock ([nowMs]) so the logic is unit-testable.
 */
class RerouteController(
    private val planned: List<GeoPos>,
    private val config: RerouteConfig = RerouteConfig(),
) {
    var state: RerouteState = RerouteState.Idle
        private set

    /** The rider projected onto the active detour; null unless [state] is [RerouteState.Active]. */
    var rerouteNavFix: NavFix? = null
        private set

    private var lastOnRouteAlong = 0.0
    private var lastRequestMs = Long.MIN_VALUE
    private var requestCount = 0
    private var lastCurrent: GeoPos? = null

    private fun rateLimitOk(nowMs: Long): Boolean =
        requestCount < config.maxRequestsPerRide &&
            (lastRequestMs == Long.MIN_VALUE || nowMs - lastRequestMs >= config.minIntervalMs)

    private fun issueRequest(current: GeoPos, nowMs: Long): RerouteRequest {
        lastRequestMs = nowMs
        requestCount++ // count at issue so a failed fetch still consumes a slot
        return RerouteRequest(current, rejoinTarget(planned, current, lastOnRouteAlong, config.rejoinLookAheadCapM))
    }

    fun onFix(current: GeoPos, mainNavFix: NavFix, onlineActive: Boolean, nowMs: Long): RerouteRequest? {
        lastCurrent = current
        val onRoute = mainNavFix.offRouteMeters <= config.offRouteThresholdM
        if (onRoute) lastOnRouteAlong = mainNavFix.distanceAlong

        if (!onlineActive || onRoute) {
            state = RerouteState.Idle
            rerouteNavFix = null
            return null
        }

        return when (val s = state) {
            is RerouteState.Idle -> {
                state = RerouteState.Pending(nowMs)
                null
            }
            is RerouteState.Pending ->
                if (nowMs - s.sinceMs >= config.debounceMs && rateLimitOk(nowMs)) issueRequest(current, nowMs)
                else null
            is RerouteState.Active -> {
                val fix = s.follower.track(current, config.offRouteThresholdM)
                rerouteNavFix = fix
                val driftedOff = fix == null || fix.offRouteMeters > config.offRouteThresholdM
                if (driftedOff && rateLimitOk(nowMs)) issueRequest(current, nowMs) else null
            }
        }
    }

    /** Install a fetched detour as a throwaway route and go [RerouteState.Active]. */
    fun onRerouteResult(directions: RouteDirections) {
        // The rider rejoined the GPX (or online went inactive) while this fetch was in flight,
        // resetting us to Idle. Installing the now-stale detour would briefly show the wrong
        // guidance, so drop it — a fresh off-route stretch will request a current one.
        if (state is RerouteState.Idle) return
        val poly = directions.geometry
        if (poly.size < 2) return // unusable; leave state so the offline floor keeps showing
        val follower = RouteFollower(poly)
        state = RerouteState.Active(
            follower = follower,
            guide = TurnGuide(onlineManeuvers(directions, poly)),
            polyline = poly,
        )
        // Seed the projection from the last known position so the detour card shows immediately
        // instead of blanking for one GPS tick until the next onFix tracks the rider.
        rerouteNavFix = lastCurrent?.let { follower.track(it, config.offRouteThresholdM) }
    }

    /** A fetch failed; the rate-limit slot was already consumed at issue time. */
    fun onRerouteFailed() { /* keep state; the offline floor shows; no counters to change */ }
}
