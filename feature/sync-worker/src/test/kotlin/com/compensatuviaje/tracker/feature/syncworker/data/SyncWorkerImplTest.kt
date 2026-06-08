package com.compensatuviaje.tracker.feature.syncworker.data

import android.content.Context
import android.content.ContextWrapper
import androidx.work.*
import com.compensatuviaje.tracker.domain.*
import com.compensatuviaje.tracker.feature.syncworker.SyncWorkerModule
import com.compensatuviaje.tracker.model.*
import com.compensatuviaje.tracker.testing.FakeConnectivityMonitor
import com.compensatuviaje.tracker.testing.FakeTripRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.Executor

// Fake GPS Point Repository
class FakeGpsPointRepository : GpsPointRepository {
    val points = mutableListOf<GpsPoint>()

    override suspend fun insert(point: GpsPoint) {
        points.add(point)
    }

    override suspend fun unsynced(tripId: String): List<GpsPoint> {
        return points.filter { it.tripId == tripId && !it.synced }
    }

    override suspend fun markSynced(ids: List<Long>) {
        val updated = points.map { point ->
            if (point.id in ids) point.copy(synced = true) else point
        }
        points.clear()
        points.addAll(updated)
    }

    override fun pointsForTrip(tripId: String): Flow<List<GpsPoint>> {
        return flow {
            emit(points.filter { it.tripId == tripId })
        }
    }
}

// OkHttp implementation of MobileApi for testing against MockWebServer
class OkHttpMobileApi(private val baseUrl: String) : MobileApi {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun login(driverId: String, pin: String, licensePlate: String): AppResult<Session> {
        TODO("Not implemented")
    }

    override suspend fun startTrip(startedAtIso: String, start: LatLng, accuracyMeters: Double): AppResult<String> {
        TODO("Not implemented")
    }

