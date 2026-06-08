package com.compensatuviaje.tracker.feature.trip

sealed class TripUiState {

    data class Idle(
        val isGpsAvailable: Boolean = false,
        val hasPermissions: Boolean = false,
    ) : TripUiState()

    data class Active(
        val tripId: String,
        val elapsedSeconds: Long,
        val localDistanceKm: Double,
        val isSyncing: Boolean,
        val isOnline: Boolean,
    ) : TripUiState()

    data object ConfirmingEnd : TripUiState()

    data object Processing : TripUiState()

    data class Summary(
        val serverDistanceKm: Double?,
        val localDistanceKm: Double,
        val durationSeconds: Long,
        val truckPlate: String,
    ) : TripUiState()

    data class Error(val message: String) : TripUiState()
}
