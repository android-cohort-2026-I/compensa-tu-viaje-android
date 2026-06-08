package com.compensatuviaje.tracker.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.common.Iso8601
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ConnectivityMonitor
import com.compensatuviaje.tracker.domain.DistanceCalculator
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.LocationTracker
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.SyncManager
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class TripViewModel(
    private val tripRepository: TripRepository,
    private val locationTracker: LocationTracker,
    private val distanceCalculator: DistanceCalculator,
    private val mobileApi: MobileApi,
    private val syncManager: SyncManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripUiState>(TripUiState.Idle())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private var currentTripId: String? = null
    private var tripStartMillis: Long = 0L
    private val collectedPoints = mutableListOf<GpsPoint>()
    private var timerJob: Job? = null
    private var pointsJob: Job? = null

    init {
        observeConnectivity()
        observeLocationTracking()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                _uiState.update { current ->
                    if (current is TripUiState.Active) {
                        if (online) currentTripId?.let { syncManager.schedule(it) }
                        current.copy(isOnline = online)
                    } else current
                }
            }
        }
    }

    // Reflects GPS signal availability through tracker's active state.
    // Call updateGpsAvailable() to set availability from system callbacks.
    private fun observeLocationTracking() {
        viewModelScope.launch {
            locationTracker.isTracking.collect { tracking ->
                _uiState.update { current ->
                    if (current is TripUiState.Idle) current.copy(isGpsAvailable = tracking)
                    else current
                }
            }
        }
    }

    fun updateGpsAvailable(available: Boolean) {
        _uiState.update { current ->
            if (current is TripUiState.Idle) current.copy(isGpsAvailable = available)
            else current
        }
    }

    fun updatePermissions(granted: Boolean) {
        _uiState.update { current ->
            if (current is TripUiState.Idle) current.copy(hasPermissions = granted)
            else current
        }
    }

    fun startTrip() {
        val idle = _uiState.value as? TripUiState.Idle ?: return
        if (!idle.isGpsAvailable) return

        val tripId = UUID.randomUUID().toString()
        currentTripId = tripId
        tripStartMillis = System.currentTimeMillis()
        collectedPoints.clear()
        val startedAt = Iso8601.now()

        // Offline-first: transition to Active immediately without waiting for server
        _uiState.value = TripUiState.Active(
            tripId = tripId,
            elapsedSeconds = 0L,
            localDistanceKm = 0.0,
            isSyncing = false,
            isOnline = false,
        )

        locationTracker.start(tripId)
        startLiveTimer(tripId)
        collectPoints(tripId)

        viewModelScope.launch {
            tripRepository.create(Trip(id = tripId, status = TripStatus.IN_PROGRESS, startedAtIso = startedAt))
            val firstPoint = collectedPoints.firstOrNull()
            val start = firstPoint?.let { LatLng(it.lat, it.lng) } ?: LatLng(0.0, 0.0)
            val accuracy = firstPoint?.accuracyMeters ?: 0.0
            when (val result = mobileApi.startTrip(startedAt, start, accuracy)) {
                is AppResult.Err -> if (result.kind == ErrorKind.UNAUTHORIZED) handleUnauthorized()
                is AppResult.Ok -> Unit
            }
        }
    }

    private fun startLiveTimer(tripId: String) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { current ->
                    if (current is TripUiState.Active && current.tripId == tripId)
                        current.copy(elapsedSeconds = (System.currentTimeMillis() - tripStartMillis) / 1000L)
                    else current
                }
            }
        }
    }

    private fun collectPoints(tripId: String) {
        pointsJob?.cancel()
        pointsJob = viewModelScope.launch {
            locationTracker.points.collect { point ->
                collectedPoints.add(point)
                val distKm = distanceCalculator.totalKm(collectedPoints.toList())
                _uiState.update { current ->
                    if (current is TripUiState.Active && current.tripId == tripId)
                        current.copy(localDistanceKm = distKm)
                    else current
                }
            }
        }
    }

    fun requestEndTrip() {
        if (_uiState.value is TripUiState.Active) {
            _uiState.value = TripUiState.ConfirmingEnd
        }
    }

    fun cancelEnd() {
        val tripId = currentTripId ?: return
        _uiState.value = TripUiState.Active(
            tripId = tripId,
            elapsedSeconds = (System.currentTimeMillis() - tripStartMillis) / 1000L,
            localDistanceKm = distanceCalculator.totalKm(collectedPoints.toList()),
            isSyncing = false,
            isOnline = false,
        )
        startLiveTimer(tripId)
    }

    fun confirmEndTrip() {
        val tripId = currentTripId ?: return
        _uiState.value = TripUiState.Processing

        viewModelScope.launch {
            timerJob?.cancel()
            pointsJob?.cancel()
            locationTracker.stop()
            syncManager.cancel()

            val endedAt = Iso8601.now()
            val finalDistance = distanceCalculator.totalKm(collectedPoints.toList())
            val durationSecs = (System.currentTimeMillis() - tripStartMillis) / 1000L
            val lastPoint = collectedPoints.lastOrNull()
            val end = lastPoint?.let { LatLng(it.lat, it.lng) } ?: LatLng(0.0, 0.0)
            val accuracy = lastPoint?.accuracyMeters ?: 0.0
            val truckPlate = sessionRepository.current.firstOrNull()?.truck?.licensePlate ?: ""

            when (val result = mobileApi.endTrip(tripId, endedAt, end, accuracy, finalDistance)) {
                is AppResult.Ok -> {
                    tripRepository.update(
                        Trip(
                            id = tripId,
                            status = TripStatus.COMPLETED,
                            startedAtIso = Iso8601.of(tripStartMillis),
                            endedAtIso = endedAt,
                            totalLocalDistanceKm = finalDistance,
                            isSyncedToServer = true,
                            serverDistanceKm = result.value.serverDistanceKm,
                        )
                    )
                    _uiState.value = TripUiState.Summary(
                        serverDistanceKm = result.value.serverDistanceKm,
                        localDistanceKm = finalDistance,
                        durationSeconds = durationSecs,
                        truckPlate = truckPlate,
                    )
                }
                is AppResult.Err -> {
                    if (result.kind == ErrorKind.UNAUTHORIZED) {
                        handleUnauthorized()
                        return@launch
                    }
                    // Server unreachable: persist as PENDING_END so WorkManager retries later
                    tripRepository.update(
                        Trip(
                            id = tripId,
                            status = TripStatus.PENDING_END,
                            startedAtIso = Iso8601.of(tripStartMillis),
                            endedAtIso = endedAt,
                            totalLocalDistanceKm = finalDistance,
                            isSyncedToServer = false,
                        )
                    )
                    _uiState.value = TripUiState.Summary(
                        serverDistanceKm = null,
                        localDistanceKm = finalDistance,
                        durationSeconds = durationSecs,
                        truckPlate = truckPlate,
                    )
                }
            }
        }
    }

    private suspend fun handleUnauthorized() {
        sessionRepository.logout()
        _uiState.value = TripUiState.Error("Tu sesión expiró. Vuelve a iniciar sesión.")
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        pointsJob?.cancel()
    }
}
