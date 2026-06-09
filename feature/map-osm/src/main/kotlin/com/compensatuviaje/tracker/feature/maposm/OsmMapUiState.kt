package com.compensatuviaje.tracker.feature.maposm
import com.compensatuviaje.tracker.model.GpsPoint

sealed interface OsmMapUiState {
    data object Loading : OsmMapUiState
    data object Empty : OsmMapUiState
    data class Success(
        val points: List<GpsPoint>,
        val startPoint: GpsPoint,
        val endPoint: GpsPoint
    ) : OsmMapUiState
    data class Error(val message: String) : OsmMapUiState
}
