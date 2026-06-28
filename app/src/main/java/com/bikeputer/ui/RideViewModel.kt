package com.bikeputer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RidePhase
import com.bikeputer.domain.RideState
import com.bikeputer.ride.RideSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class RideViewModel(
    private val sessionFlow: StateFlow<RideSession?>,
    private val ownedSession: RideSession? = null,
) : ViewModel() {
    val state: StateFlow<RideState> =
        sessionFlow
            .flatMapLatest { it?.state ?: MutableStateFlow(RideState.EMPTY) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, RideState.EMPTY)

    val phase: StateFlow<RidePhase> =
        sessionFlow
            .flatMapLatest { it?.phase ?: MutableStateFlow(RidePhase.Recording) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, RidePhase.Recording)

    val route: StateFlow<List<GeoPos>> =
        sessionFlow
            .flatMapLatest { it?.route ?: MutableStateFlow(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun start() { sessionFlow.value?.start() }
    fun pause() { sessionFlow.value?.pause() }
    fun resume() { sessionFlow.value?.resume() }
    fun finish() { sessionFlow.value?.finish() }

    override fun onCleared() {
        ownedSession?.close()
    }
}
