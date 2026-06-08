package com.compensatuviaje.tracker.feature.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val driverId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val isSynced: Boolean = false
)