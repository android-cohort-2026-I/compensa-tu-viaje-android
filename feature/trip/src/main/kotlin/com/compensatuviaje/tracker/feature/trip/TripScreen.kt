package com.compensatuviaje.tracker.feature.trip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.compensatuviaje.tracker.common.formatDuration
import com.compensatuviaje.tracker.common.formatKm
import com.compensatuviaje.tracker.designsystem.BigActionButton
import com.compensatuviaje.tracker.designsystem.ErrorState
import com.compensatuviaje.tracker.designsystem.GpsStatusIcon
import com.compensatuviaje.tracker.designsystem.LoadingState
import com.compensatuviaje.tracker.designsystem.SyncStatusIcon

@Composable
fun TripScreen(
    navController: NavController? = null,
    vm: TripViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        vm.updateGpsAvailable(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        vm.updatePermissions(granted)
    }

    when (val state = uiState) {
        is TripUiState.Idle -> HomeScreen(state = state, onStart = vm::startTrip)
        is TripUiState.Active -> TripActiveScreen(state = state, onEnd = vm::requestEndTrip)
        is TripUiState.ConfirmingEnd -> ConfirmEndDialog(
            onConfirm = vm::confirmEndTrip,
            onCancel = vm::cancelEnd,
        )
        is TripUiState.Processing -> ProcessingScreen()
        is TripUiState.Summary -> TripSummaryScreen(state = state)
        is TripUiState.Error -> ErrorState(
            text = state.message,
            onRetry = {
                navController?.navigate("auth") { popUpTo(0) }
            },
        )
    }
}

@Composable
private fun HomeScreen(state: TripUiState.Idle, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GpsStatusIcon(hasSignal = state.isGpsAvailable)
        Spacer(Modifier.height(16.dp))

        if (!state.hasPermissions) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = "Se requieren permisos de ubicación para rastrear el viaje.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        BigActionButton(
            text = "Iniciar Ruta",
            onClick = onStart,
            enabled = state.isGpsAvailable,
        )
    }
}

@Composable
private fun TripActiveScreen(state: TripUiState.Active, onEnd: () -> Unit) {
    BackHandler(onBack = onEnd)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = formatDuration(state.elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatKm(state.localDistanceKm),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(12.dp))
        SyncStatusIcon(isOnline = state.isOnline, recentlySynced = state.isSyncing)
        Spacer(Modifier.height(40.dp))
        BigActionButton(
            text = "Finalizar",
            onClick = onEnd,
            destructive = true,
        )
    }
}

@Composable
private fun ConfirmEndDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "¿Finalizar el viaje?",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "El viaje se cerrará y los datos se enviarán al servidor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(24.dp))
                BigActionButton(
                    text = "Finalizar Viaje",
                    onClick = onConfirm,
                    destructive = true,
                )
                Spacer(Modifier.height(8.dp))
                BigActionButton(
                    text = "Cancelar",
                    onClick = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ProcessingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingState()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cerrando viaje…",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun TripSummaryScreen(state: TripUiState.Summary) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Resumen del Viaje", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        Text(text = "Duración", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(text = formatDuration(state.durationSeconds), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(24.dp))

        Text(text = "Distancia", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        if (state.serverDistanceKm != null) {
            Text(text = formatKm(state.serverDistanceKm), style = MaterialTheme.typography.displaySmall)
        } else {
            Text(text = formatKm(state.localDistanceKm), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "(provisional — pendiente de sincronización con el servidor)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.truckPlate.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(text = "Camión", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(text = state.truckPlate, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
