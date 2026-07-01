package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RideState
import com.bikeputer.nav.DEFAULT_OFF_ROUTE_THRESHOLD_M
import com.bikeputer.nav.Maneuver
import com.bikeputer.nav.NavFix
import com.bikeputer.nav.RouteFollower
import com.bikeputer.nav.TurnClass
import com.bikeputer.nav.TurnGuide
import com.bikeputer.nav.UpcomingTurns
import com.bikeputer.nav.lookAheadMeters
import com.bikeputer.nav.online.NavStatus
import com.bikeputer.nav.online.RerouteConfig
import com.bikeputer.nav.online.RerouteController
import com.bikeputer.nav.online.RerouteRequest
import com.bikeputer.nav.online.RerouteState
import com.bikeputer.nav.online.RoutingClient
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import org.osmdroid.util.GeoPoint

/**
 * The live ride map hero: the base [RouteMap] plus the full navigation layer —
 * forward-biased route following, off-route detection, reroute state machine,
 * turn-by-turn guidance, fit-ahead camera window, and the overlay chrome
 * (turn card, off-route banner, progress/status chips, orientation/camera
 * toggles, recenter). Shared by [DashboardC] (as the ~55% map hero) and the
 * custom-grid map tile so both get identical navigation behaviour.
 *
 * The caller sizes the map via [modifier] (a weight/box); this composable fills it.
 */
