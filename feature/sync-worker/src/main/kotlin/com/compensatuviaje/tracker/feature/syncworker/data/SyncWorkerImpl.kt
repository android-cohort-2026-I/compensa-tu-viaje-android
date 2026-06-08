package com.compensatuviaje.tracker.feature.syncworker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.compensatuviaje.tracker.feature.syncworker.SyncWorkerModule
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.flow.firstOrNull

class SyncWorkerImpl(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tripRepository = SyncWorkerModule.tripRepository ?: return Result.success()
        val connectivityMonitor = SyncWorkerModule.connectivityMonitor ?: return Result.success()

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

        return Result.success()
    }
}
