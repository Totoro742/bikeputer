package com.bikeputer.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class ScannedDevice(val name: String?, val address: String)

/**
 * Scans for BLE peripherals advertising [serviceUuid] and exposes the
 * de-duplicated results as a StateFlow. Requires BLUETOOTH_SCAN (and
 * BLUETOOTH_CONNECT, to read device names) to be granted before [start].
 */
class BleScanner(context: Context, private val serviceUuid: UUID) {

    private val scanner =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter?.bluetoothLeScanner

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    private val found = LinkedHashMap<String, ScannedDevice>()

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName
            found[device.address] = ScannedDevice(name, device.address)
            _devices.value = found.values.toList()
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val s = scanner ?: return
        if (_scanning.value) return
        found.clear()
        _devices.value = emptyList()
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        _scanning.value = true
        s.startScan(listOf(filter), settings, callback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val s = scanner ?: return
        if (!_scanning.value) return
        s.stopScan(callback)
        _scanning.value = false
    }

    companion object {
        val CYCLING_POWER_SERVICE: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    }
}
