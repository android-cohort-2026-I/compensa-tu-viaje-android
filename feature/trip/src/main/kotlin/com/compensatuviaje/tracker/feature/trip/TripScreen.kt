package com.compensatuviaje.tracker.feature.trip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TripScreen(viewModel: TripViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState.stage) {
            TripStage.IDLE -> IdleScreen(isGpsOk = uiState.isGpsOk, onStartClick = { viewModel.onStartClick() })
            TripStage.IN_PROGRESS -> InProgressScreen(
                distanceKm = uiState.distanceKm,
                isSyncing = uiState.isSyncing,
                onFinishClick = { viewModel.onFinishClick() }
            )
            TripStage.CONFIRM_END -> ConfirmEndDialog(
                onConfirm = { viewModel.onConfirmEnd() },
                onCancel = { viewModel.onCancelEnd() }
            )
            TripStage.PROCESSING -> ProcessingScreen()
            TripStage.SUMMARY -> SummaryScreen(
                serverDistanceKm = uiState.serverDistanceKm ?: uiState.distanceKm,
                duration = uiState.durationFormatted,
                licensePlate = uiState.licensePlate,
                onDismiss = { viewModel.onSummaryDismiss() }
            )
        }
    }
}

@Composable
fun IdleScreen(isGpsOk: Boolean, onStartClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (isGpsOk) "GPS Activo" else "GPS Desactivado", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = isGpsOk
        ) {
            Text("Iniciar Ruta")
        }
    }
}

@Composable
fun InProgressScreen(distanceKm: Double, isSyncing: Boolean, onFinishClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Viaje en Curso", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(String.format("%.2f km", distanceKm), style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("00:00:00", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(if (isSyncing) "Sincronizando ☁️" else "Sin conexión 📵")
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onFinishClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Finalizar")
        }
    }
}

@Composable
fun ConfirmEndDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Finalizar Viaje") },
        text = { Text("¿Está seguro de que desea finalizar el viaje actual? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.height(56.dp)) { Text("Sí, Finalizar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, modifier = Modifier.height(56.dp)) { Text("Cancelar") }
        }
    )
}

@Composable
fun ProcessingScreen() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Procesando cierre de viaje...")
    }
}

@Composable
fun SummaryScreen(serverDistanceKm: Double, duration: String, licensePlate: String, onDismiss: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Resumen de Viaje", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Distancia: ${String.format("%.2f", serverDistanceKm)} km", style = MaterialTheme.typography.titleLarge)
        Text("Duración: $duration")
        Text("Patente: $licensePlate")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Volver al Inicio")
        }
    }
}