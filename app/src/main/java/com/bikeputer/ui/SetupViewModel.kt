package com.bikeputer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeputer.ble.BleScanner
import com.bikeputer.data.RiderSettings
import com.bikeputer.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the sensor-setup screen: exposes the current saved settings and the
 * two service-filtered scanners, and persists the user's sensor choices.
 */
class SetupViewModel(
    private val settings: SettingsRepository,
    val powerScanner: BleScanner,
    val hrScanner: BleScanner,
) : ViewModel() {

    val riderSettings: StateFlow<RiderSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, RiderSettings())

    fun startPowerScan() = powerScanner.start()
    fun stopPowerScan() = powerScanner.stop()
    fun startHrScan() = hrScanner.start()
    fun stopHrScan() = hrScanner.stop()

    fun selectPower(mac: String?) {
        viewModelScope.launch { settings.update { it.copy(powerSensorMac = mac) } }
    }

    fun selectHr(mac: String?) {
        viewModelScope.launch { settings.update { it.copy(hrSensorMac = mac) } }
    }

    override fun onCleared() {
        powerScanner.stop()
        hrScanner.stop()
    }
}