    override suspend fun syncBatch(tripId: String, currentLocalDistanceKm: Double, points: List<GpsPoint>): AppResult<Int> {
        return try {
            val pointsJson = points.joinToString(",") {
                """{"id":${it.id},"timestamp_iso":"${it.timestampIso}","latitude":${it.lat},"longitude":${it.lng},"speed_kmh":${it.speedKmh},"heading":${it.heading},"accuracy_meters":${it.accuracyMeters}}"""
            }
            val json = """{"current_local_distance_km":$currentLocalDistanceKm,"points":[$pointsJson]}"""

            val request = Request.Builder()
                .url("${baseUrl}trips/$tripId/sync")
                .post(json.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains("\"success\":true")) {
                        val count = Regex(""""synced_points_count"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: points.size
                        AppResult.Ok(count)
                    } else {
                        AppResult.Err(ErrorKind.SERVER, "Sync failed: $body")
                    }
                } else {
                    AppResult.Err(ErrorKind.SERVER, "HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            AppResult.Err(ErrorKind.NETWORK, e.message)
        }
    }

    override suspend fun endTrip(tripId: String, endedAtIso: String, end: LatLng, accuracyMeters: Double, totalLocalDistanceKm: Double): AppResult<TripSummary> {
        return try {
            val json = """{"ended_at_iso":"$endedAtIso","end_latitude":${end.lat},"end_longitude":${end.lng},"accuracy_meters":$accuracyMeters,"total_local_distance_km":$totalLocalDistanceKm}"""

            val request = Request.Builder()
                .url("${baseUrl}trips/$tripId/end")
                .post(json.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val distance = Regex(""""server_calculated_distance_km"\s*:\s*([\d.]+)""").find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: totalLocalDistanceKm
                    val co2 = Regex(""""total_co2_kg"\s*:\s*([\d.]+)""").find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    AppResult.Ok(TripSummary(distance, co2))
                } else {
                    AppResult.Err(ErrorKind.SERVER, "HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            AppResult.Err(ErrorKind.NETWORK, e.message)
        }
    }
}

fun createDummyWorkerParams(): WorkerParameters {
    val executor = Executor { it.run() }
    val taskExecutorClass = Class.forName("androidx.work.impl.utils.taskexecutor.TaskExecutor")
    val taskExecutor = Proxy.newProxyInstance(
        taskExecutorClass.classLoader,
        arrayOf(taskExecutorClass)
    ) { _, method, _ ->
        if (method.name == "getSerialTaskExecutor" || method.name == "getMainThreadExecutor") {
            executor
        } else {
            null
        }
    }

    val progressUpdaterClass = Class.forName("androidx.work.ProgressUpdater")
    val progressUpdater = Proxy.newProxyInstance(
        progressUpdaterClass.classLoader,
        arrayOf(progressUpdaterClass)
    ) { _, _, _ -> null }

    val foregroundUpdaterClass = Class.forName("androidx.work.ForegroundUpdater")
    val foregroundUpdater = Proxy.newProxyInstance(
        foregroundUpdaterClass.classLoader,
        arrayOf(foregroundUpdaterClass)
    ) { _, _, _ -> null }

    val workerFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? = null
    }

    // Find the constructor that matches the signature with 12 parameters
    val constructor = WorkerParameters::class.java.declaredConstructors.firstOrNull { 
        it.parameterTypes.size == 12 
    } ?: WorkerParameters::class.java.declaredConstructors[0]
    constructor.isAccessible = true

    return constructor.newInstance(
        UUID.randomUUID(),
        Data.EMPTY,
        emptyList<String>(),
        WorkerParameters.RuntimeExtras(),
        1,
        0,
        executor,
        kotlin.coroutines.EmptyCoroutineContext,
        taskExecutor,
        workerFactory,
        progressUpdater,
        foregroundUpdater
    ) as WorkerParameters
}

class FakeDistanceCalc : DistanceCalculator {
    override fun totalKm(points: List<GpsPoint>) = points.size * 0.5
}

class SyncWorkerImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeTripRepository: FakeTripRepository
    private lateinit var fakeGpsPointRepository: FakeGpsPointRepository
    private lateinit var fakeConnectivityMonitor: FakeConnectivityMonitor
    private lateinit var mobileApi: MobileApi
    private lateinit var context: Context

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mobileApi = OkHttpMobileApi(mockWebServer.url("/").toString())

        fakeTripRepository = FakeTripRepository()
        fakeGpsPointRepository = FakeGpsPointRepository()
        fakeConnectivityMonitor = FakeConnectivityMonitor(initial = true)

        SyncWorkerModule.tripRepository = fakeTripRepository
        SyncWorkerModule.gpsPointRepository = fakeGpsPointRepository
        SyncWorkerModule.connectivityMonitor = fakeConnectivityMonitor
        SyncWorkerModule.mobileApi = mobileApi
        SyncWorkerModule.distanceCalculator = FakeDistanceCalc()

        context = ContextWrapper(null)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun printConstructors() {
        for (constructor in WorkerParameters::class.java.declaredConstructors) {
            println("CONSTRUCTOR_PARAMS: " + constructor.parameterTypes.joinToString { it.name })
        }
    }

    @Test
    fun `doWork returns success and does nothing if no active trip`() = runTest {
        val worker = SyncWorkerImpl(context, createDummyWorkerParams())
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(fakeGpsPointRepository.points).isEmpty()
    }

    @Test
    fun `doWork returns success and does nothing if offline`() = runTest {
        fakeTripRepository.create(Trip("trip-1", TripStatus.IN_PROGRESS, "2026-06-01T00:00:00Z"))
        fakeConnectivityMonitor.set(false)

        val worker = SyncWorkerImpl(context, createDummyWorkerParams())
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork with MockWebServer 200 marks batch as synced`() = runTest {
        fakeTripRepository.create(Trip("trip-1", TripStatus.IN_PROGRESS, "2026-06-01T00:00:00Z"))
        fakeGpsPointRepository.insert(GpsPoint(1, "trip-1", "2026-06-01T00:01:00Z", 10.0, 20.0, 50.0, 0.0, 10.0, false))
        fakeGpsPointRepository.insert(GpsPoint(2, "trip-1", "2026-06-01T00:02:00Z", 10.1, 20.1, 55.0, 0.0, 10.0, false))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success":true,"synced_points_count":2}""")
        )

        val worker = SyncWorkerImpl(context, createDummyWorkerParams())
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val unsynced = fakeGpsPointRepository.unsynced("trip-1")
        assertThat(unsynced).isEmpty()

        val allPoints = fakeGpsPointRepository.points
        assertThat(allPoints).hasSize(2)
        assertThat(allPoints.all { it.synced }).isTrue()
    }

    @Test
    fun `doWork with MockWebServer 500 does not mark batch as synced but returns success`() = runTest {
        fakeTripRepository.create(Trip("trip-1", TripStatus.IN_PROGRESS, "2026-06-01T00:00:00Z"))
        fakeGpsPointRepository.insert(GpsPoint(1, "trip-1", "2026-06-01T00:01:00Z", 10.0, 20.0, 50.0, 0.0, 10.0, false))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val worker = SyncWorkerImpl(context, createDummyWorkerParams())
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val unsynced = fakeGpsPointRepository.unsynced("trip-1")
        assertThat(unsynced).hasSize(1)
        assertThat(unsynced.first().synced).isFalse()
    }

    @Test
    fun `doWork with pending_end trip calls endTrip and transitions to completed`() = runTest {
        fakeTripRepository.create(Trip("trip-1", TripStatus.PENDING_END, "2026-06-01T00:00:00Z"))
        fakeGpsPointRepository.insert(GpsPoint(1, "trip-1", "2026-06-01T00:01:00Z", -12.0430, -77.0470, 50.0, 0.0, 10.0, true))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"server_calculated_distance_km":14.5,"total_co2_kg":12.3}""")
        )

        val worker = SyncWorkerImpl(context, createDummyWorkerParams())
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val request = mockWebServer.takeRequest()
        assertThat(request.path).endsWith("/end")

        val bodyText = request.body.readUtf8()
        assertThat(bodyText).contains(""""end_latitude":-12.043""")
        assertThat(bodyText).contains(""""end_longitude":-77.047""")

        val trip = fakeTripRepository.get("trip-1")
        assertThat(trip?.status).isEqualTo(TripStatus.COMPLETED)
    }

    @Test
    fun `doWork with pending_end trip retry keeps status as pending_end when api fails`() = runTest {
        fakeTripRepository.create(Trip("trip-1", TripStatus.PENDING_END, "2026-06-01T00:00:00Z"))
        fakeGpsPointRepository.insert(GpsPoint(1, "trip-1", "2026-06-01T00:01:00Z", -12.0430, -77.0470, 50.0, 0.0, 10.0, true))

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val worker = SyncWorkerImpl(context, createDummyWorkerParams())
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val request = mockWebServer.takeRequest()
        assertThat(request.path).endsWith("/end")

        val trip = fakeTripRepository.get("trip-1")
        assertThat(trip?.status).isEqualTo(TripStatus.PENDING_END)
    }
}
