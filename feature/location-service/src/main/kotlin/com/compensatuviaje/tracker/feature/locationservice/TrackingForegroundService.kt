package com.compensatuviaje.tracker.feature.locationservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.compensatuviaje.tracker.model.GpsPoint
import java.time.Instant
import android.util.Log
import android.os.Looper
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.os.PowerManager

class TrackingForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    private companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1001
    }
    private val capturedPoints = mutableListOf<GpsPoint>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {

        super.onCreate()

        val powerManager =
            getSystemService(
                POWER_SERVICE
            ) as PowerManager

        wakeLock =
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CompensaTuViaje:TrackingWakeLock"
            )

        wakeLock?.acquire()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            buildNotification()
        )

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            )
                .setMinUpdateIntervalMillis(3000L)
                .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(
                result: LocationResult
            ) {

                val location: Location =
                    result.lastLocation ?: return

                if (location.accuracy > 50f) {
                    return
                }

                val gpsPoint = GpsPoint(
                    tripId = "TEMP_TRIP",
                    timestampIso = Instant.now().toString(),
                    lat = location.latitude,
                    lng = location.longitude,
                    speedKmh = location.speed * 3.6,
                    heading = location.bearing.toDouble(),
                    accuracyMeters = location.accuracy.toDouble(),
                    synced = false
                )

                Log.d(
                    "TrackingService",
                    gpsPoint.toString()
                )
                capturedPoints.add(gpsPoint)
            }
        }

    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento GPS",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Compensa Tu Viaje")
            .setContentText("Viaje en curso 🚛")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    @android.annotation.SuppressLint("MissingPermission")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        try {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

        } catch (securityException: SecurityException) {

            Log.e(
                "TrackingService",
                "Permiso de ubicación no concedido",
                securityException
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        wakeLock?.let {

            if (it.isHeld) {
                it.release()
            }
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}