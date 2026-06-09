package com.compensatuviaje.tracker.feature.locationservice

import com.compensatuviaje.tracker.domain.LocationTracker
import com.compensatuviaje.tracker.model.GpsPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTrackerImpl : LocationTracker {

    private val _points = MutableSharedFlow<GpsPoint>()

    override val points: Flow<GpsPoint> =
        _points.asSharedFlow()

    private val _isTracking =
        MutableStateFlow(false)

    override val isTracking: Flow<Boolean> =
        _isTracking.asStateFlow()

    private var currentTripId: String? = null

    override fun start(tripId: String) {
        currentTripId = tripId
        _isTracking.value = true
    }

    override fun stop() {
        currentTripId = null
        _isTracking.value = false
    }
}