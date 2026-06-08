package com.compensatuviaje.tracker.feature.database

import android.content.Context
import androidx.room.Room
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class TripDatabaseTest {

    private lateinit var db: TripDatabase

    @Before
    fun createDb() {
        // Creamos un falso Context usando el Proxy nativo de Java/Kotlin, sin Mockito ni Gradle
        val fakeContext = Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java)
        ) { _, _, _ -> null } as Context

        // Creamos la base de datos en memoria
        db = Room.inMemoryDatabaseBuilder(fakeContext, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testTripEntityCreation() {
        // Validamos de forma nativa que las entidades de tu base de datos se estructuren correctamente
        val trip = TripEntity(
            id = "viaje_001",
            driverId = "conductor_abc",
            vehicleId = "vehiculo_xyz",
            startTime = 1717858800000L,
            endTime = null,
            isSynced = false
        )

        assertNotNull(trip)
        assertEquals("viaje_001", trip.id)
        assertEquals("conductor_abc", trip.driverId)
        assertEquals("vehiculo_xyz", trip.vehicleId)
    }
}