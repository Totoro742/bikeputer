package com.bikeputer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bikeputer.domain.RideState
import com.bikeputer.ui.theme.HeartRateColor
import com.bikeputer.ui.theme.HrZoneColors
import com.bikeputer.ui.theme.LocalBikeColors
import com.bikeputer.ui.theme.PowerZoneColors
import com.bikeputer.ui.theme.hrZoneColor
import com.bikeputer.ui.theme.hrZoneName
import com.bikeputer.ui.theme.powerZoneColor

/** B · Instrument — dense head-unit. Power field, 3×3 metric grid, HR-zone footer. */
@Composable
fun DashboardB(state: RideState, imperial: Boolean, ftp: Int, modifier: Modifier = Modifier) {
    val c = LocalBikeColors.current
    val zoneCol = powerZoneColor(state.powerZone) ?: c.dim
    val ftpPct = DashFormat.ftpPercent(state.instantPowerW, ftp)

    Column(
        modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        // session summary banner (no lap engine yet)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(c.accentSoft)
                .border(1.dp, c.border, RoundedCornerShape(9.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricLabel("SESSION", color = c.accent, size = 11)
            MetricNumber(
                "${DashFormat.duration(state.elapsedMs)}  ·  ${DashFormat.power(state.sessionAvgPowerW)} W",
                size = 16, color = c.text2, weight = FontWeight.Bold,
            )
        }

        // power hero field with a zone-coloured left rule
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .background(c.panel)
                .border(1.dp, c.border, RoundedCornerShape(12.dp)),
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(zoneCol))
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        MetricLabel("POWER")
                        MetricNumber(
                            DashFormat.power(state.instantPowerW),
                            size = 70, unit = "W", unitSize = 20,
                            color = if (state.powerConnected) c.text else c.dim,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        MetricLabel(
                            "NP ${DashFormat.power(state.normalizedPowerW)} · AVG ${DashFormat.power(state.sessionAvgPowerW)}",
                            size = 10, spacing = 1.0,
                        )
                        MetricNumber(
                            ftpPct?.let { "$it%" } ?: "––%",
                            size = 28, color = zoneCol, weight = FontWeight.Bold,
                            unit = "FTP", unitSize = 13,
                        )
                    }
                }
                ZoneBar(
                    PowerZoneColors, state.powerZone?.minus(1),
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }

        // 3×3 grid
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, c.border, RoundedCornerShape(12.dp)),
        ) {
            GridRow(
                GridCell("HR", DashFormat.int(state.heartRateBpm), if (state.hrConnected) HeartRateColor else c.dim),
                GridCell("CADENCE", DashFormat.int(state.cadenceRpm), c.text),
                GridCell("SPEED", DashFormat.speed(state.speedKmh, imperial), c.text),
            )
            HLine()
            GridRow(
                GridCell("DIST · ${DashFormat.distanceUnit(imperial)}", DashFormat.distance(state.distanceM, imperial), c.text),
                GridCell("TIME", DashFormat.duration(state.elapsedMs), c.text, small = true),
                GridCell("ASCENT · M", DashFormat.elevation(state.elevationGainM, imperial), c.text),
            )
            HLine()
            GridRow(
                GridCell("GRADE", "––", c.accent),
                GridCell("kJ", DashFormat.int(DashFormat.energyKj(state)), c.text),
                GridCell("CAL", DashFormat.int(DashFormat.energyKj(state)), c.text),
            )
        }

        Box(Modifier.weight(1f))

        // HR zone footer
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.panel)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val hrName = hrZoneName(state.hrZone)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricLabel("HEART-RATE ZONE", size = 10, spacing = 1.5)
                MetricLabel(
                    if (hrName != null) "Z${state.hrZone} · ${hrName.uppercase()}" else "NO HR",
                    color = hrZoneColor(state.hrZone) ?: c.dim, size = 10, spacing = 1.0,
                )
            }
            ZoneBar(HrZoneColors, state.hrZone?.minus(1), Modifier.fillMaxWidth().padding(top = 9.dp))
        }
    }
}

private data class GridCell(val label: String, val value: String, val color: Color, val small: Boolean = false)

@Composable
private fun GridRow(a: GridCell, b: GridCell, cc: GridCell) {
    val line = LocalBikeColors.current.line
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Cell(a, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(line))
        Cell(b, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(line))
        Cell(cc, Modifier.weight(1f))
    }
}

@Composable
private fun Cell(cell: GridCell, modifier: Modifier) {
    Column(modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
        MetricLabel(cell.label, size = 9, spacing = 1.5)
        MetricNumber(
            cell.value,
            size = if (cell.small) 28 else 38,
            color = cell.color,
            weight = FontWeight.Bold,
        )
    }
}
