package com.compensatuviaje.tracker.feature.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoints(points: List<GpsPointEntity>)

    @Query("SELECT * FROM gps_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getPointsForTripFlow(tripId: String): Flow<List<GpsPointEntity>>

    @Query("SELECT * FROM gps_points WHERE isSyncedToServer = 0 AND tripId = :tripId")
    suspend fun getUnsyncedPointsForTrip(tripId: String): List<GpsPointEntity>

    @Query("UPDATE gps_points SET isSyncedToServer = 1 WHERE id IN (:pointIds)")
    suspend fun markPointsAsSynced(pointIds: List<Long>)
}