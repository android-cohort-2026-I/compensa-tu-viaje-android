package com.compensatuviaje.tracker.feature.syncworker.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.feature.syncworker.SyncWorkerModule
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class SyncWorkerImpl(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tripRepository = SyncWorkerModule.tripRepository ?: return Result.success()
        val gpsPointRepository = SyncWorkerModule.gpsPointRepository ?: return Result.success()
        val connectivityMonitor = SyncWorkerModule.connectivityMonitor ?: return Result.success()
        val mobileApi = SyncWorkerModule.mobileApi ?: return Result.success()
        val distanceCalculator = SyncWorkerModule.distanceCalculator

        // 1. Verificar viaje activo (status = in_progress o pending_end)
        val activeTrip = tripRepository.activeTrip().firstOrNull()
        if (activeTrip == null || (activeTrip.status != TripStatus.IN_PROGRESS && activeTrip.status != TripStatus.PENDING_END)) {
            return Result.success()
        }

        // 2. Verificar conectividad
        val isOnline = withTimeoutOrNull(5000) {
            @Suppress("UNCHECKED_CAST")
            (connectivityMonitor.isOnline as Flow<Boolean?>)
                .filterNotNull()
                .firstOrNull()
        } ?: false

        if (!isOnline) {
            return Result.success()
        }

        // 4. Leer todos los GpsPoint con synced=false del viaje activo
        val unsyncedPoints = gpsPointRepository.unsynced(activeTrip.id)

        // 5. Obtener todos los puntos para calcular la distancia actual acumulada
        val allPoints = gpsPointRepository.pointsForTrip(activeTrip.id).firstOrNull() ?: emptyList()
        val currentLocalDistanceKm = distanceCalculator?.totalKm(allPoints) ?: 0.0

        var syncFailed = false
        if (unsyncedPoints.isNotEmpty()) {
            // 7. Llamar MobileApi.syncBatch
            val syncResult = mobileApi.syncBatch(activeTrip.id, currentLocalDistanceKm, unsyncedPoints)

            when (syncResult) {
                is AppResult.Ok -> {
                    // 8. En éxito: marcar cada punto del lote como synced=true
                    gpsPointRepository.markSynced(unsyncedPoints.map { it.id })
                }
                is AppResult.Err -> {
                    // 9. En error: loguear en Logcat, no marcar nada
                    Log.e("SyncWorker", "Error syncing batch: ${syncResult.message}")
                    syncFailed = true
                }
            }
        }

        // Si la sincronización de puntos falló, salimos para reintentar en el próximo ciclo
        if (syncFailed) {
            return Result.success()
        }

        // 3. Si el viaje está en pending_end: llamar a POST /end y actualizar estado en Room si 200
        if (activeTrip.status == TripStatus.PENDING_END) {
            val lastPoint = allPoints.lastOrNull()
            val endLatLng = lastPoint?.let { LatLng(it.lat, it.lng) } ?: LatLng(0.0, 0.0)
            val endAccuracy = lastPoint?.accuracyMeters ?: 0.0
            val endedAtIso = activeTrip.endedAtIso ?: lastPoint?.timestampIso ?: com.compensatuviaje.tracker.common.Iso8601.now()

            val endResult = mobileApi.endTrip(
                tripId = activeTrip.id,
                endedAtIso = endedAtIso,
                end = endLatLng,
                accuracyMeters = endAccuracy,
                totalLocalDistanceKm = currentLocalDistanceKm
            )

            when (endResult) {
                is AppResult.Ok -> {
                    tripRepository.setStatus(activeTrip.id, TripStatus.COMPLETED)
                }
                is AppResult.Err -> {
                    Log.e("SyncWorker", "Error ending trip: ${endResult.message}")
                }
            }
        }

        return Result.success()
    }
}