@Composable
fun NavMap(
    state: RideState,
    imperial: Boolean,
    route: List<GeoPos>,
    planned: List<GeoPos> = emptyList(),
    onlineManeuvers: List<Maneuver>? = null,
    navStatus: NavStatus = NavStatus.Offline,
    rerouteClient: RoutingClient? = null,
    offRouteThresholdM: Int = DEFAULT_OFF_ROUTE_THRESHOLD_M,
    fitAheadCamera: Boolean = false,
    speedAdaptiveLookAhead: Boolean = false,
    defaultZoom: Int = 16,
    onToggleFitAhead: () -> Unit = {},
    headingUp: Boolean = false,
    onToggleHeadingUp: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LocalBikeColors.current
    var frozen by remember { mutableStateOf(false) }

    val lat = state.latitude
    val lng = state.longitude
    val points = remember(route) { route.map { GeoPoint(it.lat, it.lng) } }
    val plannedPoints = remember(planned) { planned.map { GeoPoint(it.lat, it.lng) } }
    val current = if (lat != null && lng != null) GeoPoint(lat, lng) else null

    val follower = remember(planned) { if (planned.size >= 2) RouteFollower(planned) else null }
    val navFix = remember(follower, lat, lng, offRouteThresholdM) {
        if (follower != null && lat != null && lng != null)
            follower.track(GeoPos(lat, lng), offRouteThresholdM.toDouble())
        else null
    }
    val offRoute = navFix != null && navFix.offRouteMeters > offRouteThresholdM
    var offRouteUi by remember { mutableStateOf(OffRouteUi.Hidden) }
    LaunchedEffect(offRoute) { offRouteUi = if (offRoute) OffRouteUi.Showing else OffRouteUi.Hidden }

    val onRoute = navFix != null && navFix.offRouteMeters <= offRouteThresholdM

    val rerouteController = remember(planned, offRouteThresholdM) {
        RerouteController(planned, RerouteConfig(offRouteThresholdM = offRouteThresholdM.toDouble()))
    }
    var rerouteState by remember(planned, offRouteThresholdM) { mutableStateOf<RerouteState>(RerouteState.Idle) }
    var rerouteFix by remember(planned, offRouteThresholdM) { mutableStateOf<NavFix?>(null) }
    val rerouteRequests = remember { Channel<RerouteRequest>(Channel.CONFLATED) }

    // Driven once per GPS fix. Keyed on navFix, so a value-identical fix (a truly stationary
    // rider) won't re-run this — the debounce/rejoin timers only advance as new fixes arrive,
    // which matches intent: a reroute fires once the rider actually moves off-route.
    LaunchedEffect(navFix) {
        val nf = navFix
        if (nf != null && lat != null && lng != null) {
            rerouteController.onFix(GeoPos(lat, lng), nf, rerouteClient != null, System.currentTimeMillis())
                ?.let { rerouteRequests.trySend(it) }
            rerouteState = rerouteController.state
            rerouteFix = rerouteController.rerouteNavFix
        }
    }
    LaunchedEffect(rerouteClient) {
        val client = rerouteClient ?: return@LaunchedEffect
        for (req in rerouteRequests) {
            try {
                rerouteController.onRerouteResult(client.directions(listOf(req.from, req.to)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("Bikeputer/Nav", "reroute fetch failed", e)
                rerouteController.onRerouteFailed()
            }
            rerouteState = rerouteController.state
            rerouteFix = rerouteController.rerouteNavFix
        }
    }

    val turnGuide = remember(planned, onlineManeuvers) {
        when {
            !onlineManeuvers.isNullOrEmpty() -> TurnGuide(onlineManeuvers)
            planned.size >= 2 -> TurnGuide(planned)
            else -> null
        }
    }
    val activeReroute = rerouteState as? RerouteState.Active
    val displayGuide = activeReroute?.guide ?: turnGuide
    val displayFix = if (activeReroute != null) rerouteFix else navFix
    val upcomingTurn = remember(displayGuide, displayFix) {
        if (displayGuide != null && displayFix != null) displayGuide.upcoming(displayFix.distanceAlong) else null
    }

    val aheadWindow = remember(
        navFix, follower, lat, lng, fitAheadCamera, speedAdaptiveLookAhead, state.speedKmh, offRouteThresholdM,
    ) {
        if (fitAheadCamera && follower != null && navFix != null && lat != null && lng != null &&
            navFix.offRouteMeters <= offRouteThresholdM
        ) {
            follower.lookAhead(GeoPos(lat, lng), lookAheadMeters(state.speedKmh, speedAdaptiveLookAhead))
                .map { GeoPoint(it.lat, it.lng) }
        } else {
            emptyList()
        }
    }

    Box(modifier.clipToBounds().background(c.bg2)) {
        RouteMap(
            route = points,
            current = current,
            modifier = Modifier.fillMaxSize(),
            planned = plannedPoints,
            aheadWindow = aheadWindow,
            headingUp = headingUp,
            bearingDeg = state.bearingDeg,
            frozen = frozen,
            defaultZoom = defaultZoom,
            onUserPan = { frozen = true },
        )
        // bottom-right: route progress (when navigating) stacked above ride distance
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (navFix != null) {
                NavProgressChip(navFix, imperial, Modifier.padding(bottom = 12.dp))
            }
            MetricLabel("DISTANCE", size = 9, spacing = 1.5)
            MetricNumber(
                DashFormat.distanceValue(state.distanceM, imperial),
                size = 26, color = Color(0xFFE6EAEE),
                unit = DashFormat.distanceUnit(state.distanceM, imperial).lowercase(), unitSize = 12,
                weight = FontWeight.Bold,
            )
        }
        if (offRouteUi == OffRouteUi.Showing && navFix != null && activeReroute == null) {
            OffRouteBanner(
                meters = navFix.offRouteMeters,
                imperial = imperial,
                onDismiss = { offRouteUi = OffRouteUi.Dismissed },
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
            )
        }
        if (upcomingTurn != null && (activeReroute != null || (onRoute && offRouteUi != OffRouteUi.Showing))) {
            NavTurnCard(
                upcoming = upcomingTurn,
                imperial = imperial,
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
            )
        }
        // top-left: orientation toggle (always), camera toggle (when route armed)
        Column(
            Modifier.align(Alignment.TopStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrientationToggle(headingUp = headingUp, onClick = onToggleHeadingUp)
            if (planned.isNotEmpty()) {
                CameraToggle(fitAhead = fitAheadCamera, onClick = onToggleFitAhead)
            }
        }
        if (frozen) {
            RecenterButton(
                onClick = { frozen = false },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }
        NavStatusChip(
            if (activeReroute != null) NavStatus.Rerouting else navStatus,
            Modifier.align(Alignment.TopEnd).padding(12.dp),
        )
    }
}

internal enum class OffRouteUi { Hidden, Showing, Dismissed }

@Composable
private fun NavProgressChip(navFix: NavFix, imperial: Boolean, modifier: Modifier) {
    val remaining = DashFormat.distanceValue(navFix.remainingMeters, imperial)
    val unit = DashFormat.distanceUnit(navFix.remainingMeters, imperial).lowercase()
    val pct = (navFix.fractionComplete * 100).roundToInt()
    Column(modifier, horizontalAlignment = Alignment.End) {
        MetricLabel("ROUTE", size = 9, spacing = 1.5)
        MetricNumber(
            remaining,
            size = 26, color = Color(0xFFE6EAEE),
            unit = "$unit left · $pct%", unitSize = 12, weight = FontWeight.Bold,
        )
    }
}

@Composable
private fun NavStatusChip(status: NavStatus, modifier: Modifier) {
    val c = LocalBikeColors.current
    val (label, tint) = when (status) {
        NavStatus.Online -> "ONLINE" to c.accent
        NavStatus.Offline -> "OFFLINE" to c.text2
        NavStatus.NoSignal -> "NO SIGNAL" to c.text2
        NavStatus.Rerouting -> "REROUTING…" to c.accent
    }
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.panel)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = tint, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun OffRouteBanner(meters: Double, imperial: Boolean, onDismiss: () -> Unit, modifier: Modifier) {
    val c = LocalBikeColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.accent)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (imperial) "OFF ROUTE · ${(meters * 3.28084).roundToInt()} ft" else "OFF ROUTE · ${meters.roundToInt()} m",
            color = c.bg, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
            fontSize = 12.sp, letterSpacing = 1.sp,
        )
        Text(
            "  ✕",
            color = c.bg, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        )
    }
}

