package com.compensatuviaje.tracker.feature.export

import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.Trip

object CsvGenerator {

    fun generateTripSummary(trip: Trip, points: List<GpsPoint>): String {
        val sb = StringBuilder()

        sb.appendLine("# Resumen de Viaje")
        sb.appendLine("trip_id,estado,inicio,fin,distancia_local_km,distancia_servidor_km,co2_kg,sincronizado")
        sb.appendLine(
            "${trip.id}," +
            "${trip.status.name}," +
            "${trip.startedAtIso}," +
            "${trip.endedAtIso ?: "en_curso"}," +
            "${"%.3f".format(trip.totalLocalDistanceKm)}," +
            "${"%.3f".format(trip.serverDistanceKm ?: 0.0)}," +
            "${"%.3f".format(trip.co2Kg ?: 0.0)}," +
            "${if (trip.isSyncedToServer) "si" else "no"}"
        )

        if (points.isEmpty()) return sb.toString()

        sb.appendLine()
        sb.appendLine("# Puntos GPS")
        sb.appendLine("timestamp,lat,lng,velocidad_kmh,heading,precision_metros,sincronizado")
        points.forEach { p ->
            sb.appendLine(
                "${p.timestampIso}," +
                "${p.lat}," +
                "${p.lng}," +
                "${"%.1f".format(p.speedKmh)}," +
                "${"%.1f".format(p.heading)}," +
                "${"%.1f".format(p.accuracyMeters)}," +
                "${if (p.synced) "si" else "no"}"
            )
        }

        return sb.toString()
    }
}
