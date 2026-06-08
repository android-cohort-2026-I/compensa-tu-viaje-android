package com.compensatuviaje.tracker.feature.vehicle

import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck

sealed interface VehicleUiState {
    data object Loading : VehicleUiState

    data class Content(
        val session: Session,
        val truck: Truck,
        val hasActiveTrip: Boolean,
        val isConfirming: Boolean,
    ) : VehicleUiState

    data class Error(
        val message: String,
    ) : VehicleUiState
}
