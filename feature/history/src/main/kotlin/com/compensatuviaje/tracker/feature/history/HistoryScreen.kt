package com.compensatuviaje.tracker.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.designsystem.LoadingState
import com.compensatuviaje.tracker.designsystem.EmptyState
import com.compensatuviaje.tracker.designsystem.AppTheme
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.Trip
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

// =======================================================
// 1. ESTADOS DE LA UI
// =======================================================
sealed interface HistoryUiState {
    object Loading : HistoryUiState
    object Empty : HistoryUiState
    data class Success(val trips: List<Trip>) : HistoryUiState
}

// =======================================================
// 2. VIEWMODEL
// =======================================================
class HistoryViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            tripRepository.completedTrips()
                .catch { _uiState.value = HistoryUiState.Empty }
                .collect { tripList ->
                    if (tripList.isEmpty()) {
                        _uiState.value = HistoryUiState.Empty
                    } else {
                        val sortedTrips = tripList.sortedByDescending { it.startedAtIso }
                        _uiState.value = HistoryUiState.Success(sortedTrips)
                    }
                }
        }
    }
}

// =======================================================
// 3. PANTALLA EN JETPACK COMPOSE (SCREEN CONTENEDORA)
// =======================================================
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryContent(
        uiState = uiState,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier
    )
}

// =======================================================
// 4. COMPOSABLE PURO (STATE-HOISTING)
// =======================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Historial de Viajes") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is HistoryUiState.Loading -> LoadingState()
                is HistoryUiState.Empty -> {
                    EmptyState(text = "No tienes viajes completados en el historial.")
                }
                is HistoryUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.trips) { trip ->
                            TripHistoryItem(trip = trip, onClick = { onNavigateToDetail(trip.id) })
                        }
                    }
                }
            }
        }
    }
}

// =======================================================
// 5. ELEMENTO DE LA LISTA CON INDICADOR DE SINCRONIZACIÓN
// =======================================================
@Composable
fun TripHistoryItem(trip: Trip, onClick: () -> Unit) {
    val durationText = remember(trip.startedAtIso, trip.endedAtIso) {
        if (trip.endedAtIso != null) {
            try {
                val start = Instant.parse(trip.startedAtIso)
                val end = Instant.parse(trip.endedAtIso)
                val duration = Duration.between(start, end)
                val hours = duration.toHours()
                val minutes = duration.toMinutes() % 60
                if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
            } catch (e: Exception) {
                "Tiempo no registrado"
            }
        } else {
            "En curso"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "ID: ${trip.id}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = trip.startedAtIso.substringBefore("T"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Distancia", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${trip.totalLocalDistanceKm} km", style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Duración", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = durationText, style = MaterialTheme.typography.bodyLarge)
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trip.isSyncedToServer) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Sincronizado al servidor",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sincronizado",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Pendiente de sincronizar",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

// =======================================================
// 6. PREVIEWS CON ESTADOS ESTÁTICOS
// =======================================================
@Preview(showBackground = true)
@Composable
fun HistoryContentSuccessPreview() {
    AppTheme {
        HistoryContent(
            uiState = HistoryUiState.Success(
                listOf(
                    Trip(id = "viaje_123", status = com.compensatuviaje.tracker.model.TripStatus.COMPLETED, startedAtIso = "2026-06-05T08:00:00Z", endedAtIso = "2026-06-05T10:15:00Z", totalLocalDistanceKm = 85.4, isSyncedToServer = true),
                    Trip(id = "viaje_456", status = com.compensatuviaje.tracker.model.TripStatus.COMPLETED, startedAtIso = "2026-06-04T14:00:00Z", endedAtIso = "2026-06-04T14:45:00Z", totalLocalDistanceKm = 32.1, isSyncedToServer = false)
                )
            ),
            onNavigateToDetail = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryContentEmptyPreview() {
    AppTheme {
        HistoryContent(
            uiState = HistoryUiState.Empty,
            onNavigateToDetail = {}
        )
    }
}