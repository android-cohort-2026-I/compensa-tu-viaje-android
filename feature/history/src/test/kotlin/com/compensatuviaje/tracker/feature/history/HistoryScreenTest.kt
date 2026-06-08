package com.compensatuviaje.tracker.feature.history

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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryScreenTest {

    private val testDispatcher = StandardTestDispatcher()

    // Criterio obligatorio de prueba: ViewModel con FakeTripRepository (varios viajes)
    private val fakeTripRepository = object : TripRepository {
        var mockTrips = listOf<Trip>()
        override fun completedTrips(): Flow<List<Trip>> = flowOf(mockTrips)
        override fun activeTrip(): Flow<Trip?> = flowOf(null)
        override suspend fun create(trip: Trip) {}
        override suspend fun update(trip: Trip) {}
        override suspend fun setStatus(tripId: String, status: TripStatus) {}
        override suspend fun get(tripId: String): Trip? = null
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
    fun `verificar que el estado pasa a Success y los viajes se ordenan cronologicamente de forma descendente`() = runTest {
        // Varios viajes con fechas desordenadas
        fakeTripRepository.mockTrips = listOf(
            Trip(id = "viaje_antiguo", status = TripStatus.COMPLETED, startedAtIso = "2026-06-01T10:00:00Z"),
            Trip(id = "viaje_reciente", status = TripStatus.COMPLETED, startedAtIso = "2026-06-03T10:00:00Z"),
            Trip(id = "viaje_medio", status = TripStatus.COMPLETED, startedAtIso = "2026-06-02T10:00:00Z")
        )

        val viewModel = HistoryViewModel(fakeTripRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HistoryUiState.Success)

        val list = (state as HistoryUiState.Success).trips
        // El viaje más reciente debe ser el primero en la lista entregada a la UI
        assertEquals("viaje_reciente", list[0].id)
        assertEquals("viaje_medio", list[1].id)
        assertEquals("viaje_antiguo", list[2].id)
    }

    @Test
    fun `verificar que cuando no existen registros en la base de datos el estado sea Empty`() = runTest {
        fakeTripRepository.mockTrips = emptyList()

        val viewModel = HistoryViewModel(fakeTripRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(HistoryUiState.Empty, viewModel.uiState.value)
    }
}