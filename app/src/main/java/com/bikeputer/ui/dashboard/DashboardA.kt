package com.bikeputer.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bikeputer.domain.RideState
import com.bikeputer.ui.theme.HeartRateColor
import com.bikeputer.ui.theme.LocalBikeColors
import com.bikeputer.ui.theme.PowerZoneColors
import com.bikeputer.ui.theme.powerZoneColor
import com.bikeputer.ui.theme.powerZoneName

/** A · Glance — huge, minimal. One hero power metric plus paired big readouts. */
@Composable
fun DashboardA(state: RideState, imperial: Boolean, ftp: Int, modifier: Modifier = Modifier) {
    val c = LocalBikeColors.current
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp)) {

        // hero power
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MetricLabel("POWER · WATTS", spacing = 3.0)
            MetricNumber(
                DashFormat.power(state.instantPowerW),
                size = 118,
                color = if (state.powerConnected) c.text else c.dim,
            )
            ZoneBar(
                PowerZoneColors,
                state.powerZone?.minus(1),
                Modifier.fillMaxWidth().padding(top = 14.dp),
                height = 9.dp,
                gap = 3.dp,
            )
            val zName = powerZoneName(state.powerZone)
            val ftpPct = DashFormat.ftpPercent(state.instantPowerW, ftp)
            if (zName != null) {
                MetricLabel(
                    "Z${state.powerZone} · ${zName.uppercase()}" +
                        (ftpPct?.let { " · $it% FTP" } ?: ""),
                    color = powerZoneColor(state.powerZone) ?: c.dim,
                    modifier = Modifier.padding(top = 10.dp),
                    size = 12,
                )
            }
        }

        HLine(Modifier.padding(vertical = 16.dp))
        PairRow(
            "HEART RATE", DashFormat.int(state.heartRateBpm),
            if (state.hrConnected) HeartRateColor else c.dim, "bpm",
            "CADENCE", DashFormat.int(state.cadenceRpm), c.text, "rpm",
        )
        HLine(Modifier.padding(vertical = 16.dp))
        PairRow(
            "SPEED · ${DashFormat.speedUnit(imperial)}", DashFormat.speed(state.speedKmh, imperial),
            if (state.gpsAvailable) c.text else c.dim, null,
            "DIST · ${DashFormat.distanceUnit(imperial)}",
            DashFormat.distance(state.distanceM, imperial), c.text, null,
        )

        Column(Modifier.weight(1f)) {}

        // bottom strip
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.bg2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StripCell("ELAPSED", DashFormat.duration(state.elapsedMs), c.text2, Modifier.weight(1f))
            CellDivider()
            StripCell("AVG PWR", DashFormat.power(state.sessionAvgPowerW), c.text2, Modifier.weight(1f))
            CellDivider()
            StripCell("kJ", DashFormat.int(DashFormat.energyKj(state)), c.text2, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PairRow(
    lLabel: String, lValue: String, lColor: androidx.compose.ui.graphics.Color, lUnit: String?,
    rLabel: String, rValue: String, rColor: androidx.compose.ui.graphics.Color, rUnit: String?,
) {
    val c = LocalBikeColors.current
    Row(Modifier.fillMaxWidth()) {
        PairCell(lLabel, lValue, lColor, lUnit, Modifier.weight(1f))
        Column(Modifier.width(1.dp).background(c.line)) {}
        PairCell(rLabel, rValue, rColor, rUnit, Modifier.weight(1f))
    }
}

@Composable
private fun PairCell(
    label: String, value: String, color: androidx.compose.ui.graphics.Color, unit: String?,
    modifier: Modifier,
) {
    Column(modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MetricLabel(label, align = TextAlign.Center)
        MetricNumber(value, size = 66, color = color, unit = unit, unitSize = 18)
    }
}

@Composable
private fun StripCell(
    label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier,
) {
    Column(modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MetricLabel(label, size = 10)
        MetricNumber(value, size = 28, color = color, weight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
private fun CellDivider() {
    Column(Modifier.width(1.dp).background(LocalBikeColors.current.line)) {
        androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = 22.dp))
    }
}
