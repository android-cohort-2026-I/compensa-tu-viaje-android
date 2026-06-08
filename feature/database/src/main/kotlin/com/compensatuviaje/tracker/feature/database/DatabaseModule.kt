package com.compensatuviaje.tracker.feature.database

import android.content.Context
import androidx.room.Room

// TODO: Implementar AppDatabase (Room) con TripDao y GpsPointDao
// Implementa: TripRepository, GpsPointRepository de :core:domain
class DatabaseModule {

    // Variable estática para asegurar que solo exista una instancia de la base de datos (Singleton Manual)
    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null

        fun provideTripDatabase(context: Context): TripDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "compensa_tu_viaje_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        fun provideTripDao(context: Context): TripDao {
            return provideTripDatabase(context).tripDao()
        }

        fun provideGpsPointDao(context: Context): GpsPointDao {
            return provideTripDatabase(context).gpsPointDao()
        }
    }
}