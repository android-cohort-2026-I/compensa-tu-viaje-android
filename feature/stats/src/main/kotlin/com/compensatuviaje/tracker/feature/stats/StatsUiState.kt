package com.compensatuviaje.tracker.feature.stats

data class StatsUiState(
    val totalTrips: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationHours: Double = 0.0,
    val isLoading: Boolean = false
)