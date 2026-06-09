package com.compensatuviaje.tracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.flow.MutableStateFlow
import kotlinx.flow.StateFlow
import kotlinx.flow.asStateFlow
import kotlinx.flow.update

class StatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    fun loadTripStatistics() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            delay(1000)
            _uiState.update {
                it.copy(
                    totalTrips = 14,
                    totalDistanceKm = 342.5,
                    totalDurationHours = 18.2,
                    isLoading = false
                )
            }
        }
    }
}