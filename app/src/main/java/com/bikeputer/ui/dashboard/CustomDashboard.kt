package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bikeputer.domain.CustomGrid
import com.bikeputer.domain.GRID_COLUMNS
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.MapTile
import com.bikeputer.domain.MetricTile
import com.bikeputer.domain.RideState
import com.bikeputer.domain.TileContent
import com.bikeputer.domain.cellAt
import com.bikeputer.nav.DEFAULT_OFF_ROUTE_THRESHOLD_M
import com.bikeputer.nav.Maneuver
import com.bikeputer.nav.online.NavStatus
import com.bikeputer.nav.online.RoutingClient
import com.bikeputer.ui.theme.HeartRateColor
import com.bikeputer.ui.theme.LocalBikeColors
import com.bikeputer.ui.theme.hrZoneColor
import com.bikeputer.ui.theme.powerZoneColor

@Composable
fun CustomDashboard(
    grid: CustomGrid,
    state: RideState,
    imperial: Boolean,
    ftp: Int,
    route: List<GeoPos> = emptyList(),
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
    editing: Boolean = false,
    onCellTap: (row: Int, col: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val c = LocalBikeColors.current
    Column(modifier.fillMaxSize().background(c.bg).padding(6.dp)) {
        var r = 0
        while (r < grid.rows) {
            val mapHere = grid.cellAt(r, 0)?.takeIf { it.content is MapTile && it.row == r }
            if (mapHere != null) {
                MapRow(
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
                    editing = editing,
                    onTap = { onCellTap(mapHere.row, 0) },
                )
                r += 3
            } else {
                MetricRow(grid, r, state, imperial, ftp, editing, onCellTap)
                r += 1
            }
        }
    }
}

@Composable
private fun ColumnScope.MetricRow(
    grid: CustomGrid,
    row: Int,
    state: RideState,
    imperial: Boolean,
    ftp: Int,
    editing: Boolean,
    onCellTap: (Int, Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (col in 0 until GRID_COLUMNS) {
            MetricCell(
                content = grid.cellAt(row, col)?.content,
                state = state,
                imperial = imperial,
                ftp = ftp,
                editing = editing,
                onTap = { onCellTap(row, col) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ColumnScope.MapRow(
    state: RideState,
    imperial: Boolean,
    route: List<GeoPos>,
    planned: List<GeoPos>,
    onlineManeuvers: List<Maneuver>?,
    navStatus: NavStatus,
    rerouteClient: RoutingClient?,
    offRouteThresholdM: Int,
    fitAheadCamera: Boolean,
    speedAdaptiveLookAhead: Boolean,
    defaultZoom: Int,
    onToggleFitAhead: () -> Unit,
    headingUp: Boolean,
    onToggleHeadingUp: () -> Unit,
    editing: Boolean,
    onTap: () -> Unit,
) {
    val c = LocalBikeColors.current

    Box(
        Modifier.fillMaxWidth().weight(3f).clip(RoundedCornerShape(10.dp))
            .let {
                if (editing) it.border(1.dp, c.accent, RoundedCornerShape(10.dp)).clickable(onClick = onTap) else it
            },
    ) {
        if (editing) {
            // Static placeholder while editing — avoids spinning up osmdroid in the editor.
            Box(Modifier.fillMaxSize().background(c.bg2), Alignment.Center) {
                MetricLabel("MAP", size = 12)
            }
        } else {
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
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MetricCell(
    content: TileContent?,
    state: RideState,
    imperial: Boolean,
    ftp: Int,
    editing: Boolean,
    onTap: () -> Unit,
    modifier: Modifier,
) {
    val c = LocalBikeColors.current
    val base = modifier.clip(RoundedCornerShape(8.dp))
        .let { if (editing) it.border(1.dp, c.border, RoundedCornerShape(8.dp)).clickable(onClick = onTap) else it }
    if (content is MetricTile) {
        val r = MetricCatalog.read(content.metric, state, imperial, ftp)
        Column(base.background(c.bg2).padding(horizontal = 10.dp, vertical = 8.dp)) {
            MetricLabel(r.label, size = 9, spacing = 1.0)
            MetricNumber(
                r.value, size = 30, color = colorFor(r.color, state),
                weight = FontWeight.Bold, unit = r.unit, unitSize = 13,
            )
        }
    } else {
        // Empty cell — show "+" affordance in editing mode, invisible otherwise.
        Box(base.let { if (editing) it.background(c.bg2) else it }, Alignment.Center) {
            if (editing) MetricLabel("+", size = 18, color = c.dim)
        }
    }
}

@Composable
private fun colorFor(role: ColorRole, state: RideState): Color {
    val c = LocalBikeColors.current
    return when (role) {
        ColorRole.Text -> c.text
        ColorRole.Dim -> c.dim
        ColorRole.HeartRate -> HeartRateColor
        ColorRole.PowerZone -> powerZoneColor(state.powerZone) ?: c.dim
        ColorRole.HrZone -> hrZoneColor(state.hrZone) ?: c.dim
        ColorRole.Accent -> c.accent
    }
}
