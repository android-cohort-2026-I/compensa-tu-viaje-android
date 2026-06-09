package com.compensatuviaje.tracker.feature.maposm

import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.testing.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OsmMapViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepository: FakeGpsPointRepository
    private lateinit var viewModel: OsmMapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGpsPointRepository()
        viewModel = OsmMapViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @Test
    fun `loadPoints con sampleTrack emite Success con puntos correctos`() = runTest {
        // Preparamos el fake con los puntos del SampleData
        fakeRepository.setPoints(SampleData.sampleTrack)

        val state = viewModel.loadPoints("trip-1").first { it !is OsmMapUiState.Loading }

        assertTrue(state is OsmMapUiState.Success)
        val success = state as OsmMapUiState.Success

        assertEquals(3, success.points.size)

        assertEquals(SampleData.sampleTrack.first(), success.startPoint)
        assertEquals(SampleData.sampleTrack.last(), success.endPoint)
    }


    @Test
    fun `loadPoints con lista vacia emite Empty`() = runTest {
        fakeRepository.setPoints(emptyList())

        val state = viewModel.loadPoints("trip-1").first { it !is OsmMapUiState.Loading }

        assertEquals(OsmMapUiState.Empty, state)
    }


    @Test
    fun `loadPoints con un solo punto emite Success donde startPoint y endPoint son iguales`() = runTest {
        val soloPoint = listOf(SampleData.sampleTrack.first())
        fakeRepository.setPoints(soloPoint)

        val state = viewModel.loadPoints("trip-1").first { it !is OsmMapUiState.Loading }

        assertTrue(state is OsmMapUiState.Success)
        val success = state as OsmMapUiState.Success

        assertEquals(1, success.points.size)
        // Con un punto, inicio y fin son el mismo
        assertEquals(success.startPoint, success.endPoint)
    }


    @Test
    fun `loadPoints con puntos de baja precision igual emite Success con todos los puntos`() = runTest {
        val lowAccuracyPoints = listOf(
            GpsPoint(10, "trip-1", "2026-06-01T15:00:00Z", -12.0470, -77.0430, 55.0, 180.0, 4.1),
            GpsPoint(11, "trip-1", "2026-06-01T15:01:00Z", -12.0485, -77.0430, 60.0, 180.0, 3.8),
        )
        fakeRepository.setPoints(lowAccuracyPoints)

        val state = viewModel.loadPoints("trip-1").first { it !is OsmMapUiState.Loading }

        assertTrue(state is OsmMapUiState.Success)
        assertEquals(2, (state as OsmMapUiState.Success).points.size)
    }
}