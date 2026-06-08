package com.compensatuviaje.tracker.feature.locationservice

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Controlador público del módulo location-service.
 * Otros módulos (feature:trip) usan esta clase para iniciar/detener el GPS.
 */
class LocationServiceController(private val context: Context) {

    fun iniciar(tripId: String) {
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_START
            putExtra(LocationForegroundService.EXTRA_TRIP_ID, tripId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun detener() {
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
