package com.compensatuviaje.tracker.feature.vehicle

import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import com.compensatuviaje.tracker.model.Truck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VehicleScreenTest {

    private val testDispatcher = StandardTestDispatcher()

    // =======================================================
    // FAKES CONTRATADOS (FakeSession para pruebas mínimas)
    // =======================================================
    private val fakeSessionRepository = object : SessionRepository {
        var mockSession: Session? = Session(
            token = "jwt_token_ejemplo",
            driverName = "Angelo Mafer Anyelina",
            truck = Truck(id = "TRK-999", licensePlate = "ABC-123", category = "Remolcador")
        )
        override val current: Flow<Session?> get() = flowOf(mockSession)

        override suspend fun setSession(session: Session) {}
        override suspend fun logout() {}
    }

    private val fakeTripRepository = object : TripRepository {
        override fun activeTrip(): Flow<Trip?> = flowOf(null)
        override fun completedTrips(): Flow<List<Trip>> = flowOf(emptyList())
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

    // =======================================================
    // TESTS UNITARIOS OBLIGATORIOS
    // =======================================================
    @Test
    fun `cuando inicia con sesion valida el estado es Success`() = runTest {
        val viewModel = VehicleViewModel(fakeSessionRepository, fakeTripRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val expected = VehicleUiState.Success(plate = "ABC-123", category = "Remolcador")
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun `cuando inicia sin datos de sesion el estado cambia a EmptyData`() = runTest {
        fakeSessionRepository.mockSession = null
        val viewModel = VehicleViewModel(fakeSessionRepository, fakeTripRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(VehicleUiState.EmptyData, viewModel.uiState.value)
    }
}