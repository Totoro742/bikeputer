package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.bikeputer.data.DashboardLayout
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RideState
import com.bikeputer.nav.DEFAULT_OFF_ROUTE_THRESHOLD_M
import com.bikeputer.nav.Maneuver
import com.bikeputer.nav.online.NavStatus
import com.bikeputer.nav.online.RoutingClient
import com.bikeputer.ui.theme.LocalBikeColors

/**
 * The live ride screen: shared chrome (clock, REC/paused, pause control) over
 * the [layout]-selected dashboard. Fed entirely by [state].
 */
@Composable
fun RideDashboard(
    state: RideState,
    layout: DashboardLayout,
    imperial: Boolean,
    ftp: Int,
    paused: Boolean,
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
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalBikeColors.current
    Column(modifier.fillMaxSize().background(c.bg)) {
        RideTopBar(
            state = state,
            paused = paused,
            onBack = onBack,
            onTogglePause = onTogglePause,
        )
        // Clip the content area so the osmdroid MapView (which over-draws past its
        // bounds) can't paint up over the top bar.
        Box(Modifier.weight(1f).clipToBounds()) {
            when (layout) {
                DashboardLayout.A -> DashboardA(state, imperial, ftp)
                DashboardLayout.B -> DashboardB(state, imperial, ftp)
                // TEMPORARY fallback — Task 5 replaces this with CustomDashboard.
                DashboardLayout.Custom -> DashboardA(state, imperial, ftp)
                DashboardLayout.C -> DashboardC(
                    state = state,
                    imperial = imperial,
                    route = route,
                    planned = planned,
                    onlineManeuvers = onlineManeuvers,
                    navStatus = navStatus,
                    rerouteClient = rerouteClient,
                    offRouteThresholdM = offRouteThresholdM,
                    fitAheadCamera = fitAheadCamera,
                    speedAdaptiveLookAhead = speedAdaptiveLookAhead,
                    defaultZoom = defaultZoom,
                    onToggleFitAhead = onToggleFitAhead,
                    headingUp = headingUp,
                    onToggleHeadingUp = onToggleHeadingUp,
                )
            }
        }
    }
}
