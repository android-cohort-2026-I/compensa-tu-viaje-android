package com.compensatuviaje.tracker.feature.export

import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

// Fake local que sí podemos controlar
class TestTripRepository : TripRepository {
    val completedTripsFlow = MutableStateFlow<List<Trip>>(emptyList())
    val activeTripFlow = MutableStateFlow<Trip?>(null)
    override fun completedTrips(): Flow<List<Trip>> = completedTripsFlow
    override fun activeTrip(): Flow<Trip?> = activeTripFlow
    override suspend fun create(trip: Trip) { activeTripFlow.value = trip }
    override suspend fun update(trip: Trip) { activeTripFlow.value = trip }
    override suspend fun setStatus(tripId: String, status: TripStatus) {}
    override suspend fun get(tripId: String): Trip? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExportScreenTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TestTripRepository
    private lateinit var viewModel: ExportViewModel

    private val sampleTrip = Trip(
        id = "abc-123",
        status = TripStatus.COMPLETED,
        startedAtIso = "2026-06-01T10:00:00Z",
        endedAtIso = "2026-06-01T12:00:00Z",
        totalLocalDistanceKm = 42.5,
        isSyncedToServer = true,
        serverDistanceKm = 43.0,
        co2Kg = 32.5,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = TestTripRepository()
        viewModel = ExportViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Estado inicial ---

    @Test
    fun `estado inicial no tiene contenido exportado`() = runTest {
        assertThat(viewModel.uiState.value.exportedContent).isNull()
    }

    @Test
    fun `estado inicial formato es CSV`() = runTest {
        assertThat(viewModel.uiState.value.exportFormat).isEqualTo(ExportFormat.CSV)
    }

    @Test
    fun `despues de cargar isLoading es false`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    // --- Exportación ---

    @Test
    fun `exportTrips con viajes genera CSV con encabezado`() = runTest {
        repository.completedTripsFlow.value = listOf(sampleTrip)
        advanceUntilIdle()

        viewModel.exportTrips()

        val content = viewModel.uiState.value.exportedContent
        assertThat(content).isNotNull()
        assertThat(content).contains("id,estado,inicio")
    }

    @Test
    fun `exportTrips sin viajes pone error en estado`() = runTest {
        advanceUntilIdle()
        viewModel.exportTrips()

        assertThat(viewModel.uiState.value.error).isNotNull()
        assertThat(viewModel.uiState.value.exportedContent).isNull()
    }

    // --- Cambio de formato ---

    @Test
    fun `setFormat cambia el formato seleccionado`() = runTest {
        viewModel.setFormat(ExportFormat.PLAIN_TEXT)
        assertThat(viewModel.uiState.value.exportFormat).isEqualTo(ExportFormat.PLAIN_TEXT)
    }

    @Test
    fun `setFormat limpia contenido exportado previo`() = runTest {
        repository.completedTripsFlow.value = listOf(sampleTrip)
        advanceUntilIdle()
        viewModel.exportTrips()

        viewModel.setFormat(ExportFormat.PLAIN_TEXT)
        assertThat(viewModel.uiState.value.exportedContent).isNull()
    }

    // --- buildCsv ---

    @Test
    fun `buildCsv genera encabezado correcto`() {
        val csv = viewModel.buildCsv(emptyList())
        assertThat(csv).startsWith("id,estado,inicio,fin,distancia_km")
    }

    @Test
    fun `buildCsv incluye id y estado del viaje`() {
        val csv = viewModel.buildCsv(listOf(sampleTrip))
        assertThat(csv).contains("abc-123")
        assertThat(csv).contains("COMPLETED")
        assertThat(csv).contains("true")
    }

    @Test
    fun `buildCsv incluye distancias formateadas`() {
        val csv = viewModel.buildCsv(listOf(sampleTrip))
        assertThat(csv).contains("42")
        assertThat(csv).contains("43")
        assertThat(csv).contains("32")
    }

    @Test
    fun `buildCsv maneja campos opcionales nulos`() {
        val trip = Trip(
            id = "xyz",
            status = TripStatus.COMPLETED,
            startedAtIso = "2026-06-01T10:00:00Z",
        )
        val csv = viewModel.buildCsv(listOf(trip))
        assertThat(csv).contains("xyz")
        assertThat(csv).contains("COMPLETED")
    }

    // --- buildPlainText ---

    @Test
    fun `buildPlainText contiene encabezado de reporte`() {
        val text = viewModel.buildPlainText(emptyList())
        assertThat(text).contains("REPORTE DE VIAJES")
        assertThat(text).contains("Total: 0")
    }

    @Test
    fun `buildPlainText incluye id del viaje`() {
        val trip = Trip(
            id = "trip-999",
            status = TripStatus.COMPLETED,
            startedAtIso = "2026-06-01T10:00:00Z",
            totalLocalDistanceKm = 15.0,
        )
        val text = viewModel.buildPlainText(listOf(trip))
        assertThat(text).contains("trip-999")
    }

    @Test
    fun `buildPlainText incluye distancia del viaje`() {
        val trip = Trip(
            id = "trip-999",
            status = TripStatus.COMPLETED,
            startedAtIso = "2026-06-01T10:00:00Z",
            totalLocalDistanceKm = 15.0,
        )
        val text = viewModel.buildPlainText(listOf(trip))
        assertThat(text).contains("15")
    }

    // --- clearExport / clearError ---

    @Test
    fun `clearExport elimina el contenido exportado`() = runTest {
        repository.completedTripsFlow.value = listOf(sampleTrip)
        advanceUntilIdle()
        viewModel.exportTrips()

        viewModel.clearExport()
        assertThat(viewModel.uiState.value.exportedContent).isNull()
    }

    @Test
    fun `clearError elimina el error del estado`() = runTest {
        advanceUntilIdle()
        viewModel.exportTrips()
        viewModel.clearError()
        assertThat(viewModel.uiState.value.error).isNull()
    }
}
