package com.compensatuviaje.tracker.feature.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    uiState: ExportUiState,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Exportar viaje") }) }
    ) { padding ->
        Box(
            modifier = modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is ExportUiState.Loading   -> ExportLoadingContent()
                is ExportUiState.Empty     -> ExportEmptyContent()
                is ExportUiState.Ready     -> ExportReadyContent(
                    trip = uiState.trip,
                    pointCount = uiState.pointCount,
                    onExportCsv = onExportCsv,
                )
                is ExportUiState.Exporting -> ExportingContent(format = uiState.format)
                is ExportUiState.Done      -> ExportDoneContent(format = uiState.format)
                is ExportUiState.Error     -> ExportErrorContent(message = uiState.message)
            }
        }
    }
}

@Composable
private fun ExportLoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text("Cargando datos del viaje…")
    }
}

@Composable
private fun ExportEmptyContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Text("No hay datos para exportar",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("Completa un viaje antes de exportar.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExportReadyContent(
    trip: Trip,
    pointCount: Int,
    onExportCsv: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(Icons.Default.FileDownload, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Viaje listo para exportar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TripInfoRow("ID de viaje", trip.id.take(16) + if (trip.id.length > 16) "…" else "")
                TripInfoRow("Inicio", trip.startedAtIso.replace("T", " ").take(19))
                TripInfoRow("Fin", trip.endedAtIso?.replace("T", " ")?.take(19) ?: "En curso")
                TripInfoRow("Distancia local", "${"%.2f".format(trip.totalLocalDistanceKm)} km")
                trip.serverDistanceKm?.let {
                    TripInfoRow("Distancia servidor", "${"%.2f".format(it)} km")
                }
                TripInfoRow("Puntos GPS", "$pointCount registros")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onExportCsv,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null,
                modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar CSV", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TripInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExportingContent(format: ExportFormat) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text("Generando ${format.label}…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ExportDoneContent(format: ExportFormat) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("¡${format.label} listo!", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text("El archivo fue enviado al selector de apps para compartir.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExportErrorContent(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Text("Algo salió mal", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text(message, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PreviewReady() {
    val trip = Trip("trip-1", TripStatus.COMPLETED,
        "2026-06-01T14:30:00Z", "2026-06-01T16:10:00Z",
        145.8, true, 146.0, 112.5)
    ExportScreen(uiState = ExportUiState.Ready(trip, emptyList(), 128), onExportCsv = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PreviewEmpty() {
    ExportScreen(uiState = ExportUiState.Empty, onExportCsv = {})
}
