package com.compensatuviaje.tracker.feature.mapgoogle

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapGoogleScreen(
    // Recibimos la lista de puntos GPS. Por defecto está vacía para que no explote si no hay datos.
    routePoints: List<LatLng> = emptyList()
) {
    // Coordenada por defecto (Arequipa) en caso de que aún no haya ruta
    val defaultLocation = LatLng(-16.39889, -71.53500)

    // Si la ruta tiene puntos, centramos la cámara en el inicio del viaje, sino en Arequipa
    val startLocation = routePoints.firstOrNull() ?: defaultLocation

    // Controla hacia dónde mira el mapa y qué tanto zoom tiene
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 14f)
    }

    // Render del mapa
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Solo dibujamos cosas si realmente hay una ruta guardada
        if (routePoints.isNotEmpty()) {

            // Render de la polyline de la ruta
            Polyline(
                points = routePoints,
                color = Color.Blue,
                width = 10f
            )

            // Marcador de inicio usando rememberMarkerState
            Marker(
                state = rememberMarkerState(position = routePoints.first()),
                title = "Inicio del viaje"
            )

            // Marcador de fin usando rememberMarkerState
            Marker(
                state = rememberMarkerState(position = routePoints.last()),
                title = "Fin del viaje"
            )
        }
    }
}

// --- FASE DE VALIDACIÓN VISUAL ---
@Preview(showSystemUi = true)
@Composable
fun MapGoogleScreenPreview() {
    // Simulamos un track de ejemplo en Arequipa para cumplir con el criterio de aceptación de pruebas
    val mockTrack = listOf(
        LatLng(-16.39889, -71.53500),
        LatLng(-16.40444, -71.53944),
        LatLng(-16.40917, -71.54194)
    )

    MapGoogleScreen(routePoints = mockTrack)
}