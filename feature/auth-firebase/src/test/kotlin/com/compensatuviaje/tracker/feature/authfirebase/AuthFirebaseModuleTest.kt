package com.compensatuviaje.tracker.feature.authfirebase

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.testing.FakeMobileApi
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthFirebaseModuleTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildApi(
        authSucceeds: Boolean = true,
    ): FirebaseMobileApi {
        val authProvider = FakeFirebaseAuthProvider(shouldSucceed = authSucceeds)
        val delegate = FakeMobileApi()
        return FirebaseMobileApi(authProvider, delegate)
    }

    // ── login exitoso ─────────────────────────────────────────────────────────

    @Test
    fun `login exitoso devuelve Session con token de Firebase`() = runTest {
        val api = buildApi(authSucceeds = true)

        val result = api.login("driver-01", "1234", "XYZ-987")

        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        val session = (result as AppResult.Ok).value
        assertThat(session.token).isEqualTo("eyJfakeFirebaseToken")
        assertThat(session.truck.licensePlate).isEqualTo("XYZ-987")
    }

    @Test
    fun `login exitoso incluye el UID de Firebase como truck id`() = runTest {
        val api = buildApi(authSucceeds = true)

        val result = api.login("driver-01", "1234", "XYZ-987")

        val session = (result as AppResult.Ok).value
        assertThat(session.truck.id).isEqualTo("fake-uid-123")
    }

    @Test
    fun `login deriva email correcto a partir del driverId`() = runTest {
        val fakeAuth = FakeFirebaseAuthProvider(shouldSucceed = true)
        val api = FirebaseMobileApi(fakeAuth, FakeMobileApi())

        api.login("driver-42", "5678", "ABC-123")

        assertThat(fakeAuth.lastEmail).isEqualTo("driver-42@compensatuviaje.com")
        assertThat(fakeAuth.lastPassword).isEqualTo("5678")
    }

    // ── login fallido ─────────────────────────────────────────────────────────

    @Test
    fun `login fallido devuelve AppResult-Err UNAUTHORIZED`() = runTest {
        val api = buildApi(authSucceeds = false)

        val result = api.login("bad-driver", "wrong", "XXX-000")

        assertThat(result).isInstanceOf(AppResult.Err::class.java)
        assertThat((result as AppResult.Err).kind).isEqualTo(ErrorKind.UNAUTHORIZED)
    }

    @Test
    fun `login llama exactamente una vez a signInWithEmailAndPassword`() = runTest {
        val fakeAuth = FakeFirebaseAuthProvider(shouldSucceed = true)
        val api = FirebaseMobileApi(fakeAuth, FakeMobileApi())

        api.login("driver-01", "1234", "XYZ-987")

        assertThat(fakeAuth.signInCallCount).isEqualTo(1)
    }

    // ── delegación ────────────────────────────────────────────────────────────

    @Test
    fun `startTrip delega al api REST y devuelve trip_id`() = runTest {
        val api = buildApi()

        val result = api.startTrip("2026-06-08T10:00:00Z", LatLng(-16.409, -71.537), 4.5)

        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        assertThat((result as AppResult.Ok).value).isEqualTo("trip-uuid-888-999")
    }

    @Test
    fun `syncBatch delega al api REST y devuelve cantidad de puntos`() = runTest {
        val api = buildApi()

        val result = api.syncBatch("trip-uuid-888-999", 12.5, SampleData.sampleTrack)

        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        assertThat((result as AppResult.Ok).value).isEqualTo(SampleData.sampleTrack.size)
    }

    @Test
    fun `endTrip delega al api REST y devuelve TripSummary`() = runTest {
        val api = buildApi()

        val result = api.endTrip(
            "trip-uuid-888-999", "2026-06-08T12:00:00Z",
            LatLng(-16.420, -71.530), 3.8, 145.0,
        )

        assertThat(result).isInstanceOf(AppResult.Ok::class.java)
        val summary = (result as AppResult.Ok).value
        assertThat(summary.serverDistanceKm).isEqualTo(146.0)
        assertThat(summary.co2Kg).isEqualTo(112.5)
    }

    // ── AuthFirebaseModule factory ────────────────────────────────────────────

    @Test
    fun `AuthFirebaseModule-create devuelve instancia no nula`() {
        val api = AuthFirebaseModule.create(
            authProvider = FakeFirebaseAuthProvider(),
            delegate = FakeMobileApi(),
        )
        assertThat(api).isNotNull()
    }
}
