package com.bikeputer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bikeputer.domain.CustomGrid
import com.bikeputer.domain.MapTile
import com.bikeputer.domain.MetricId
import com.bikeputer.domain.MetricTile
import com.bikeputer.domain.RideState
import com.bikeputer.domain.canPlace
import com.bikeputer.domain.cellAt
import com.bikeputer.domain.clear
import com.bikeputer.domain.place
import com.bikeputer.domain.setRows
import com.bikeputer.ui.dashboard.CustomDashboard
import com.bikeputer.ui.dashboard.MetricCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridEditorScreen(
    grid: CustomGrid,
    onSave: (CustomGrid) -> Unit,
    onBack: () -> Unit,
) {
    var working by remember { mutableStateOf(grid) }
    var picking by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (row, col) being edited

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        // header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Cancel") }
            TextButton(onClick = { working = working.setRows(if (working.rows == 6) 5 else 6) }) {
                Text("Rows: ${working.rows}")
            }
            TextButton(onClick = { onSave(working); onBack() }) { Text("Save") }
        }

        // live editable grid (sample state so tiles show something)
        CustomDashboard(
            grid = working,
            state = SAMPLE_STATE,
            imperial = false,
            ftp = 200,
            editing = true,
            onCellTap = { r, c -> picking = r to c },
            modifier = Modifier.weight(1f),
        )
    }

    val cell = picking
    if (cell != null) {
        val (row, col) = cell
        ModalBottomSheet(onDismissRequest = { picking = null }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                // clear / map options
                TextButton(onClick = { working = working.clear(row, col); picking = null }) { Text("Clear cell") }
                // Map only offered at col 0 where a 3×3 fits once the tapped cell is cleared
                val afterClear = working.clear(row, col)
                if (col == 0 && afterClear.canPlace(MapTile, row, 0)) {
                    TextButton(onClick = { working = afterClear.place(MapTile, row, 0); picking = null }) { Text("Map") }
                }
                // every metric
                MetricId.entries.forEach { id ->
                    TextButton(onClick = {
                        working = working.clear(row, col).place(MetricTile(id), row, col)
                        picking = null
                    }) { Text(MetricCatalog.label(id, imperial = false)) }
                }
            }
        }
    }
}

private val SAMPLE_STATE = RideState.EMPTY.copy(
    instantPowerW = 243, cadenceRpm = 88, heartRateBpm = 148, speedKmh = 31.4,
    avgPower3sW = 250, avgPower10sW = 240, normalizedPowerW = 255, sessionAvgPowerW = 210,
    elapsedMs = 3_725_000L, distanceM = 24_300.0, avgSpeedKmh = 27.0, maxSpeedKmh = 52.0,
    elevationGainM = 410.0, avgHeartRateBpm = 142, powerZone = 4, hrZone = 3,
    powerConnected = true, hrConnected = true, gpsAvailable = true,
)
