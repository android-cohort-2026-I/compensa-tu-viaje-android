package com.compensatuviaje.tracker.feature.apiclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Request bodies ────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    @SerialName("driver_id")   val driverId: String,
    @SerialName("pin")         val pin: String,
    @SerialName("license_plate") val licensePlate: String,
)

@Serializable
data class LatLngDto(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
)

@Serializable
data class StartTripRequest(
    @SerialName("started_at")       val startedAt: String,
    @SerialName("start_location")   val startLocation: LatLngDto,
    @SerialName("accuracy_meters")  val accuracyMeters: Double,
)

@Serializable
data class GpsPointDto(
    @SerialName("timestamp")        val timestamp: String,
    @SerialName("lat")              val lat: Double,
    @SerialName("lng")              val lng: Double,
    @SerialName("speed_kmh")        val speedKmh: Double,
    @SerialName("heading")          val heading: Double,
    @SerialName("accuracy_meters")  val accuracyMeters: Double,
)

@Serializable
data class SyncBatchRequest(
    @SerialName("current_local_distance_km") val currentLocalDistanceKm: Double,
    @SerialName("points")                    val points: List<GpsPointDto>,
)

@Serializable
data class EndTripRequest(
    @SerialName("ended_at")                  val endedAt: String,
    @SerialName("end_location")              val endLocation: LatLngDto,
    @SerialName("accuracy_meters")           val accuracyMeters: Double,
    @SerialName("total_local_distance_km")   val totalLocalDistanceKm: Double,
)

// ── Response bodies ───────────────────────────────────────────────────────────

@Serializable
data class TruckDto(
    @SerialName("id")            val id: String,
    @SerialName("license_plate") val licensePlate: String,
    @SerialName("category")      val category: String,
)

@Serializable
data class LoginResponse(
    @SerialName("token")       val token: String,
    @SerialName("driver_name") val driverName: String,
    @SerialName("truck")       val truck: TruckDto,
)

@Serializable
data class StartTripResponse(
    @SerialName("trip_id") val tripId: String,
    @SerialName("status")  val status: String,
)

@Serializable
data class SyncBatchResponse(
    @SerialName("success")             val success: Boolean,
    @SerialName("synced_points_count") val syncedPointsCount: Int,
    @SerialName("message")             val message: String,
)

@Serializable
data class TripSummaryDto(
    @SerialName("server_calculated_distance_km") val serverCalculatedDistanceKm: Double,
    @SerialName("total_co2_kg")                  val totalCo2Kg: Double,
)

@Serializable
data class EndTripResponse(
    @SerialName("trip_id") val tripId: String,
    @SerialName("status")  val status: String,
    @SerialName("summary") val summary: TripSummaryDto,
)
