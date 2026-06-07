package com.compensatuviaje.tracker.feature.locationservice

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.compensatuviaje.tracker.domain.LocationTracker
import com.compensatuviaje.tracker.model.GpsPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Implementación real de LocationTracker usando FusedLocationProvider.
 * Captura puntos GPS de alta precisión con WakeLock implícito via ForegroundService.
 */
class LocationTrackerImpl(
    private val context: Context,
    private val scope: CoroutineScope,
) : LocationTracker {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _points = MutableSharedFlow<GpsPoint>(extraBufferCapacity = 64)
    override val points: Flow<GpsPoint> = _points.asSharedFlow()

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: Flow<Boolean> = _isTracking.asStateFlow()

    private var currentTripId: String = ""

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                val punto = GpsPoint(
                    tripId = currentTripId,
                    timestampIso = Instant.now().toString(),
                    lat = location.latitude,
                    lng = location.longitude,
                    speedKmh = (location.speed * 3.6), // m/s → km/h
                    heading = location.bearing.toDouble(),
                    accuracyMeters = location.accuracy.toDouble(),
                    synced = false,
                )
                scope.launch { _points.emit(punto) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(tripId: String) {
        currentTripId = tripId
        _isTracking.value = true

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L // cada 5 segundos
        ).apply {
            setMinUpdateDistanceMeters(5f) // mínimo 5 metros de desplazamiento
            setWaitForAccurateLocation(false)
        }.build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun stop() {
        _isTracking.value = false
        fusedClient.removeLocationUpdates(locationCallback)
    }
}
