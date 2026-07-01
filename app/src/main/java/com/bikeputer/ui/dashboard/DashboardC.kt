package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RideState
import com.bikeputer.nav.DEFAULT_OFF_ROUTE_THRESHOLD_M
import com.bikeputer.nav.Maneuver
import com.bikeputer.nav.online.NavStatus
import com.bikeputer.nav.online.RoutingClient
import com.bikeputer.ui.theme.HeartRateColor
import com.bikeputer.ui.theme.HrZoneColors
import com.bikeputer.ui.theme.LocalBikeColors
import com.bikeputer.ui.theme.PowerZoneColors
import com.bikeputer.ui.theme.hrZoneColor
import com.bikeputer.ui.theme.powerZoneColor

/** C · Ride — map-forward & bold. ~55% map hero, 3×2 metric grid below. */
@Composable
fun DashboardC(
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

    Column(modifier.fillMaxSize()) {
        NavMap(
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
            modifier = Modifier.fillMaxWidth().weight(0.55f),
        )

        // 3×2 metric grid
        Column(Modifier.fillMaxWidth().weight(0.45f).background(c.bg2)) {
            CGridRow(
                CTile("POWER", DashFormat.power(state.instantPowerW),
                    if (state.powerConnected) c.text else c.dim,
                    sub = state.powerZone?.let { "Z$it · WATTS" } ?: "WATTS",
                    subColor = powerZoneColor(state.powerZone) ?: c.dim,
                    zone = state.powerZone),
                CTile("HEART RATE", DashFormat.int(state.heartRateBpm),
                    if (state.hrConnected) HeartRateColor else c.dim,
                    sub = state.hrZone?.let { "Z$it · BPM" } ?: "BPM",
                    subColor = hrZoneColor(state.hrZone) ?: c.dim,
                    zone = state.hrZone,
                    zoneColors = HrZoneColors),
                CTile("CADENCE", DashFormat.int(state.cadenceRpm), c.text, sub = "RPM", subColor = c.dim),
                Modifier.weight(1f),
            )
            HLine()
            CGridRow(
                CTile("SPEED", DashFormat.speed(state.speedKmh, imperial), c.text,
                    sub = DashFormat.speedUnit(imperial), subColor = c.dim),
                CTile("TIME", DashFormat.duration(state.elapsedMs), c.text, sub = "ELAPSED", subColor = c.dim, small = true),
                CTile("ASCENT", DashFormat.elevation(state.elevationGainM, imperial), c.text,
                    sub = DashFormat.elevationUnit(imperial), subColor = c.dim),
                Modifier.weight(1f),
            )
        }
    }
}

private data class CTile(
    val label: String,
    val value: String,
    val color: Color,
    val sub: String,
    val subColor: Color,
    val zone: Int? = null,
    val zoneColors: List<Color> = PowerZoneColors,
    val small: Boolean = false,
)

@Composable
private fun CGridRow(a: CTile, b: CTile, cc: CTile, rowMod: Modifier) {
    val line = LocalBikeColors.current.line
    Row(rowMod.fillMaxWidth().height(IntrinsicSize.Min)) {
        CCell(a, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(line))
        CCell(b, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(line))
        CCell(cc, Modifier.weight(1f))
    }
}

@Composable
private fun CCell(tile: CTile, modifier: Modifier) {
    Column(
        modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MetricLabel(tile.label, size = 10, spacing = 2.0)
        MetricNumber(
            tile.value,
            size = if (tile.small) 36 else 50,
            color = tile.color,
            weight = FontWeight.ExtraBold,
        )
        MetricLabel(tile.sub, color = tile.subColor, size = 10, spacing = 1.0)
        if (tile.zone != null) {
            ZoneBar(
                tile.zoneColors, tile.zone - 1,
                Modifier.width(86.dp).padding(top = 6.dp), height = 5.dp,
            )
        }
    }
}
