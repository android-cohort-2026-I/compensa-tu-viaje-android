package com.compensatuviaje.tracker.feature.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gps_points",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["trip_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trip_id")]
)
data class GpsPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trip_id: String,
    val timestamp: String,
    val lat: Double,
    val lng: Double,
    val speed_kmh: Double,
    val heading: Double,
    val accuracy_meters: Double,
    val synced: Int = 0
)