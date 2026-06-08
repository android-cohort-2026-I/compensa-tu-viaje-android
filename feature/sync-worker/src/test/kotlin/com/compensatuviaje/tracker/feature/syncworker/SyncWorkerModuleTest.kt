package com.compensatuviaje.tracker.feature.syncworker

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.TripSummary
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import com.compensatuviaje.tracker.model.Truck
import com.compensatuviaje.tracker.testing.FakeTripRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FakeGpsPointRepository : GpsPointRepository {
    private val points = mutableListOf<GpsPoint>()
    override suspend fun insert(point: GpsPoint) { points.add(point) }
    override suspend fun unsynced(tripId: String): List<GpsPoint> =
        points.filter { it.tripId == tripId && !it.synced }
    override suspend fun markSynced(ids: List<Long>) {
        val updated = points.map { if (it.id in ids) it.copy(synced = true) else it }
        points.clear()
        points.addAll(updated)
    }
    override fun pointsForTrip(tripId: String): Flow<List<GpsPoint>> =
        flowOf(points.filter { it.tripId == tripId })
}

class FakeSyncApi(
    private val syncResult: AppResult<Int> = AppResult.Ok(0),
) : MobileApi {
    override suspend fun login(driverId: String, pin: String, licensePlate: String) =
        AppResult.Ok(Session("token", "Driver", Truck("1", "ABC", "Camion")))
    override suspend fun startTrip(startedAtIso: String, start: LatLng, accuracyMeters: Double) =
        AppResult.Ok("trip-001")
    override suspend fun syncBatch(tripId: String, currentLocalDistanceKm: Double, points: List<GpsPoint>) =
        syncResult
    override suspend fun endTrip(tripId: String, endedAtIso: String, end: LatLng, accuracyMeters: Double, totalLocalDistanceKm: Double): AppResult<TripSummary> =
        AppResult.Ok(TripSummary(100.0, 75.0))
}

class SyncWorkerModuleTest {

    private lateinit var tripRepo: FakeTripRepository
    private lateinit var gpsRepo: FakeGpsPointRepository

    private val sampleTrip = Trip(
        id = "trip-001",
        status = TripStatus.IN_PROGRESS,
        startedAtIso = "2026-06-08T10:00:00Z",
        totalLocalDistanceKm = 10.0,
    )

    private fun samplePoint(id: Long) = GpsPoint(
        id = id, tripId = "trip-001",
        timestampIso = "2026-06-08T10:0${id}:00Z",
        lat = -16.4 + id * 0.001, lng = -71.5 + id * 0.001,
        speedKmh = 40.0, heading = 90.0, accuracyMeters = 10.0, synced = false,
    )

    @Before
    fun setUp() {
        tripRepo = FakeTripRepository()
        gpsRepo = FakeGpsPointRepository()
    }

    @Test
    fun `sync marks points as synced on 200 OK`() = runTest {
        val api = FakeSyncApi(AppResult.Ok(2))
        tripRepo.create(sampleTrip)
        gpsRepo.insert(samplePoint(1))
        gpsRepo.insert(samplePoint(2))
        val unsynced = gpsRepo.unsynced("trip-001")
        val result = api.syncBatch("trip-001", 10.0, unsynced)
        if (result is AppResult.Ok) gpsRepo.markSynced(unsynced.map { it.id })
        assertThat(gpsRepo.unsynced("trip-001")).isEmpty()
    }

    @Test
    fun `sync does not mark points on error`() = runTest {
        val api = FakeSyncApi(AppResult.Err(ErrorKind.SERVER))
        tripRepo.create(sampleTrip)
        gpsRepo.insert(samplePoint(1))
        val unsynced = gpsRepo.unsynced("trip-001")
        val result = api.syncBatch("trip-001", 10.0, unsynced)
        if (result is AppResult.Ok) gpsRepo.markSynced(unsynced.map { it.id })
        assertThat(gpsRepo.unsynced("trip-001")).hasSize(1)
    }

    @Test
    fun `no points means nothing to sync`() = runTest {
        tripRepo.create(sampleTrip)
        assertThat(gpsRepo.unsynced("trip-001")).isEmpty()
    }

    @Test
    fun `batch sends all unsynced points together`() = runTest {
        val api = FakeSyncApi(AppResult.Ok(3))
        tripRepo.create(sampleTrip)
        gpsRepo.insert(samplePoint(1))
        gpsRepo.insert(samplePoint(2))
        gpsRepo.insert(samplePoint(3))
        val unsynced = gpsRepo.unsynced("trip-001")
        assertThat(unsynced).hasSize(3)
        val result = api.syncBatch("trip-001", 10.0, unsynced)
        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
    }
}
