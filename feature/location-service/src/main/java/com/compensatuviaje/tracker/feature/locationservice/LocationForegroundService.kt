package com.compensatuviaje.tracker.feature.locationservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.domain.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que captura puntos GPS durante el viaje.
 * Funciona con pantalla apagada y sobrevive el modo Doze.
 */
class LocationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "canal_viaje_en_curso"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACCION_INICIAR"
        const val ACTION_STOP = "ACCION_DETENER"
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val MAX_ACCURACY_METERS = 50.0
    }

    // Scope del servicio — se cancela al destruir
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Inyectados desde LocationTrackerImpl (implementación real)
    private val locationTracker: LocationTracker by lazy {
        LocationTrackerImpl(applicationContext, serviceScope)
    }
    private val gpsPointRepository: GpsPointRepository by lazy {
        // Obtenido desde el módulo :feature:database via contrato
        ServiceLocator.getGpsPointRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val tripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: return START_NOT_STICKY
                iniciarCaptura(tripId)
            }
            ACTION_STOP -> detenerCaptura()
        }
        return START_STICKY
    }

    private fun iniciarCaptura(tripId: String) {
        startForeground(
            NOTIFICATION_ID,
            construirNotificacion(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        locationTracker.start(tripId)

        serviceScope.launch {
            locationTracker.points.collect { punto ->
                // Solo guardar puntos con buena precisión GPS
                if (punto.accuracyMeters <= MAX_ACCURACY_METERS) {
                    gpsPointRepository.insert(punto)
                }
            }
        }
    }

    private fun detenerCaptura() {
        locationTracker.stop()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(
            CHANNEL_ID,
            "Viaje en curso",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación activa mientras el viaje está en progreso"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }

    private fun construirNotificacion(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Viaje en curso 🚛")
            .setContentText("Registrando ruta GPS en segundo plano")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
