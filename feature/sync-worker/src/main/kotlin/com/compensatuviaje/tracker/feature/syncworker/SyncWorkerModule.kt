package com.compensatuviaje.tracker.feature.syncworker

import android.content.Context
import com.compensatuviaje.tracker.domain.*
import com.compensatuviaje.tracker.feature.syncworker.data.SyncManagerImpl

object SyncWorkerModule {
    var tripRepository: TripRepository? = null
    var gpsPointRepository: GpsPointRepository? = null
    var mobileApi: MobileApi? = null
    var connectivityMonitor: ConnectivityMonitor? = null
    var distanceCalculator: DistanceCalculator? = null

    fun provideSyncManager(context: Context, tripRepository: TripRepository): SyncManager {
        return SyncManagerImpl(context, tripRepository)
    }
}
