package com.compensatuviaje.tracker.feature.database.dao

import androidx.room.*
import com.compensatuviaje.tracker.feature.database.entity.GpsPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsPointDao {
    @Insert
    suspend fun insert(point: GpsPointEntity)

    @Query("SELECT * FROM gps_points WHERE trip_id = :tripId AND synced = 0")
    suspend fun getUnsyncedPoints(tripId: String): List<GpsPointEntity>

    @Query("UPDATE gps_points SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("SELECT * FROM gps_points WHERE trip_id = :tripId ORDER BY timestamp ASC")
    fun getPointsByTrip(tripId: String): Flow<List<GpsPointEntity>>

    @Query("DELETE FROM gps_points WHERE synced = 1 AND trip_id = :tripId")
    suspend fun deleteSyncedPoints(tripId: String)
}