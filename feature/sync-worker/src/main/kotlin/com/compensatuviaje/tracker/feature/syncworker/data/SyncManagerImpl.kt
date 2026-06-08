package com.compensatuviaje.tracker.feature.syncworker.data

import android.content.Context
import androidx.work.*
import com.compensatuviaje.tracker.domain.SyncManager
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SyncManagerImpl(
    private val context: Context,
    private val tripRepository: TripRepository
) : SyncManager {

    private val workManager = WorkManager.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun schedule(tripId: String) {
        enqueuePeriodicSync(tripId)
    }

    override fun cancel() {
        coroutineScope.launch {
            val activeTrip = tripRepository.activeTrip().firstOrNull()
            if (activeTrip != null && activeTrip.status == TripStatus.IN_PROGRESS) {
                tripRepository.setStatus(activeTrip.id, TripStatus.PENDING_END)
                enqueueImmediateSync(activeTrip.id)
            }
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }

    fun enqueuePeriodicSync(tripId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = workDataOf("trip_id" to tripId)

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorkerImpl>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun enqueueImmediateSync(tripId: String) {
        val data = workDataOf("trip_id" to tripId)

        val immediateRequest = OneTimeWorkRequestBuilder<SyncWorkerImpl>()
            .setInputData(data)
            .build()

        workManager.enqueue(immediateRequest)
    }

    companion object {
        const val PERIODIC_WORK_NAME = "SyncPeriodicWork"
    }
}
