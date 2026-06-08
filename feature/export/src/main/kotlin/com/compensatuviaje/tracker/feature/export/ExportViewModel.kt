package com.compensatuviaje.tracker.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.common.formatKm
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
    val exportedContent: String? = null,
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val error: String? = null,
)

enum class ExportFormat(val label: String) {
    CSV("CSV"),
    PLAIN_TEXT("Texto plano"),
}

class ExportViewModel(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        loadTrips()
    }

    private fun loadTrips() {
        viewModelScope.launch {
            tripRepository.completedTrips().collect { trips ->
                _uiState.update { it.copy(trips = trips, isLoading = false) }
            }
        }
    }

    fun setFormat(format: ExportFormat) {
        _uiState.update { it.copy(exportFormat = format, exportedContent = null) }
    }

    fun exportTrips() {
        val trips = _uiState.value.trips
        if (trips.isEmpty()) {
            _uiState.update { it.copy(error = "No hay viajes completados para exportar") }
            return
        }
        val content = when (_uiState.value.exportFormat) {
            ExportFormat.CSV -> buildCsv(trips)
            ExportFormat.PLAIN_TEXT -> buildPlainText(trips)
        }
        _uiState.update { it.copy(exportedContent = content, error = null) }
    }

    fun clearExport() {
        _uiState.update { it.copy(exportedContent = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    internal fun buildCsv(trips: List<Trip>): String {
        val header = "id,estado,inicio,fin,distancia_km,distancia_servidor_km,co2_kg,sincronizado"
        val rows = trips.joinToString("\n") { t ->
            listOf(
                t.id,
                t.status.name,
                t.startedAtIso,
                t.endedAtIso ?: "",
                "%.2f".format(java.util.Locale.US, t.totalLocalDistanceKm),
                t.serverDistanceKm.let { if (it != null) "%.2f".format(java.util.Locale.US, it) else "" },
                t.co2Kg.let { if (it != null) "%.2f".format(java.util.Locale.US, it) else "" },
                t.isSyncedToServer.toString(),
            ).joinToString(",")
        }
        return "$header\n$rows"
    }

    internal fun buildPlainText(trips: List<Trip>): String {
        val sb = StringBuilder()
        sb.appendLine("=== REPORTE DE VIAJES ===")
        sb.appendLine("Total: ${trips.size} viaje(s)")
        sb.appendLine()
        trips.forEachIndexed { i, t ->
            sb.appendLine("Viaje ${i + 1}")
            sb.appendLine("  ID       : ${t.id}")
            sb.appendLine("  Estado   : ${t.status.name}")
            sb.appendLine("  Inicio   : ${t.startedAtIso}")
            sb.appendLine("  Fin      : ${t.endedAtIso ?: "—"}")
            sb.appendLine("  Distancia: ${formatKm(t.totalLocalDistanceKm)}")
            if (t.serverDistanceKm != null) {
                sb.appendLine("  Dist.srv : ${formatKm(t.serverDistanceKm!!)}")
            }
            if (t.co2Kg != null) {
                sb.appendLine("  CO₂      : ${"%.2f".format(java.util.Locale.US, t.co2Kg!!)} kg")
            }
            sb.appendLine("  Sync     : ${if (t.isSyncedToServer) "Sí" else "No"}")
            sb.appendLine()
        }
        return sb.toString()
    }
}
