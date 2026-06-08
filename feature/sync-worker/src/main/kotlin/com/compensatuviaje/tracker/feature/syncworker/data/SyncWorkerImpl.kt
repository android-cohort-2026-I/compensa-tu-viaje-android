package com.compensatuviaje.tracker.feature.syncworker.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.feature.syncworker.SyncWorkerModule
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.flow.firstOrNull

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
        val isOnline = connectivityMonitor.isOnline.firstOrNull() ?: false
        if (!isOnline) {
            return Result.success()
        }

        // 3. (Manejo de pending_end - Commit 6)

        // 4. Leer todos los GpsPoint con synced=false del viaje activo
        val unsyncedPoints = gpsPointRepository.unsynced(activeTrip.id)

        // 5. Si el lote está vacío, retornar success (nada que enviar)
        if (unsyncedPoints.isEmpty()) {
            return Result.success()
        }

        // 6. Calcular current_local_distance_km sumando distancias de todos los puntos en el viaje
        val allPoints = gpsPointRepository.pointsForTrip(activeTrip.id).firstOrNull() ?: emptyList()
        val currentLocalDistanceKm = distanceCalculator?.totalKm(allPoints) ?: 0.0

        // 7. Llamar MobileApi.syncBatch
        val syncResult = mobileApi.syncBatch(activeTrip.id, currentLocalDistanceKm, unsyncedPoints)

        when (syncResult) {
            is AppResult.Ok -> {
                // 8. En éxito: marcar cada punto del lote como synced=true
                gpsPointRepository.markSynced(unsyncedPoints.map { it.id })
            }
            is AppResult.Err -> {
                // 9. En error: loguear en Logcat, no marcar nada, retornar success
                Log.e("SyncWorker", "Error syncing batch: ${syncResult.message}")
            }
        }

        return Result.success()
    }
}
