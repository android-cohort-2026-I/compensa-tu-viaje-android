package com.compensatuviaje.tracker.feature.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TripRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val MissingVehicleMessage =
    "No se encontro informacion del vehiculo. Cerra sesion e intenta de nuevo."

sealed interface VehicleEvent {
    data object Continue : VehicleEvent
    data object CloseSession : VehicleEvent
}

class VehicleViewModel(
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VehicleUiState>(VehicleUiState.Loading)
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VehicleEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<VehicleEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                sessionRepository.current,
                tripRepository.activeTrip(),
            ) { session, activeTrip ->
                val truck = session?.truck
                if (session == null || truck == null) {
                    VehicleUiState.Error(MissingVehicleMessage)
                } else {
                    val current = _uiState.value
                    val isConfirming = (current as? VehicleUiState.Content)?.isConfirming ?: false
                    VehicleUiState.Content(
                        session = session,
                        truck = truck,
                        hasActiveTrip = activeTrip != null,
                        isConfirming = isConfirming,
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onConfirm() {
        val content = _uiState.value as? VehicleUiState.Content ?: return
        if (content.isConfirming) return

        _uiState.value = content.copy(isConfirming = true)
        _events.tryEmit(VehicleEvent.Continue)
    }

    fun onCloseSession() {
        _events.tryEmit(VehicleEvent.CloseSession)
    }
}

class VehicleViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VehicleViewModel(sessionRepository, tripRepository) as T
    }
}
