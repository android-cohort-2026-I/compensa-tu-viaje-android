package com.compensatuviaje.tracker.feature.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    // Cambiado para evitar usar extensiones que no estén importadas en el gradle
    val uiState = viewModel.uiState.value

    LaunchedEffect(Unit) {
        viewModel.loadTripStatistics()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Estadísticas del Grupo BlackDog",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            StatCard(label = "Total de Viajes Registrados", value = "${uiState.totalTrips}")
            Spacer(modifier = Modifier.height(12.dp))
            StatCard(label = "Distancia Recorrida Total", value = "${uiState.totalDistanceKm} Km")
            Spacer(modifier = Modifier.height(12.dp))
            StatCard(label = "Tiempo Total en Ruta", value = "${uiState.totalDurationHours} Horas")
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}