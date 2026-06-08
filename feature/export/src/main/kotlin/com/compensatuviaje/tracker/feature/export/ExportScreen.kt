package com.compensatuviaje.tracker.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compensatuviaje.tracker.designsystem.BigActionButton
import com.compensatuviaje.tracker.designsystem.EmptyState
import com.compensatuviaje.tracker.designsystem.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // Mostrar errores en snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar viajes") },
                actions = {
                    if (uiState.exportedContent != null) {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(uiState.exportedContent!!))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingState()
                    }
                }

                uiState.exportedContent != null -> {
                    ExportPreview(
                        content = uiState.exportedContent!!,
                        onCopy = {
                            clipboard.setText(AnnotatedString(uiState.exportedContent!!))
                        },
                        onBack = { viewModel.clearExport() },
                    )
                }

                else -> {
                    ExportConfig(
                        uiState = uiState,
                        onFormatSelected = viewModel::setFormat,
                        onExport = viewModel::exportTrips,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportConfig(
    uiState: ExportUiState,
    onFormatSelected: (ExportFormat) -> Unit,
    onExport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))

        // Resumen de viajes disponibles
        Text(
            text = "Viajes completados: ${uiState.trips.size}",
            style = MaterialTheme.typography.titleMedium,
        )

        if (uiState.trips.isNotEmpty()) {
            val totalKm = uiState.trips.sumOf { it.totalLocalDistanceKm }
            val totalCo2 = uiState.trips.mapNotNull { it.co2Kg }.sum()
            Text(
                text = "Distancia total: ${"%.1f".format(totalKm)} km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (totalCo2 > 0) {
                Text(
                    text = "CO₂ total: ${"%.2f".format(totalCo2)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Formato de exportación",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportFormat.entries.forEach { format ->
                FilterChip(
                    selected = uiState.exportFormat == format,
                    onClick = { onFormatSelected(format) },
                    label = { Text(format.label) },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.trips.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState("No hay viajes completados aún")
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        BigActionButton(
            text = "Generar exportación",
            onClick = onExport,
            enabled = uiState.trips.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExportPreview(
    content: String,
    onCopy: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Vista previa", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar al portapapeles")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Caja con el contenido exportado, scrollable
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BigActionButton(
            text = "Copiar al portapapeles",
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BigActionButton(
            text = "Nueva exportación",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
