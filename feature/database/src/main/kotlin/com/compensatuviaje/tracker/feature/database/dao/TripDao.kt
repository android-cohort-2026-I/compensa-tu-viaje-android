package com.compensatuviaje.tracker.feature.database.dao

import androidx.room.*
import com.compensatuviaje.tracker.feature.database.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE status = 'in_progress' LIMIT 1")
    fun getActiveTrip(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE status = 'in_progress' LIMIT 1")
    suspend fun getActiveTripOnce(): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE status = 'completed' ORDER BY started_at DESC")
    fun getCompletedTrips(): Flow<List<TripEntity>>

    @Query("UPDATE trips SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE trips SET id = :newId, is_synced_to_server = 1 WHERE id = :oldId")
    suspend fun reconcileId(oldId: String, newId: String)
}