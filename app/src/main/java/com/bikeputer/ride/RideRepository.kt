package com.bikeputer.ride

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import com.bikeputer.domain.RidePhase
import com.bikeputer.domain.RideState
import com.bikeputer.metrics.MetricsEngine
import com.bikeputer.metrics.ZoneModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class RideRepository(
    private val power: PowerSource,
    private val heartRate: HeartRateSource,
    private val location: LocationProvider,
    private val powerZones: ZoneModel?,
    private val hrZones: ZoneModel?,
) {
    /**
     * Raw location stream, exposed so a consumer can accumulate the route
     * un-conflated. The real source is a hot SharedFlow, so a second collector
     * adds no extra GPS subscription; the merged [rideState] pipeline shares it.
     */
    val locationSamples: Flow<LocationSample> get() = location.samples

    private sealed interface Event {
        data class Power(val s: PowerSample) : Event
        data class Hr(val s: HeartRateSample) : Event
        data class Loc(val s: LocationSample) : Event
        data class Tick(val ms: Long) : Event
    }

    // Emits a fresh RideState whenever any source emits OR elapsedMsFlow ticks.
    // All events are merged into one sequentially-collected flow, so the single
    // MetricsEngine is only ever touched by one coroutine — safe on any dispatcher.
    fun rideState(
        elapsedMsFlow: Flow<Long>,
        phase: StateFlow<RidePhase> = MutableStateFlow(RidePhase.Recording),
    ): Flow<RideState> = flow {
        val engine = MetricsEngine(powerZones, hrZones)
        var elapsedMs = 0L

        merge(
            power.samples.map { Event.Power(it) },
            heartRate.samples.map { Event.Hr(it) },
            location.samples.map { Event.Loc(it) },
            elapsedMsFlow.map { Event.Tick(it) },
        ).collect { event ->
            val recording = phase.value == RidePhase.Recording
            when (event) {
                is Event.Power -> if (recording) engine.onPower(event.s)
                is Event.Hr -> if (recording) engine.onHeartRate(event.s)
                is Event.Loc -> if (recording) engine.onLocation(event.s)
                is Event.Tick -> elapsedMs = event.ms
            }
            emit(
                engine.snapshot(
                    elapsedMs = elapsedMs,
                    powerConnected = power.connected.value,
                    hrConnected = heartRate.connected.value,
                    gpsAvailable = location.available.value,
                )
            )
        }
    }
}
