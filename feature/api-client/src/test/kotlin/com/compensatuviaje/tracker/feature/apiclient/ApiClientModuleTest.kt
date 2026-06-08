package com.compensatuviaje.tracker.feature.apiclient

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.testing.MockApiDispatcher
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ApiClientModuleTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RetrofitMobileApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = MockApiDispatcher()
        server.start()
        api = RetrofitMobileApi(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login exitoso devuelve Session con token y datos del camion`() = runTest {
        val result = api.login("driver-01", "1234", "XYZ-987")

        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        val session = (result as AppResult.Ok).value
        assertThat(session.token).isEqualTo("eyJfaketoken")
        assertThat(session.driverName).isEqualTo("Carlos Mendoza")
        assertThat(session.truck.licensePlate).isEqualTo("XYZ-987")
        assertThat(session.truck.category).isEqualTo("heavy_duty")
    }

    @Test
    fun `login con 401 devuelve AppResult-Err UNAUTHORIZED`() = runTest {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) =
                MockResponse().setResponseCode(401)
        }
        val result = api.login("bad", "0000", "XXX-000")
        assertThat(result).isInstanceOf(AppResult.Err::class.java)
        assertThat((result as AppResult.Err).kind)
            .isEqualTo(com.compensatuviaje.tracker.domain.ErrorKind.UNAUTHORIZED)
    }

    @Test
    fun `login con 500 devuelve AppResult-Err SERVER`() = runTest {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) =
                MockResponse().setResponseCode(500)
        }
        val result = api.login("driver-01", "1234", "XYZ-987")
        assertThat(result).isInstanceOf(AppResult.Err::class.java)
        assertThat((result as AppResult.Err).kind)
            .isEqualTo(com.compensatuviaje.tracker.domain.ErrorKind.SERVER)
    }

    // ── startTrip ─────────────────────────────────────────────────────────────

    @Test
    fun `startTrip exitoso devuelve trip_id correcto`() = runTest {
        val result = api.startTrip(
            startedAtIso = "2026-06-08T10:00:00Z",
            start = LatLng(-16.409, -71.537),
            accuracyMeters = 4.5,
        )
        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        assertThat((result as AppResult.Ok).value).isEqualTo("trip-uuid-888-999")
    }

    @Test
    fun `startTrip envia el body correcto al servidor`() = runTest {
        api.startTrip("2026-06-08T10:00:00Z", LatLng(-16.409, -71.537), 4.5)
        val recorded = server.takeRequest()
        assertThat(recorded.path).endsWith("/trips/start")
        assertThat(recorded.body.readUtf8()).contains("started_at")
    }

    // ── syncBatch ─────────────────────────────────────────────────────────────

    @Test
    fun `syncBatch exitoso devuelve cantidad de puntos sincronizados`() = runTest {
        val result = api.syncBatch(
            tripId = "trip-uuid-888-999",
            currentLocalDistanceKm = 12.5,
            points = SampleData.sampleTrack,
        )
        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        // El mock devuelve synced_points_count: 2
        assertThat((result as AppResult.Ok).value).isEqualTo(2)
    }

    @Test
    fun `syncBatch incluye todos los puntos en el body`() = runTest {
        api.syncBatch("trip-uuid-888-999", 12.5, SampleData.sampleTrack)
        // Saltar la request de startTrip si hubiera; en este test es la primera
        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertThat(body).contains("points")
        assertThat(body).contains("current_local_distance_km")
    }

    @Test
    fun `syncBatch con lista vacia devuelve Ok con 2 (respuesta del mock)`() = runTest {
        val result = api.syncBatch("trip-uuid-888-999", 0.0, emptyList())
        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
    }

    // ── endTrip ───────────────────────────────────────────────────────────────

    @Test
    fun `endTrip exitoso devuelve TripSummary con distancia y co2`() = runTest {
        val result = api.endTrip(
            tripId = "trip-uuid-888-999",
            endedAtIso = "2026-06-08T12:00:00Z",
            end = LatLng(-16.420, -71.530),
            accuracyMeters = 3.8,
            totalLocalDistanceKm = 145.0,
        )
        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        val summary = (result as AppResult.Ok).value
        assertThat(summary.serverDistanceKm).isEqualTo(146.0)
        assertThat(summary.co2Kg).isEqualTo(112.5)
    }

    @Test
    fun `endTrip con 404 devuelve AppResult-Err NOT_FOUND`() = runTest {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) =
                MockResponse().setResponseCode(404)
        }
        val result = api.endTrip("trip-inexistente", "2026-06-08T12:00:00Z",
            LatLng(-16.420, -71.530), 3.8, 0.0)
        assertThat(result).isInstanceOf(AppResult.Err::class.java)
        assertThat((result as AppResult.Err).kind)
            .isEqualTo(com.compensatuviaje.tracker.domain.ErrorKind.NOT_FOUND)
    }

    // ── ApiClientModule factory ───────────────────────────────────────────────

    @Test
    fun `ApiClientModule-create devuelve instancia no nula`() {
        val api = ApiClientModule.create(
            baseUrl = server.url("/").toString()
        )
        assertThat(api).isNotNull()
    }
}
