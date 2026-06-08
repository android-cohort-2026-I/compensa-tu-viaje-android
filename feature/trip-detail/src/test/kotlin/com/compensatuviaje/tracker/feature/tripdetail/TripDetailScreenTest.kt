package com.compensatuviaje.tracker.feature.tripdetail

import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripDetailScreenTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeTripRepository = object : TripRepository {
        var stubbedTrip: Trip? = null
        override suspend fun get(tripId: String): Trip? = stubbedTrip

        override fun activeTrip(): Flow<Trip?> = flowOf(null)
        override fun completedTrips(): Flow<List<Trip>> = flowOf(emptyList())
        override suspend fun create(trip: Trip) {}
        override suspend fun update(trip: Trip) {}
        override suspend fun setStatus(tripId: String, status: TripStatus) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cuando el viaje existe en la BD local debe calcular duracion y pasar a estado Success`() = runTest {
        fakeTripRepository.stubbedTrip = Trip(
            id = "viaje_real_123",
            status = TripStatus.COMPLETED,
            startedAtIso = "2026-06-05T10:00:00Z",
            endedAtIso = "2026-06-05T10:45:00Z",
            totalLocalDistanceKm = 15.0
        )

        val viewModel = TripDetailViewModel(fakeTripRepository, "viaje_real_123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TripDetailUiState.Success)

        val successState = state as TripDetailUiState.Success
        assertEquals("viaje_real_123", successState.trip.id)
        assertEquals("45 min", successState.durationText)
    }

    @Test
    fun `cuando se busca un ID inexistente debe pasar a estado ErrorNotFound de manera controlada`() = runTest {
        fakeTripRepository.stubbedTrip = null

        val viewModel = TripDetailViewModel(fakeTripRepository, "id_fantasma_999")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TripDetailUiState.ErrorNotFound, viewModel.uiState.value)
    }
}