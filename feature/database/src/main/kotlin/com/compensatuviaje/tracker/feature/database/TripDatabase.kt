package com.compensatuviaje.tracker.feature.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, GpsPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun gpsPointDao(): GpsPointDao
}