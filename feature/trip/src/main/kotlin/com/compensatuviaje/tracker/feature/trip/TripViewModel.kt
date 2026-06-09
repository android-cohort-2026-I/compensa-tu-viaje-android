package com.compensatuviaje.tracker.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.*
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripViewModel(
    private val tripRepository: TripRepository,
    private val locationTracker: LocationTracker,
    private val distanceCalculator: DistanceCalculator,
    private val syncManager: SyncManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val mobileApi: MobileApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private val currentTrack = mutableListOf<GpsPoint>()

    init {
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { isOnline ->
                _uiState.value = _uiState.value.copy(isSyncing = isOnline)
            }
        }

        viewModelScope.launch {
            locationTracker.points.collect { point ->
                currentTrack.add(point)
                val currentDistance = distanceCalculator.totalKm(currentTrack)
                _uiState.value = _uiState.value.copy(distanceKm = currentDistance)
            }
        }

        viewModelScope.launch {
            tripRepository.activeTrip().collect { trip ->
                if (trip != null && trip.status == TripStatus.IN_PROGRESS) {
                    _uiState.value = _uiState.value.copy(stage = TripStage.IN_PROGRESS)
                    locationTracker.start(trip.id)
                    syncManager.schedule(trip.id)
                } else {
                    _uiState.value = _uiState.value.copy(stage = TripStage.IDLE)
                }
            }
        }
    }

    fun onStartClick() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            currentTrack.clear()

            val tempTripId = "local_${System.currentTimeMillis()}"
            val newTrip = Trip(
                id = tempTripId,
                status = TripStatus.IN_PROGRESS,
                startedAtIso = "2026-01-01T00:00:00Z"
            )

            tripRepository.create(newTrip)
            locationTracker.start(tempTripId)
            syncManager.schedule(tempTripId)

            _uiState.value = _uiState.value.copy(isLoading = false, stage = TripStage.IN_PROGRESS)
        }
    }

    fun onFinishClick() {
        _uiState.value = _uiState.value.copy(stage = TripStage.CONFIRM_END)
    }

    fun onCancelEnd() {
        _uiState.value = _uiState.value.copy(stage = TripStage.IN_PROGRESS)
    }

    fun onConfirmEnd() {
        _uiState.value = _uiState.value.copy(stage = TripStage.PROCESSING)
        viewModelScope.launch {
            locationTracker.stop()
            syncManager.cancel()
            _uiState.value = _uiState.value.copy(
                stage = TripStage.SUMMARY,
                serverDistanceKm = _uiState.value.distanceKm
            )
        }
    }

    fun onSummaryDismiss() {
        currentTrack.clear()
        _uiState.value = TripUiState(stage = TripStage.IDLE)
    }
}

class TripViewModelFactory(
    private val tripRepository: TripRepository,
    private val locationTracker: LocationTracker,
    private val distanceCalculator: DistanceCalculator,
    private val syncManager: SyncManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val mobileApi: MobileApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TripViewModel(
            tripRepository, locationTracker, distanceCalculator,
            syncManager, connectivityMonitor, mobileApi
        ) as T
    }
}