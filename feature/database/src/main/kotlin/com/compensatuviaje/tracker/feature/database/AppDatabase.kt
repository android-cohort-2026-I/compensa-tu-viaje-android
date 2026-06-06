package com.compensatuviaje.tracker.feature.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.compensatuviaje.tracker.feature.database.dao.GpsPointDao
import com.compensatuviaje.tracker.feature.database.dao.TripDao
import com.compensatuviaje.tracker.feature.database.entity.GpsPointEntity
import com.compensatuviaje.tracker.feature.database.entity.TripEntity

@Database(
    entities = [TripEntity::class, GpsPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun gpsPointDao(): GpsPointDao
}