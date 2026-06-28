package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RideState
import com.bikeputer.nav.DEFAULT_OFF_ROUTE_THRESHOLD_M
import com.bikeputer.nav.routeCoverage
import kotlin.math.roundToInt
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors
import org.osmdroid.util.GeoPoint

/** End-of-ride summary: a static route snapshot, core stats, and a Done button. */
@Composable
fun RideSummaryScreen(
    state: RideState,
    route: List<GeoPos>,
    imperial: Boolean,
    onDone: () -> Unit,
    planned: List<GeoPos> = emptyList(),
    offRouteThresholdM: Int = DEFAULT_OFF_ROUTE_THRESHOLD_M,
) {
    val c = LocalBikeColors.current
    val points = route.map { GeoPoint(it.lat, it.lng) }
    val plannedPoints = planned.map { GeoPoint(it.lat, it.lng) }
    val coveragePercent = remember(planned, route, offRouteThresholdM) {
        if (planned.isEmpty()) null
        else (routeCoverage(planned, route, offRouteThresholdM.toDouble()) * 100).roundToInt()
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Text(
            "RIDE SUMMARY",
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = c.dim,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )

        Box(Modifier.fillMaxWidth().weight(0.45f).clipToBounds()) {
            RouteMap(route = points, current = null, modifier = Modifier.fillMaxSize(), follow = false, planned = plannedPoints)
        }

        Column(Modifier.fillMaxWidth().weight(0.45f).padding(horizontal = 16.dp)) {
            SummaryRow("DISTANCE", DashFormat.distance(state.distanceM, imperial), DashFormat.distanceUnit(imperial))
            SummaryRow("TIME", DashFormat.duration(state.elapsedMs), "")
            SummaryRow("AVG SPEED", DashFormat.speed(state.avgSpeedKmh, imperial), DashFormat.speedUnit(imperial))
            SummaryRow("MAX SPEED", DashFormat.speed(state.maxSpeedKmh, imperial), DashFormat.speedUnit(imperial))
            SummaryRow("AVG POWER", DashFormat.power(state.sessionAvgPowerW), "W")
            SummaryRow("NORMALIZED POWER", DashFormat.power(state.normalizedPowerW), "W")
            SummaryRow("ASCENT", DashFormat.elevation(state.elevationGainM, imperial), DashFormat.elevationUnit(imperial))
            SummaryRow("AVG HR", DashFormat.int(state.avgHeartRateBpm), "BPM")
            if (coveragePercent != null) {
                SummaryRow("ROUTE FOLLOWED", coveragePercent.toString(), "%")
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.accent)
                .clickable(onClick = onDone)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "DONE",
                color = c.bg,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, unit: String) {
    val c = LocalBikeColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = c.dim,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                color = c.text,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            if (unit.isNotEmpty()) {
                Text(
                    " $unit",
                    modifier = Modifier.padding(start = 4.dp),
                    color = c.dim,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
