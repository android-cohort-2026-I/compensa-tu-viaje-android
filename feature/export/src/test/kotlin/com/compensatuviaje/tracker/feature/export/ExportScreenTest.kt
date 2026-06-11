package com.compensatuviaje.tracker.feature.export

import com.compensatuviaje.tracker.model.TripStatus
import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExportScreenTest {

    @Test fun `csv contiene encabezado de resumen`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        assertThat(csv).contains("trip_id,estado,inicio,fin,distancia_local_km")
    }

    @Test fun `csv contiene el id del viaje`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        assertThat(csv).contains(SampleData.sampleTrip.id)
    }

    @Test fun `csv contiene encabezado de puntos GPS cuando hay puntos`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        assertThat(csv).contains("timestamp,lat,lng,velocidad_kmh")
    }

    @Test fun `csv contiene coordenadas del primer punto`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        assertThat(csv).contains("-12.047")
        assertThat(csv).contains("-77.043")
    }

    @Test fun `csv con lista vacia solo tiene seccion de resumen`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, emptyList())
        assertThat(csv).contains("trip_id")
        assertThat(csv).doesNotContain("timestamp,lat,lng")
    }

    @Test fun `csv maneja viaje sin fin`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, emptyList())
        assertThat(csv).contains("en_curso")
    }

    @Test fun `csv maneja viaje completo con distancia servidor`() {
        val trip = SampleData.sampleTrip.copy(
            status = TripStatus.COMPLETED,
            endedAtIso = "2026-06-01T16:10:00Z",
            totalLocalDistanceKm = 145.8,
            serverDistanceKm = 146.0,
            co2Kg = 112.5,
            isSyncedToServer = true,
        )
        val csv = CsvGenerator.generateTripSummary(trip, SampleData.sampleTrack)
        assertThat(csv).contains("145.800")
        assertThat(csv).contains("146.000")
        assertThat(csv).contains("si")
    }

    @Test fun `csv tiene tantas filas de puntos como la lista de entrada`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        val pointLines = csv.lines().filter { it.contains("2026-06-01") && it.contains("-12.") }
        assertThat(pointLines).hasSize(SampleData.sampleTrack.size)
    }

    @Test fun `csv formatea velocidad con un decimal`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        assertThat(csv).contains("45.5")
        assertThat(csv).contains("50.2")
    }

    @Test fun `csv indica no para puntos no sincronizados`() {
        val csv = CsvGenerator.generateTripSummary(SampleData.sampleTrip, SampleData.sampleTrack)
        assertThat(csv).contains("no")
    }

    @Test fun `estado Ready tiene conteo correcto de puntos`() {
        val state = ExportUiState.Ready(
            trip = SampleData.sampleTrip,
            points = SampleData.sampleTrack,
            pointCount = SampleData.sampleTrack.size,
        )
        assertThat(state.pointCount).isEqualTo(3)
    }

    @Test fun `formato CSV tiene extension y mime correctos`() {
        assertThat(ExportFormat.CSV.extension).isEqualTo("csv")
        assertThat(ExportFormat.CSV.mimeType).isEqualTo("text/csv")
    }
}
