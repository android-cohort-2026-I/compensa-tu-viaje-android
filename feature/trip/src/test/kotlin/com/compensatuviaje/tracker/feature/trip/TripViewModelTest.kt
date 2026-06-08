package com.compensatuviaje.tracker.feature.trip

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ConnectivityMonitor
import com.compensatuviaje.tracker.domain.DistanceCalculator
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.SyncManager
import com.compensatuviaje.tracker.domain.TripSummary
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.TripStatus
import com.compensatuviaje.tracker.testing.FakeConnectivityMonitor
import com.compensatuviaje.tracker.testing.FakeLocationTracker
import com.compensatuviaje.tracker.testing.FakeMobileApi
import com.compensatuviaje.tracker.testing.FakeTripRepository
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeVm(
        tripRepo: FakeTripRepository = FakeTripRepository(),
        tracker: FakeLocationTracker = FakeLocationTracker(),
        distance: DistanceCalculator = FakeDistanceCalculator(),
        api: MobileApi = FakeMobileApi(),
        sync: SyncManager = FakeSyncManager(),
        connectivity: ConnectivityMonitor = FakeConnectivityMonitor(initial = true),
        session: SessionRepository = FakeSessionRepository(),
    ) = TripViewModel(tripRepo, tracker, distance, api, sync, connectivity, session)

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `startTrip transitions to Active immediately without waiting for server`() = runTest {
        // SlowApi never responds during the coroutine test scope;
        // the Active state must appear synchronously before it resolves.
        val slowApi = object : MobileApi {
            override suspend fun login(driverId: String, pin: String, licensePlate: String) =
                AppResult.Ok(SampleData.session)
            override suspend fun startTrip(startedAtIso: String, start: LatLng, accuracyMeters: Double): AppResult<String> {
                kotlinx.coroutines.delay(Long.MAX_VALUE / 2)
                return AppResult.Ok("server-id")
            }
            override suspend fun syncBatch(tripId: String, currentLocalDistanceKm: Double, points: List<GpsPoint>) =
                AppResult.Ok(0)
            override suspend fun endTrip(tripId: String, endedAtIso: String, end: LatLng, accuracyMeters: Double, totalLocalDistanceKm: Double): AppResult<TripSummary> =
                AppResult.Ok(TripSummary(0.0, 0.0))
        }

        val vm = makeVm(api = slowApi)
        vm.updateGpsAvailable(true)

        vm.startTrip()

        // Assert immediately — do NOT advance the dispatcher
        assertThat(vm.uiState.value).isInstanceOf(TripUiState.Active::class.java)
    }

    @Test
    fun `confirmEndTrip with successful API transitions to Summary with correct serverDistanceKm`() = runTest {
        val vm = makeVm()
        vm.updateGpsAvailable(true)
        vm.startTrip()
        advanceUntilIdle()

        vm.requestEndTrip()
        vm.confirmEndTrip()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(TripUiState.Summary::class.java)
        // FakeMobileApi.endTrip returns TripSummary(serverDistanceKm = 146.0, ...)
        assertThat((state as TripUiState.Summary).serverDistanceKm).isEqualTo(146.0)
    }

    @Test
    fun `confirmEndTrip with failing API leaves trip as PENDING_END in repository`() = runTest {
        val failingApi = object : MobileApi {
            override suspend fun login(driverId: String, pin: String, licensePlate: String) =
                AppResult.Ok(SampleData.session)
            override suspend fun startTrip(startedAtIso: String, start: LatLng, accuracyMeters: Double): AppResult<String> =
                AppResult.Ok("trip-id")
            override suspend fun syncBatch(tripId: String, currentLocalDistanceKm: Double, points: List<GpsPoint>) =
                AppResult.Ok(0)
            override suspend fun endTrip(tripId: String, endedAtIso: String, end: LatLng, accuracyMeters: Double, totalLocalDistanceKm: Double): AppResult<TripSummary> =
                AppResult.Err(ErrorKind.NETWORK)
        }
        val tripRepo = FakeTripRepository()
        val vm = makeVm(tripRepo = tripRepo, api = failingApi)

        vm.updateGpsAvailable(true)
        vm.startTrip()
        advanceUntilIdle()

        vm.requestEndTrip()
        vm.confirmEndTrip()
        advanceUntilIdle()

        val trip = tripRepo.activeTrip().first()
        assertThat(trip?.status).isEqualTo(TripStatus.PENDING_END)

        // UI shows a provisional summary rather than an error screen
        assertThat(vm.uiState.value).isInstanceOf(TripUiState.Summary::class.java)
        assertThat((vm.uiState.value as TripUiState.Summary).serverDistanceKm).isNull()
    }

    @Test
    fun `Active state updates isOnline to false when ConnectivityMonitor emits offline`() = runTest {
        val connectivity = FakeConnectivityMonitor(initial = true)
        val vm = makeVm(connectivity = connectivity)

        vm.updateGpsAvailable(true)
        vm.startTrip()
        advanceUntilIdle()

        assertThat((vm.uiState.value as TripUiState.Active).isOnline).isTrue()

        connectivity.set(false)
        advanceUntilIdle()

        assertThat((vm.uiState.value as TripUiState.Active).isOnline).isFalse()
    }

    @Test
    fun `startTrip with 401 response transitions to Error state with Spanish message without HTTP code`() = runTest {
        val unauthorizedApi = object : MobileApi {
            override suspend fun login(driverId: String, pin: String, licensePlate: String) =
                AppResult.Ok(SampleData.session)
            override suspend fun startTrip(startedAtIso: String, start: LatLng, accuracyMeters: Double): AppResult<String> =
                AppResult.Err(ErrorKind.UNAUTHORIZED)
            override suspend fun syncBatch(tripId: String, currentLocalDistanceKm: Double, points: List<GpsPoint>) =
                AppResult.Ok(0)
            override suspend fun endTrip(tripId: String, endedAtIso: String, end: LatLng, accuracyMeters: Double, totalLocalDistanceKm: Double): AppResult<TripSummary> =
                AppResult.Ok(TripSummary(0.0, 0.0))
        }

        val vm = makeVm(api = unauthorizedApi)
        vm.updateGpsAvailable(true)
        vm.startTrip()         // state = Active immediately (offline-first)
        advanceUntilIdle()     // server call runs → UNAUTHORIZED → Error

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(TripUiState.Error::class.java)
        val error = state as TripUiState.Error
        assertThat(error.message).doesNotContain("401")
        assertThat(error.message).doesNotContain("HTTP")
        assertThat(error.message).contains("sesión")
    }
}

// ── Local fakes for dependencies absent from core:testing ────────────────────

private class FakeDistanceCalculator(private val result: Double = 0.0) : DistanceCalculator {
    override fun totalKm(points: List<GpsPoint>): Double = result
}

private class FakeSyncManager : SyncManager {
    val scheduled = mutableListOf<String>()
    var cancelCalled = false
    override fun schedule(tripId: String) { scheduled += tripId }
    override fun cancel() { cancelCalled = true }
}

private class FakeSessionRepository(
    private val session: Session? = SampleData.session,
) : SessionRepository {
    private val _current = MutableStateFlow(session)
    override val current: Flow<Session?> = _current.asStateFlow()
    override suspend fun setSession(session: Session) { _current.value = session }
    override suspend fun logout() { _current.value = null }
}
