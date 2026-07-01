package com.bikeputer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bikeputer.ble.HeartRateSensor
import com.bikeputer.ble.PowerSensor
import com.bikeputer.data.SettingsRepository
import com.bikeputer.location.LocationSource
import com.bikeputer.metrics.Zones
import com.bikeputer.ride.RideRepository
import com.bikeputer.ride.RideSession
import com.bikeputer.ride.SessionTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class ServiceStep { Stop, StartAndBuild, StartOnly }

internal fun decideStep(action: String?, hasSession: Boolean, starting: Boolean): ServiceStep =
    when {
        action == RideService.ACTION_STOP -> ServiceStep.Stop
        !hasSession && !starting -> ServiceStep.StartAndBuild
        else -> ServiceStep.StartOnly
    }

class RideService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var liveLocation: LocationSource? = null
    private var powerSensor: PowerSensor? = null
    private var hrSensor: HeartRateSensor? = null
    @Volatile private var starting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (decideStep(intent?.action, _activeSession.value != null, starting)) {
            ServiceStep.Stop -> {
                teardown()
                stopSelf()
                START_NOT_STICKY
            }
            ServiceStep.StartAndBuild -> {
                startForeground(1, buildNotification())
                starting = true
                // Build on the main thread (LocationSource.start() needs a Looper),
                // but read settings off it so onStartCommand never blocks on file I/O.
                serviceScope.launch(Dispatchers.Main.immediate) { startRide() }
                START_STICKY
            }
            ServiceStep.StartOnly -> {
                startForeground(1, buildNotification())
                START_STICKY
            }
        }
    }

    private fun teardown() {
        liveLocation?.stop()
        liveLocation = null
        _activeSession.value?.close()
        _activeSession.value = null
        // Release the GATT clients — otherwise every ride leaks one per sensor and
        // Android eventually stops delivering sensor data (see HeartRateSensor.close).
        powerSensor?.close()
        powerSensor = null
        hrSensor?.close()
        hrSensor = null
        starting = false
    }

    private suspend fun startRide() {
        val settingsRepo = SettingsRepository(applicationContext)
        val s = withContext(Dispatchers.IO) { settingsRepo.settings.first() }

        val power = PowerSensor(applicationContext)
        val hr = HeartRateSensor(applicationContext)
        powerSensor = power
        hrSensor = hr
        val location = LocationSource(applicationContext)
        liveLocation = location
        runCatching { location.start() }

        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        serviceScope.launch {
            s.powerSensorMac?.let { mac -> runCatching { power.connect(adapter.getRemoteDevice(mac)) } }
            s.hrSensorMac?.let { mac -> runCatching { hr.connect(adapter.getRemoteDevice(mac)) } }
        }

        val repo = RideRepository(
            power = power,
            heartRate = hr,
            location = location,
            powerZones = Zones.powerZonesFromFtp(s.ftp),
            hrZones = Zones.hrZonesFromMax(s.maxHr),
        )
        val session = RideSession(repo, SessionTimer(), serviceScope)
        session.start()
        _activeSession.value = session
    }

    override fun onDestroy() {
        super.onDestroy()
        teardown()
        serviceScope.cancel()
    }

    private fun buildNotification(): Notification {
        val channelId = "ride"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Ride", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bikeputer")
            .setContentText("Recording live metrics")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.bikeputer.action.START"
        const val ACTION_STOP = "com.bikeputer.action.STOP"

        private val _activeSession = MutableStateFlow<RideSession?>(null)
        val activeSession: StateFlow<RideSession?> = _activeSession
    }
}
