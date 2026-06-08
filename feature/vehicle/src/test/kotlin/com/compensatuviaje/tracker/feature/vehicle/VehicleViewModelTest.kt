package com.compensatuviaje.tracker.feature.vehicle

import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.testing.FakeTripRepository
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class FakeSessionRepository(
    initialSession: Session? = null,
) : SessionRepository {
    private val session = MutableStateFlow(initialSession)
    override val current: Flow<Session?> = session

    override suspend fun setSession(session: Session) {
        this.session.value = session
    }

    override suspend fun logout() {
        session.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sesion con truck valido emite Content con patente y categoria`() = runTest {
        val viewModel = VehicleViewModel(
            sessionRepository = FakeSessionRepository(SampleData.session),
            tripRepository = FakeTripRepository(),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as VehicleUiState.Content
        assertThat(state.truck.licensePlate).isEqualTo(SampleData.truck.licensePlate)
        assertThat(state.truck.category).isEqualTo(SampleData.truck.category)
        assertThat(state.hasActiveTrip).isFalse()
    }

    @Test
    fun `sesion null emite Error amigable`() = runTest {
        val viewModel = VehicleViewModel(
            sessionRepository = FakeSessionRepository(null),
            tripRepository = FakeTripRepository(),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as VehicleUiState.Error
        assertThat(state.message).contains("No se encontro informacion del vehiculo")
    }

    @Test
    fun `onConfirm evita doble confirmacion rapida`() = runTest {
        val viewModel = VehicleViewModel(
            sessionRepository = FakeSessionRepository(SampleData.session),
            tripRepository = FakeTripRepository(),
        )
        advanceUntilIdle()

        val events = mutableListOf<VehicleEvent>()
        val eventJob = launch {
            viewModel.events.collect { events.add(it) }
        }
        advanceUntilIdle()

        viewModel.onConfirm()
        viewModel.onConfirm()
        advanceUntilIdle()

        val state = viewModel.uiState.value as VehicleUiState.Content
        assertThat(state.isConfirming).isTrue()
        assertThat(events).containsExactly(VehicleEvent.Continue)
        eventJob.cancel()
    }

    @Test
    fun `onConfirm no crashea sin viaje activo`() = runTest {
        val viewModel = VehicleViewModel(
            sessionRepository = FakeSessionRepository(SampleData.session),
            tripRepository = FakeTripRepository(),
        )
        advanceUntilIdle()

        viewModel.onConfirm()

        val state = viewModel.uiState.value as VehicleUiState.Content
        assertThat(state.hasActiveTrip).isFalse()
        assertThat(state.isConfirming).isTrue()
    }
}
