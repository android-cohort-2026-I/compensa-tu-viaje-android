package com.compensatuviaje.tracker.feature.syncworker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.SyncManager
import com.compensatuviaje.tracker.domain.TripRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorkerModule

// ── Worker ────────────────────────────────────────────────────────────────────

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val tripRepository: TripRepository,
    private val gpsPointRepository: GpsPointRepository,
    private val mobileApi: MobileApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val activeTrip = tripRepository.activeTrip().first()
            ?: return Result.success()

        if (activeTrip.id.isBlank()) return Result.success()

        val unsyncedPoints = gpsPointRepository.unsynced(activeTrip.id)
        if (unsyncedPoints.isEmpty()) return Result.success()

        return when (mobileApi.syncBatch(
            tripId = activeTrip.id,
            currentLocalDistanceKm = activeTrip.totalLocalDistanceKm,
            points = unsyncedPoints,
        )) {
            is AppResult.Ok -> {
                gpsPointRepository.markSynced(unsyncedPoints.map { it.id })
                Result.success()
            }
            is AppResult.Err -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync_worker"
    }
}

// ── SyncManager ───────────────────────────────────────────────────────────────

class WorkManagerSyncManager(private val context: Context) : SyncManager {

    override fun schedule(tripId: String) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(5, TimeUnit.MINUTES)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
