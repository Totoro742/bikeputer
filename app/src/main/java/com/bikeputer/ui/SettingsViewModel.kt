package com.bikeputer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeputer.data.DashboardLayout
import com.bikeputer.data.NavMode
import com.bikeputer.data.RiderSettings
import com.bikeputer.data.SettingsRepository
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
    fun setNavMode(mode: NavMode) = update { it.copy(navMode = mode) }
    fun setOrsApiKey(key: String) = update { it.copy(orsApiKey = key) }

    private fun update(transform: (RiderSettings) -> RiderSettings) {
        viewModelScope.launch { settings.update(transform) }
    }
}
