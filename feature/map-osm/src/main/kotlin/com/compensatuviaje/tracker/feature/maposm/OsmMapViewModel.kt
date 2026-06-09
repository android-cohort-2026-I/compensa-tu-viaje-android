package com.compensatuviaje.tracker.feature.maposm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.GpsPointRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OsmMapViewModel(
    private val repository: GpsPointRepository
) : ViewModel() {

    fun loadPoints(tripId: String) = repository
        .pointsForTrip(tripId)
        .map { points ->
            when {
                points.isEmpty() -> OsmMapUiState.Empty
                else -> OsmMapUiState.Success(
                    points = points,
                    startPoint = points.first(),
                    endPoint = points.last()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OsmMapUiState.Loading
        )
}