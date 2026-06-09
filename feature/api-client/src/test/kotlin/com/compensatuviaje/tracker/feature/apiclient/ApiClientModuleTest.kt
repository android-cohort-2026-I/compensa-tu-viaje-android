package com.compensatuviaje.tracker.feature.apiclient

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.testing.MockApiDispatcher
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test

class ApiClientModuleTest {

    // Servidor principal con MockApiDispatcher (respuestas del contrato)
    private lateinit var server: MockWebServer
    private lateinit var api: RetrofitMobileApi

    // Servidor de error: sin dispatcher custom, usa QueueDispatcher por defecto
    private lateinit var errorServer: MockWebServer
    private lateinit var errorApi: RetrofitMobileApi

    @Before
    fun setUp() {
        // Servidor happy-path
        server = MockWebServer()
        server.dispatcher = MockApiDispatcher()
        server.start()
        api = ApiClientModule(
            baseUrl = server.url("/").toString(),
            tokenProvider = { "eyJfaketoken" },
        ).mobileApi as RetrofitMobileApi

        // Servidor de errores (QueueDispatcher por defecto, acepta enqueue)
        errorServer = MockWebServer()
        errorServer.start()
        errorApi = ApiClientModule(
            baseUrl = errorServer.url("/").toString(),
            tokenProvider = { "eyJfaketoken" },
        ).mobileApi as RetrofitMobileApi
    }

    @After
    fun tearDown() {
        server.shutdown()
        errorServer.shutdown()
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun assertOk(result: AppResult<*>) =
        assertThat(result is AppResult.Ok).isTrue()

    private fun assertErr(result: AppResult<*>, expectedKind: ErrorKind) {
        assertThat(result is AppResult.Err).isTrue()
        assertThat((result as AppResult.Err).kind).isEqualTo(expectedKind)
    }

    // ─── login ─────────────────────────────────────────────────────────────

    @Test
    fun `login exitoso devuelve Session con token y datos del camion`() = runTest {
        val result = api.login("carlos01", "1234", "XYZ-987")

        assertOk(result)
        val session = (result as AppResult.Ok).value
        assertThat(session.token).isEqualTo("eyJfaketoken")
        assertThat(session.driverName).isEqualTo("Carlos Mendoza")
        assertThat(session.truck.licensePlate).isEqualTo("XYZ-987")
    }

    @Test
    fun `login con 401 devuelve Err UNAUTHORIZED`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(401))
        assertErr(errorApi.login("bad", "0000", "ZZZ-000"), ErrorKind.UNAUTHORIZED)
    }

    @Test
    fun `login con 422 devuelve Err VALIDATION`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(422))
        assertErr(errorApi.login("", "", ""), ErrorKind.VALIDATION)
    }

    @Test
    fun `login con 500 devuelve Err SERVER`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(500))
        assertErr(errorApi.login("carlos01", "1234", "XYZ-987"), ErrorKind.SERVER)
    }

    @Test
    fun `login se envia a la ruta correcta con metodo POST`() = runTest {
        api.login("carlos01", "1234", "XYZ-987")
        val recorded = server.takeRequest()
        assertThat(recorded.path).endsWith("/auth/login")
        assertThat(recorded.method).isEqualTo("POST")
    }

    // ─── startTrip ─────────────────────────────────────────────────────────

    @Test
    fun `startTrip exitoso devuelve trip_id`() = runTest {
        val result = api.startTrip(
            startedAtIso = "2026-06-01T14:30:00Z",
            start = LatLng(-12.0470, -77.0430),
            accuracyMeters = 4.1,
        )

        assertOk(result)
        assertThat((result as AppResult.Ok).value).isEqualTo("trip-uuid-888-999")
    }

    @Test
    fun `startTrip con 409 devuelve Err CONFLICT`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(409))
        assertErr(
            errorApi.startTrip("2026-06-01T14:30:00Z", LatLng(0.0, 0.0), 5.0),
            ErrorKind.CONFLICT,
        )
    }

    @Test
    fun `startTrip envia Authorization Bearer`() = runTest {
        api.startTrip("2026-06-01T14:30:00Z", LatLng(-12.0, -77.0), 4.0)
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer eyJfaketoken")
    }

    // ─── syncBatch ─────────────────────────────────────────────────────────

    @Test
    fun `syncBatch exitoso devuelve cantidad de puntos sincronizados`() = runTest {
        val result = api.syncBatch(
            tripId = "trip-uuid-888-999",
            currentLocalDistanceKm = 1.23,
            points = SampleData.sampleTrack,
        )

        assertOk(result)
        assertThat((result as AppResult.Ok).value).isAtLeast(0)
    }

    @Test
    fun `syncBatch con 200 no devuelve error`() = runTest {
        assertOk(api.syncBatch("trip-uuid-888-999", 0.5, SampleData.sampleTrack))
    }

    @Test
    fun `syncBatch con 500 devuelve Err SERVER`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(500))
        assertErr(
            errorApi.syncBatch("trip-uuid-888-999", 0.5, SampleData.sampleTrack),
            ErrorKind.SERVER,
        )
    }

    @Test
    fun `syncBatch envia todos los puntos en un solo request batch`() = runTest {
        api.syncBatch("trip-uuid-888-999", 1.23, SampleData.sampleTrack)

        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("/sync")
        assertThat(recorded.method).isEqualTo("POST")
        val body = recorded.body.readUtf8()
        assertThat(body).contains("points")
        assertThat(body).contains("current_local_distance_km")
    }

    @Test
    fun `syncBatch con 404 devuelve Err NOT_FOUND`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(404))
        assertErr(
            errorApi.syncBatch("trip-inexistente", 0.0, emptyList()),
            ErrorKind.NOT_FOUND,
        )
    }

    // ─── endTrip ──────────────────────────────────────────────────────────

    @Test
    fun `endTrip exitoso devuelve TripSummary con distancia del servidor y co2`() = runTest {
        val result = api.endTrip(
            tripId = "trip-uuid-888-999",
            endedAtIso = "2026-06-01T16:30:00Z",
            end = LatLng(-12.0500, -77.0430),
            accuracyMeters = 4.0,
            totalLocalDistanceKm = 145.0,
        )

        assertOk(result)
        val summary = (result as AppResult.Ok).value
        assertThat(summary.serverDistanceKm).isEqualTo(146.0)
        assertThat(summary.co2Kg).isEqualTo(112.5)
    }

    @Test
    fun `endTrip con 404 devuelve Err NOT_FOUND`() = runTest {
        errorServer.enqueue(MockResponse().setResponseCode(404))
        assertErr(
            errorApi.endTrip("trip-inexistente", "2026-06-01T16:30:00Z", LatLng(0.0, 0.0), 5.0, 0.0),
            ErrorKind.NOT_FOUND,
        )
    }

    @Test
    fun `endTrip con timeout devuelve Err NETWORK`() = runTest {
        errorServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertErr(
            errorApi.endTrip("trip-uuid-888-999", "2026-06-01T16:30:00Z", LatLng(0.0, 0.0), 5.0, 145.0),
            ErrorKind.NETWORK,
        )
    }

    // ─── ApiClientModule factory ───────────────────────────────────────────

    @Test
    fun `ApiClientModule crea instancia valida de MobileApi`() {
        val module = ApiClientModule(
            baseUrl = server.url("/").toString(),
            tokenProvider = { null },
        )
        assertThat(module.mobileApi).isNotNull()
    }
}
