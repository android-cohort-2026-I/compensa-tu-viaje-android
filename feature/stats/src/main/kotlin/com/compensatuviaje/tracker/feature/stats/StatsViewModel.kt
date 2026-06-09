package com.compensatuviaje.tracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class StatsViewModel : ViewModel() {

    private val _uiState = mutableStateOf(StatsUiState())
    val uiState: State<StatsUiState> = _uiState

    fun loadTripStatistics() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Simulación directa sin hilos complejos
        _uiState.value = _uiState.value.copy(
            totalTrips = 14,
            totalDistanceKm = 342.5,
            totalDurationHours = 18.2,
            isLoading = false
        )
    }
}