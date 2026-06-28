package com.bikeputer.location

import android.annotation.SuppressLint
import android.content.Context
import com.bikeputer.domain.LocationSample
import com.bikeputer.ride.LocationProvider
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationSource(
    context: Context,
    staleMs: Long = 3000,
) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val _samples = MutableSharedFlow<LocationSample>(extraBufferCapacity = 8)
    override val samples: Flow<LocationSample> = _samples
    private val _available = MutableStateFlow(false)
    override val available: StateFlow<Boolean> = _available

    private val staleness = GpsStalenessTracker(staleMs)
    private var watchdog: CoroutineScope? = null

    private val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            val now = System.currentTimeMillis()
            staleness.onSample(now)
            _available.value = true
            _samples.tryEmit(
                LocationSample(
                    timestampMs = now,
                    lat = loc.latitude,
                    lng = loc.longitude,
                    altitudeM = if (loc.hasAltitude()) loc.altitude else 0.0,
                    speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                    bearingDeg = if (loc.hasBearing() && loc.speed > 0.5f) loc.bearing else null,
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        watchdog?.cancel()
        client.requestLocationUpdates(request, callback, null)
        val scope = CoroutineScope(Dispatchers.Default)
        watchdog = scope
        scope.launch {
            while (isActive) {
                delay(1000)
                _available.value = staleness.isAvailable(System.currentTimeMillis())
            }
        }
    }

    fun stop() {
        client.removeLocationUpdates(callback)
        watchdog?.cancel()
        watchdog = null
        _available.value = false
    }
}
