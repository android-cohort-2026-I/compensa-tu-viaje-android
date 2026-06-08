package com.compensatuviaje.tracker.feature.locationservice

import com.compensatuviaje.tracker.domain.LocationTracker
import com.compensatuviaje.tracker.model.GpsPoint
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackerTest {

    private lateinit var fakeTracker: FakeLocationTracker

    @Before
    fun setUp() {
        fakeTracker = FakeLocationTracker()
    }

    @Test
    fun `estado inicial no esta rastreando`() = runTest {
        val isTracking = fakeTracker.isTracking.first()
        assertThat(isTracking).isFalse()
    }

    @Test
    fun `al llamar start isTracking cambia a true`() = runTest {
        fakeTracker.start("viaje-001")
        val isTracking = fakeTracker.isTracking.first()
        assertThat(isTracking).isTrue()
    }

    @Test
    fun `al llamar stop isTracking cambia a false`() = runTest {
        fakeTracker.start("viaje-001")
        fakeTracker.stop()
        val isTracking = fakeTracker.isTracking.first()
        assertThat(isTracking).isFalse()
    }

    @Test
    fun `puntos con precision mayor a 50 deben ser filtrados`() = runTest {
        val puntoBueno = crearPunto(id = 1, accuracy = 30.0)
        val puntoMalo = crearPunto(id = 2, accuracy = 75.0)

        val filtrados = listOf(puntoBueno, puntoMalo)
            .filter { it.accuracyMeters <= LocationForegroundService.MAX_ACCURACY_METERS }

        assertThat(filtrados).hasSize(1)
        assertThat(filtrados.first().id).isEqualTo(1)
    }

    @Test
    fun `puntos con precision exactamente 50 son aceptados`() {
        val punto = crearPunto(accuracy = 50.0)
        val aceptado = punto.accuracyMeters <= LocationForegroundService.MAX_ACCURACY_METERS
        assertThat(aceptado).isTrue()
    }

    @Test
    fun `puntos emitidos por el tracker se reciben correctamente`() = runTest {
        val punto = crearPunto(id = 10, accuracy = 20.0)
        val recibidos = mutableListOf<GpsPoint>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeTracker.points.toList(recibidos)
        }

        fakeTracker.start("viaje-001")
        fakeTracker.emitir(punto)
        fakeTracker.stop()
        job.cancel()

        assertThat(recibidos).contains(punto)
    }

    private fun crearPunto(id: Long = 0, accuracy: Double = 10.0) = GpsPoint(
        id = id,
        tripId = "viaje-test",
        timestampIso = "2025-01-01T00:00:00Z",
        lat = -16.409,
        lng = -71.537,
        speedKmh = 0.0,
        heading = 0.0,
        accuracyMeters = accuracy,
        synced = false,
    )
}

// ─── Fake local (no depende de :core:testing para este módulo) ───────────────

class FakeLocationTracker : LocationTracker {

    private val _points = MutableSharedFlow<GpsPoint>(extraBufferCapacity = 64)
    override val points: Flow<GpsPoint> = _points

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: Flow<Boolean> = _isTracking

    override fun start(tripId: String) { _isTracking.value = true }
    override fun stop() { _isTracking.value = false }

    suspend fun emitir(punto: GpsPoint) { _points.emit(punto) }
}
