package com.bikeputer.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.bikeputer.domain.HeartRateSample
import com.bikeputer.ride.HeartRateSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.UUID

class HeartRateSensor(context: Context) : HeartRateSource {

    private val hrService = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val hrChar   = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    private val _samples = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 8)
    override val samples: Flow<HeartRateSample> = _samples

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean> = _connected

    private val manager = object : BleManager(context) {

        private var hr: BluetoothGattCharacteristic? = null

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            hr = gatt.getService(hrService)?.getCharacteristic(hrChar)
            return hr != null
        }

        override fun initialize() {
            setNotificationCallback(hr).with { _, data ->
                val bytes = data.value ?: return@with
                _samples.tryEmit(
                    HeartRateSample(System.currentTimeMillis(), HeartRateParser.parse(bytes))
                )
            }
            enableNotifications(hr).enqueue()
        }

        override fun onServicesInvalidated() {
            hr = null
        }
    }

    init {
        manager.setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) = Unit
            override fun onDeviceConnected(device: BluetoothDevice) = Unit
            override fun onDeviceReady(device: BluetoothDevice) {
                _connected.value = true
            }
            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                _connected.value = false
            }
            override fun onDeviceDisconnecting(device: BluetoothDevice) = Unit
            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                _connected.value = false
            }
        })
    }

    suspend fun connect(device: BluetoothDevice) {
        manager.connect(device)
            .retry(3, 200)
            .useAutoConnect(true)
            .suspend()
    }

    /**
     * Releases the underlying GATT client. Must be called when the sensor is no
     * longer needed. Without this each ride leaks a [BluetoothGatt] client (and its
     * auto-reconnect keeps competing for the strap's single connection); after a few
     * rides Android's client pool is exhausted and connections stop delivering data.
     */
    fun close() {
        runCatching { manager.disconnect().enqueue() }
        manager.close()
        _connected.value = false
    }
}
