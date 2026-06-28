package com.bikeputer.ride

import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RidePhase
import com.bikeputer.domain.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns one live ride pipeline: the [RideRepository] metrics flow plus the
 * [SessionTimer], collected eagerly in [scope]. Owns the authoritative ride
 * [phase]; the repository reads it to freeze metric accumulation while paused
 * or finished. When [scope] is the foreground service scope the pipeline keeps
 * running while the Activity is backgrounded; the ViewModel just observes.
 */
class RideSession(
    repository: RideRepository,
    private val timer: SessionTimer,
    private val scope: CoroutineScope,
    now: () -> Long = { System.currentTimeMillis() },
) {
    private val _phase = MutableStateFlow(RidePhase.Recording)
    val phase: StateFlow<RidePhase> = _phase

    val state: StateFlow<RideState> =
        repository.rideState(timer.elapsedMs(now), _phase)
            // Eagerly: the metrics engine is session-scoped and must not be torn
            // down mid-ride (e.g. when the Activity stops observing).
            .stateIn(scope, SharingStarted.Eagerly, RideState.EMPTY)

    private val _route = MutableStateFlow<List<GeoPos>>(emptyList())
    val route: StateFlow<List<GeoPos>> = _route

    init {
        // Accumulate from the raw (un-conflated) location stream so no fix is
        // dropped if several arrive in one dispatch; the conflated [state] would
        // collapse them. Gated on phase so a paused/finished ride stops growing.
        scope.launch {
            repository.locationSamples.collect { s ->
                if (_phase.value != RidePhase.Recording) return@collect
                val last = _route.value.lastOrNull()
                if (last == null || last.lat != s.lat || last.lng != s.lng) {
                    _route.value = _route.value + GeoPos(s.lat, s.lng)
                }
            }
        }
    }

    /** Begin/ensure recording. No-op once the ride is Finished. */
    fun start() {
        if (_phase.value == RidePhase.Finished) return
        timer.start()
        _phase.value = RidePhase.Recording
    }

    /** Freeze the ride. Only valid while Recording. */
    fun pause() {
        if (_phase.value != RidePhase.Recording) return
        timer.pause()
        _phase.value = RidePhase.Paused
    }

    /** Unfreeze the ride. Only valid while Paused. */
    fun resume() {
        if (_phase.value != RidePhase.Paused) return
        timer.start()
        _phase.value = RidePhase.Recording
    }

    /** End the ride. Terminal. */
    fun finish() {
        if (_phase.value == RidePhase.Finished) return
        timer.pause()
        _phase.value = RidePhase.Finished
    }

    fun close() {
        scope.cancel()
    }
}
