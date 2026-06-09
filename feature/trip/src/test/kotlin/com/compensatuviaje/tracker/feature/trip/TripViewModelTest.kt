package com.compensatuviaje.tracker.feature.trip

import com.compensatuviaje.tracker.domain.*
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.testing.FakeConnectivityMonitor
import com.compensatuviaje.tracker.testing.FakeLocationTracker
import com.compensatuviaje.tracker.testing.FakeMobileApi
import com.compensatuviaje.tracker.testing.FakeTripRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class FakeSyncManager : SyncManager {
    override fun schedule(tripId: String) {}
    override fun cancel() {}
}

class FakeDistanceCalc : DistanceCalculator {
    override fun totalKm(points: List<GpsPoint>) = points.size * 0.5
}

class TripViewModelTest {

    private lateinit var viewModel: TripViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = TripViewModel(
            FakeTripRepository(), FakeLocationTracker(), FakeDistanceCalc(),
            FakeSyncManager(), FakeConnectivityMonitor(), FakeMobileApi()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `iniciar viaje cambia a IN_PROGRESS`() = runTest {
        assertThat(viewModel.uiState.value.stage).isEqualTo(TripStage.IDLE)
        viewModel.onStartClick()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.stage).isEqualTo(TripStage.IN_PROGRESS)
    }

    @Test
    fun `finalizar pide confirmacion y procesa`() = runTest {
        viewModel.onStartClick()
        advanceUntilIdle()
        viewModel.onFinishClick()
        assertThat(viewModel.uiState.value.stage).isEqualTo(TripStage.CONFIRM_END)
        viewModel.onCancelEnd()
        assertThat(viewModel.uiState.value.stage).isEqualTo(TripStage.IN_PROGRESS)
        viewModel.onFinishClick()
        viewModel.onConfirmEnd()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.stage).isEqualTo(TripStage.SUMMARY)
    }
}