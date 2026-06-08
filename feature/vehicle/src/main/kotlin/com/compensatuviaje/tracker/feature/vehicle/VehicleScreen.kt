package com.compensatuviaje.tracker.feature.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.designsystem.BigActionButton
import com.compensatuviaje.tracker.designsystem.LoadingState
import com.compensatuviaje.tracker.designsystem.EmptyState
import com.compensatuviaje.tracker.designsystem.ErrorState
import com.compensatuviaje.tracker.designsystem.AppTheme
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TripRepository
import com.compensatuviaje.tracker.model.TripStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// =======================================================
// 1. ESTADOS DE LA UI
// =======================================================
sealed interface VehicleUiState {
    object Loading : VehicleUiState
    data class Success(
        val plate: String,
        val category: String,
        val isProcessing: Boolean = false,
        val errorMessage: String? = null
    ) : VehicleUiState
    object EmptyData : VehicleUiState
    object NavigateToHome : VehicleUiState
}

// =======================================================
// 2. VIEWMODEL
// =======================================================
class VehicleViewModel(
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VehicleUiState>(VehicleUiState.Loading)
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    init {
        loadVehicleData()
    }

    private fun loadVehicleData() {
        viewModelScope.launch {
            sessionRepository.current
                .catch { _uiState.value = VehicleUiState.EmptyData }
                .collect { session ->
                    if (session != null && session.truck.licensePlate.isNotBlank()) {
                        _uiState.value = VehicleUiState.Success(
                            plate = session.truck.licensePlate,
                            category = session.truck.category
                        )
                    } else {
                        _uiState.value = VehicleUiState.EmptyData
                    }
                }
        }
    }

    fun confirmVehicle() {
        val currentState = _uiState.value as? VehicleUiState.Success ?: return
        if (currentState.isProcessing) return

        _uiState.value = currentState.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val currentTrip = tripRepository.activeTrip().first()
                if (currentTrip != null) {
                    val updatedTrip = currentTrip.copy(
                        status = TripStatus.IN_PROGRESS,
                        isSyncedToServer = false
                    )
                    tripRepository.update(updatedTrip)
                }
                _uiState.value = VehicleUiState.NavigateToHome
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isProcessing = false,
                    errorMessage = "Error al confirmar el vehículo: ${e.message}"
                )
            }
        }
    }
}

// =======================================================
// 3. PANTALLA EN JETPACK COMPOSE (SCREEN CONTENEDORA)
// =======================================================
@Composable
fun VehicleScreen(
    viewModel: VehicleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState is VehicleUiState.NavigateToHome) {
        LaunchedEffect(Unit) {
            onNavigateToHome()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is VehicleUiState.Loading -> {
                LoadingState()
            }
            is VehicleUiState.EmptyData -> {
                EmptyState(text = "No se encontraron datos del vehículo activo en la sesión.")
            }
            is VehicleUiState.Success -> {
                VehicleContent(
                    state = state,
                    onConfirmClick = { viewModel.confirmVehicle() }
                )
            }
            else -> {}
        }
    }
}

// =======================================================
// 4. COMPOSABLE PURO (STATE-HOISTING)
// =======================================================
@Composable
fun VehicleContent(
    state: VehicleUiState.Success,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Confirmación de Vehículo",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Patente / Placa:", style = MaterialTheme.typography.labelLarge)
                Text(text = state.plate, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Categoría / Tipo:", style = MaterialTheme.typography.labelLarge)
                Text(text = state.category, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (state.errorMessage != null) {
            ErrorState(
                text = state.errorMessage,
                onRetry = onConfirmClick
            )
        } else if (state.isProcessing) {
            LoadingState()
        } else {
            BigActionButton(
                text = "Confirmar y Continuar",
                onClick = onConfirmClick
            )
        }
    }
}

// =======================================================
// 5. PREVIEW CON ESTADOS ESTÁTICOS
// =======================================================
@Preview(showBackground = true)
@Composable
fun VehicleContentPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            VehicleContent(
                state = VehicleUiState.Success(
                    plate = "ABC-123",
                    category = "Carga Pesada",
                    isProcessing = false
                ),
                onConfirmClick = {}
            )
        }
    }
}