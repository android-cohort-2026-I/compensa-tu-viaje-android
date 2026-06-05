package com.compensatuviaje.tracker.feature.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel(
    private val tripRepository: TripRepository,
    private val gpsPointRepository: GpsPointRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Loading)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun loadFromData(trip: Trip, points: List<GpsPoint>) {
        if (points.isEmpty() && trip.totalLocalDistanceKm == 0.0) {
            _uiState.value = ExportUiState.Empty
            return
        }
        _uiState.value = ExportUiState.Ready(
            trip = trip,
            points = points,
            pointCount = points.size,
        )
    }

    fun exportCsv(context: Context, trip: Trip, points: List<GpsPoint>) {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Exporting(ExportFormat.CSV)
            runCatching {
                val csv = CsvGenerator.generateTripSummary(trip, points)
                val fileName = "viaje_${trip.id}_${trip.startedAtIso.take(10)}.csv"
                val file = File(context.cacheDir, fileName)
                file.writeText(csv, Charsets.UTF_8)
                shareCsv(context, file)
                _uiState.value = ExportUiState.Done(ExportFormat.CSV)
            }.onFailure {
                _uiState.value = ExportUiState.Error("No se pudo generar el archivo.")
            }
        }
    }

    private fun shareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = ExportFormat.CSV.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Resumen de viaje — ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir viaje").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    class Factory(
        private val tripRepository: TripRepository,
        private val gpsPointRepository: GpsPointRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExportViewModel(tripRepository, gpsPointRepository) as T
    }
}
