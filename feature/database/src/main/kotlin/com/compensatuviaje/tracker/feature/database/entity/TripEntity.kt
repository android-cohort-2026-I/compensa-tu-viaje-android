package com.compensatuviaje.tracker.feature.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val status: String,
    val started_at: String,
    val ended_at: String? = null,
    val total_local_distance_km: Double = 0.0,
    val is_synced_to_server: Int = 0
)