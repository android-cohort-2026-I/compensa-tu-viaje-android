package com.compensatuviaje.tracker.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VehicleScreen() {
    VehicleScreen(
        uiState = VehicleUiState.Loading,
        onConfirm = {},
        onCloseSession = {},
    )
}

@Composable
fun VehicleScreen(
    viewModel: VehicleViewModel,
    onContinue: () -> Unit = {},
    onCloseSession: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                VehicleEvent.Continue -> onContinue()
                VehicleEvent.CloseSession -> onCloseSession()
            }
        }
    }

    VehicleScreen(
        uiState = uiState,
        onConfirm = viewModel::onConfirm,
        onCloseSession = viewModel::onCloseSession,
    )
}

@Composable
fun VehicleScreen(
    uiState: VehicleUiState,
    onConfirm: () -> Unit,
    onCloseSession: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            VehicleUiState.Loading -> CircularProgressIndicator()
            is VehicleUiState.Error -> VehicleErrorContent(
                message = uiState.message,
                onCloseSession = onCloseSession,
            )
            is VehicleUiState.Content -> VehicleContent(
                state = uiState,
                onConfirm = onConfirm,
            )
        }
    }
}

@Composable
private fun VehicleContent(
    state: VehicleUiState.Content,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Confirma tu camion",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = state.truck.licensePlate,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tipo: ${state.truck.category}",
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.hasActiveTrip) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hay un viaje activo asociado a esta sesion.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isConfirming,
        ) {
            if (state.isConfirming) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Confirmar y continuar")
            }
        }
    }
}

@Composable
private fun VehicleErrorContent(
    message: String,
    onCloseSession: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onCloseSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Cerrar sesion")
        }
    }
}
