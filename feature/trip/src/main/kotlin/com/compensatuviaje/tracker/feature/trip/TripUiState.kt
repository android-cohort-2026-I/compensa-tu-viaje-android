package com.compensatuviaje.tracker.feature.trip

enum class TripStage {
    IDLE, IN_PROGRESS, CONFIRM_END, PROCESSING, SUMMARY
}

data class TripUiState(
    val stage: TripStage = TripStage.IDLE,
    val isGpsOk: Boolean = false,
    val permissionRevoked: Boolean = false,
    val elapsedFormatted: String = "00:00:00",
    val distanceKm: Double = 0.0,
    val isSyncing: Boolean = false,
    val serverDistanceKm: Double? = null,
    val durationFormatted: String = "00:00:00",
    val licensePlate: String = "",
    val co2Kg: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)