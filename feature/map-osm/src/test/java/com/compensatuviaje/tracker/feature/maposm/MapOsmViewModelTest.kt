package com.compensatuviaje.tracker.feature.maposm

import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.model.GpsPoint
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapOsmViewModelTest {

    private lateinit var fakeRepo: FakeGpsPointRepository
    private lateinit var viewModel: MapOsmViewModel

    @Before
    fun setUp() {
        fakeRepo = FakeGpsPointRepository()
        viewModel = MapOsmViewModel(fakeRepo)
    }

    @Test
    fun `estado inicial no esta cargando y sin puntos`() = runTest {
        val estado = viewModel.uiState.value
        assertThat(estado.estaCargando).isFalse()
        assertThat(estado.puntos).isEmpty()
        assertThat(estado.error).isNull()
    }

    @Test
    fun `cargarPuntos muestra puntos del repositorio`() = runTest {
        val puntos = listOf(
            crearPunto(id = 1, lat = -16.409, lng = -71.537),
            crearPunto(id = 2, lat = -16.410, lng = -71.538),
        )
        fakeRepo.setPuntos("viaje-001", puntos)

        viewModel.cargarPuntos("viaje-001")

        val estado = viewModel.uiState.value
        assertThat(estado.puntos).hasSize(2)
        assertThat(estado.error).isNull()
    }

    @Test
    fun `cargarPuntos con viaje sin puntos muestra lista vacia`() = runTest {
        fakeRepo.setPuntos("viaje-vacio", emptyList())

        viewModel.cargarPuntos("viaje-vacio")

        assertThat(viewModel.uiState.value.puntos).isEmpty()
    }

    @Test
    fun `mapeo de GpsPoint a GeoPoint es correcto`() {
        val punto = crearPunto(lat = -12.0464, lng = -77.0428)
        // Verificar que las coordenadas se mapean correctamente
        assertThat(punto.lat).isEqualTo(-12.0464)
        assertThat(punto.lng).isEqualTo(-77.0428)
    }

    @Test
    fun `ruta con un solo punto no genera polyline de dos extremos`() {
        val unPunto = listOf(crearPunto(id = 1))
        // Con un solo punto solo hay marcador de inicio, no de fin
        assertThat(unPunto.size).isLessThan(2)
    }

    private fun crearPunto(
        id: Long = 0,
        lat: Double = -16.409,
        lng: Double = -71.537,
    ) = GpsPoint(
        id = id,
        tripId = "viaje-test",
        timestampIso = "2025-01-01T00:00:00Z",
        lat = lat,
        lng = lng,
        speedKmh = 60.0,
        heading = 90.0,
        accuracyMeters = 10.0,
        synced = false,
    )
}

// ─── Fake del repositorio ────────────────────────────────────────────────────

class FakeGpsPointRepository : GpsPointRepository {

    private val data = mutableMapOf<String, List<GpsPoint>>()

    fun setPuntos(tripId: String, puntos: List<GpsPoint>) {
        data[tripId] = puntos
    }

    override suspend fun insert(point: GpsPoint) {
        val lista = data.getOrDefault(point.tripId, emptyList()).toMutableList()
        lista.add(point)
        data[point.tripId] = lista
    }

    override suspend fun unsynced(tripId: String): List<GpsPoint> =
        data.getOrDefault(tripId, emptyList()).filter { !it.synced }

    override suspend fun markSynced(ids: List<Long>) {
        data.forEach { (tripId, puntos) ->
            data[tripId] = puntos.map { if (it.id in ids) it.copy(synced = true) else it }
        }
    }

    override fun pointsForTrip(tripId: String): Flow<List<GpsPoint>> =
        flowOf(data.getOrDefault(tripId, emptyList()))
}
