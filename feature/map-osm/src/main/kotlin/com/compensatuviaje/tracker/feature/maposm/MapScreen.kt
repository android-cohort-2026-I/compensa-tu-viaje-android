package com.compensatuviaje.tracker.feature.maposm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun OsmMapScreen(
    viewModel: OsmMapViewModel,
    tripId: String
) {
    val uiState by viewModel.loadPoints(tripId).collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is OsmMapUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
            is OsmMapUiState.Empty -> Text(
                text = "No hay puntos GPS para este viaje.",
                modifier = Modifier.align(Alignment.Center)
            )
            is OsmMapUiState.Error -> Text(
                text = "Error: ${state.message}",
                modifier = Modifier.align(Alignment.Center)
            )
            is OsmMapUiState.Success -> OsmMapView(
                points = state.points,
                startPoint = state.startPoint,
                endPoint = state.endPoint,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}