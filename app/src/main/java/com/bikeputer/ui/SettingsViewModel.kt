package com.bikeputer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeputer.data.DashboardLayout
import com.bikeputer.data.NavMode
import com.bikeputer.data.RiderSettings
import com.bikeputer.data.SettingsRepository
import com.bikeputer.domain.CustomGrid
import com.bikeputer.ui.dashboard.MapZoom
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Drives the settings screen; every change persists immediately via DataStore. */
class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val riderSettings: StateFlow<RiderSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, RiderSettings())

    fun setLayout(layout: DashboardLayout) = update { it.copy(layout = layout) }
    fun setDark(dark: Boolean) = update { it.copy(darkTheme = dark) }
    fun setImperial(imperial: Boolean) = update { it.copy(useImperial = imperial) }

    fun adjustFtp(delta: Int) = update { it.copy(ftp = (it.ftp + delta).coerceIn(80, 500)) }
    fun adjustMaxHr(delta: Int) = update { it.copy(maxHr = (it.maxHr + delta).coerceIn(120, 220)) }
    fun adjustOffRouteThreshold(delta: Int) =
        update { it.copy(offRouteThresholdM = (it.offRouteThresholdM + delta).coerceIn(10, 100)) }
    fun setSpeedAdaptiveLookAhead(adaptive: Boolean) =
        update { it.copy(speedAdaptiveLookAhead = adaptive) }
    fun adjustDefaultMapZoom(delta: Int) =
        update { it.copy(defaultMapZoom = MapZoom.coerce(it.defaultMapZoom + delta)) }
    fun setNavMode(mode: NavMode) = update { it.copy(navMode = mode) }
    fun setOrsApiKey(key: String) = update { it.copy(orsApiKey = key) }

    fun setActiveCustomGrid(id: String) = update { it.copy(activeCustomGridId = id) }

    /** Adds a fresh grid seeded from DEFAULT, makes it active, returns its id. */
    fun addCustomGrid(): String {
        val id = System.currentTimeMillis().toString()
        update {
            val next = CustomGrid.DEFAULT.copy(id = id, name = "Grid ${it.customGrids.size + 1}")
            it.copy(customGrids = it.customGrids + next, activeCustomGridId = id)
        }
        return id
    }

    fun deleteCustomGrid(id: String) = update {
        val remaining = it.customGrids.filterNot { g -> g.id == id }
        it.copy(
            customGrids = remaining.ifEmpty { listOf(CustomGrid.DEFAULT) },
            activeCustomGridId = if (it.activeCustomGridId == id) remaining.firstOrNull()?.id else it.activeCustomGridId,
        )
    }

    fun renameCustomGrid(id: String, name: String) = update {
        it.copy(customGrids = it.customGrids.map { g -> if (g.id == id) g.copy(name = name) else g })
    }

    fun saveCustomGrid(grid: CustomGrid) = update {
        it.copy(customGrids = it.customGrids.map { g -> if (g.id == grid.id) grid else g })
    }

    private fun update(transform: (RiderSettings) -> RiderSettings) {
        viewModelScope.launch { settings.update(transform) }
    }
}
