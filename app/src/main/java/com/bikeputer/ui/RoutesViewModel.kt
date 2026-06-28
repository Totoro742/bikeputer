package com.bikeputer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bikeputer.data.RouteStore
import com.bikeputer.data.SettingsRepository
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.SavedRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/** Drives the Routes library: import, arm/clear, delete, and route preview. */
class RoutesViewModel(
    private val store: RouteStore,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _routes = MutableStateFlow<List<SavedRoute>>(emptyList())
    val routes: StateFlow<List<SavedRoute>> = _routes

    val activeRouteId: StateFlow<String?> =
        settings.settings.map { it.activeRouteId }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            _routes.value = withContext(Dispatchers.IO) { store.list() }
        }
    }

    fun import(input: InputStream, fallbackName: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { store.import(fallbackName, input) } }
            if (result.isFailure) _error.value = "Couldn't read that GPX file" else refresh()
        }
    }

    fun arm(id: String) {
        viewModelScope.launch { settings.update { it.copy(activeRouteId = id) } }
    }

    fun clear() {
        viewModelScope.launch { settings.update { it.copy(activeRouteId = null) } }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(id) }
            if (activeRouteId.value == id) settings.update { it.copy(activeRouteId = null) }
            refresh()
        }
    }

    suspend fun loadPoints(id: String): List<GeoPos> =
        withContext(Dispatchers.IO) { store.loadPoints(id) }

    fun clearError() { _error.value = null }
}
