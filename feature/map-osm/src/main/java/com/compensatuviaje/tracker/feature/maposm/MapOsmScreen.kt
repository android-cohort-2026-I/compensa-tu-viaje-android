package com.compensatuviaje.tracker.feature.maposm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compensatuviaje.tracker.model.GpsPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Pantalla principal del módulo map-osm.
 * Muestra la ruta de un viaje sobre OpenStreetMap sin necesitar API key.
 */
@Composable
fun MapOsmScreen(
    tripId: String,
    modifier: Modifier = Modifier,
    viewModel: MapOsmViewModel = viewModel(
        factory = MapOsmViewModel.Factory
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tripId) {
        viewModel.cargarPuntos(tripId)
    }

    when {
        uiState.estaCargando -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.puntos.isEmpty() -> {
            EstadoVacio(modifier = modifier)
        }

        else -> {
            MapaOsm(puntos = uiState.puntos, modifier = modifier)
        }
    }
}

// ─── Mapa OSM ────────────────────────────────────────────────────────────────

@Composable
private fun MapaOsm(
    puntos: List<GpsPoint>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true

                dibujarRuta(puntos)

                // Centrar en el último punto (posición más reciente)
                val centro = GeoPoint(puntos.last().lat, puntos.last().lng)
                controller.setZoom(15.0)
                controller.setCenter(centro)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            mapView.dibujarRuta(puntos)
            val ultimo = GeoPoint(puntos.last().lat, puntos.last().lng)
            mapView.controller.animateTo(ultimo)
            mapView.invalidate()
        }
    )
}

// ─── Extensión para dibujar la ruta ─────────────────────────────────────────

private fun MapView.dibujarRuta(puntos: List<GpsPoint>) {
    val geoPoints = puntos.map { GeoPoint(it.lat, it.lng) }

    // Polyline de la ruta
    val polyline = Polyline().apply {
        setPoints(geoPoints)
        outlinePaint.color = android.graphics.Color.BLUE
        outlinePaint.strokeWidth = 6f
    }
    overlays.add(polyline)

    // Marcador de inicio (verde)
    val marcadorInicio = Marker(this).apply {
        position = geoPoints.first()
        title = "Inicio del viaje"
        snippet = "Punto de partida"
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    overlays.add(marcadorInicio)

    // Marcador de fin (solo si hay más de un punto)
    if (geoPoints.size > 1) {
        val marcadorFin = Marker(this).apply {
            position = geoPoints.last()
            title = "Fin del viaje"
            snippet = "Destino final"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        overlays.add(marcadorFin)
    }
}

// ─── Estado vacío ────────────────────────────────────────────────────────────

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Sin ruta disponible",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inicia un viaje para ver la ruta aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
