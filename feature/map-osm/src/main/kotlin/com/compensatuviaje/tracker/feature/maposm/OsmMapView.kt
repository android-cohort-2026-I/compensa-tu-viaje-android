package com.compensatuviaje.tracker.feature.maposm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.compensatuviaje.tracker.model.GpsPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
    points: List<GpsPoint>,
    startPoint: GpsPoint,
    endPoint: GpsPoint,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            val polyline = Polyline().apply {
                setPoints(points.map { GeoPoint(it.lat, it.lng) })
            }
            mapView.overlays.add(polyline)
            val start = Marker(mapView).apply {
                position = GeoPoint(startPoint.lat, startPoint.lng)
                title = "Inicio"
            }
            mapView.overlays.add(start)
            val end = Marker(mapView).apply {
                position = GeoPoint(endPoint.lat, endPoint.lng)
                title = "Fin"
            }
            mapView.overlays.add(end)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(GeoPoint(startPoint.lat, startPoint.lng))
            mapView.invalidate()
        },
        modifier = modifier
    )
}