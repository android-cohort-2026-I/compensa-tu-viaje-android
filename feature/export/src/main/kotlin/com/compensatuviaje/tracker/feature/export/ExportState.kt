package com.compensatuviaje.tracker.feature.export

import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.Trip

sealed interface ExportUiState {
    data object Loading : ExportUiState
    data object Empty : ExportUiState
    data class Ready(
        val trip: Trip,
        val points: List<GpsPoint>,
        val pointCount: Int,
    ) : ExportUiState
    data class Exporting(val format: ExportFormat) : ExportUiState
    data class Error(val message: String) : ExportUiState
    data class Done(val format: ExportFormat) : ExportUiState
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV", "csv", "text/csv"),
}