@Composable
private fun OrientationToggle(headingUp: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalBikeColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (headingUp) "HEADING UP" else "NORTH UP",
            color = c.text2, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun RecenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalBikeColors.current
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "◎",
            color = c.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
    }
}

/** Rotation (degrees, clockwise from "up") for an up-arrow glyph per turn class. */
private fun TurnClass.arrowRotation(): Float = when (this) {
    TurnClass.SlightLeft -> -40f
    TurnClass.Left -> -80f
    TurnClass.SharpLeft -> -120f
    TurnClass.SlightRight -> 40f
    TurnClass.Right -> 80f
    TurnClass.SharpRight -> 120f
}

@Composable
private fun TurnArrow(turn: TurnClass, color: Color, sizeSp: Int) {
    Text(
        "↑",
        color = color,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = sizeSp.sp,
        modifier = Modifier.rotate(turn.arrowRotation()),
    )
}

@Composable
private fun NavTurnCard(upcoming: UpcomingTurns, imperial: Boolean, modifier: Modifier) {
    val c = LocalBikeColors.current
    val next = upcoming.next
    val distanceLabel = if (next.distanceM <= 15.0) {
        "NOW"
    } else {
        "${DashFormat.distanceValue(next.distanceM, imperial)} ${DashFormat.distanceUnit(next.distanceM, imperial).lowercase()}"
    }
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.panel)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TurnArrow(next.turn, c.accent, sizeSp = 30)
            Text(
                "  $distanceLabel",
                color = Color(0xFFE6EAEE),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
            )
        }
        next.name?.let { name ->
            Text(
                name,
                color = c.text2,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
            )
        }
        upcoming.then?.let { then ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "then ",
                    color = c.text2,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
                TurnArrow(then.turn, c.text2, sizeSp = 14)
            }
        }
    }
}

@Composable
private fun CameraToggle(fitAhead: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalBikeColors.current
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (fitAhead) "FIT AHEAD" else "FOLLOW",
            color = c.text2, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.sp,
        )
    }
}
