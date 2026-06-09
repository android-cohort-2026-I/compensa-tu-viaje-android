package com.compensatuviaje.tracker.feature.apiclient

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.TripSummary
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck
import com.compensatuviaje.tracker.network.buildRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// ─────────────────────────────────────────────
// DTOs de REQUEST
// ─────────────────────────────────────────────

@Serializable
data class LoginRequest(
    @SerialName("driver_id") val driverId: String,
    @SerialName("pin") val pin: String,
    @SerialName("license_plate") val licensePlate: String,
)

@Serializable
data class StartTripRequest(
    @SerialName("started_at") val startedAt: String,
    @SerialName("start_lat") val startLat: Double,
    @SerialName("start_lng") val startLng: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Double,
)

@Serializable
data class GpsPointDto(
    @SerialName("timestamp") val timestamp: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("speed_kmh") val speedKmh: Double,
    @SerialName("heading") val heading: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Double,
)

@Serializable
data class SyncBatchRequest(
    @SerialName("current_local_distance_km") val currentLocalDistanceKm: Double,
    @SerialName("points") val points: List<GpsPointDto>,
)

@Serializable
data class EndTripRequest(
    @SerialName("ended_at") val endedAt: String,
    @SerialName("end_lat") val endLat: Double,
    @SerialName("end_lng") val endLng: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Double,
    @SerialName("total_local_distance_km") val totalLocalDistanceKm: Double,
)

// ─────────────────────────────────────────────
// DTOs de RESPONSE
// ─────────────────────────────────────────────

@Serializable
data class TruckDto(
    @SerialName("id") val id: String,
    @SerialName("license_plate") val licensePlate: String,
    @SerialName("category") val category: String,
)

@Serializable
data class LoginResponse(
    @SerialName("token") val token: String,
    @SerialName("driver_name") val driverName: String,
    @SerialName("truck") val truck: TruckDto,
)

@Serializable
data class StartTripResponse(
    @SerialName("trip_id") val tripId: String,
    @SerialName("status") val status: String,
)

@Serializable
data class SyncBatchResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("synced_points_count") val syncedPointsCount: Int,
    @SerialName("message") val message: String? = null,
)

@Serializable
data class TripSummaryDto(
    @SerialName("server_calculated_distance_km") val serverDistanceKm: Double,
    @SerialName("total_co2_kg") val co2Kg: Double,
)

@Serializable
data class EndTripResponse(
    @SerialName("trip_id") val tripId: String,
    @SerialName("status") val status: String,
    @SerialName("summary") val summary: TripSummaryDto,
)

// ─────────────────────────────────────────────
// Interfaz Retrofit (servicio HTTP puro)
// ─────────────────────────────────────────────

interface TrackerApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("trips/start")
    suspend fun startTrip(@Body body: StartTripRequest): Response<StartTripResponse>

    @POST("trips/{trip_id}/sync")
    suspend fun syncBatch(
        @Path("trip_id") tripId: String,
        @Body body: SyncBatchRequest,
    ): Response<SyncBatchResponse>

    @POST("trips/{trip_id}/end")
    suspend fun endTrip(
        @Path("trip_id") tripId: String,
        @Body body: EndTripRequest,
    ): Response<EndTripResponse>
}

// ─────────────────────────────────────────────
// Implementación de MobileApi
// ─────────────────────────────────────────────

class RetrofitMobileApi(
    private val service: TrackerApiService,
) : MobileApi {

    override suspend fun login(
        driverId: String,
        pin: String,
        licensePlate: String,
    ): AppResult<Session> = safeCall {
        val response = service.login(LoginRequest(driverId, pin, licensePlate))
        mapResponse(response) { dto ->
            Session(
                token = dto.token,
                driverName = dto.driverName,
                truck = Truck(
                    id = dto.truck.id,
                    licensePlate = dto.truck.licensePlate,
                    category = dto.truck.category,
                ),
            )
        }
    }

    override suspend fun startTrip(
        startedAtIso: String,
        start: LatLng,
        accuracyMeters: Double,
    ): AppResult<String> = safeCall {
        val response = service.startTrip(
            StartTripRequest(startedAtIso, start.lat, start.lng, accuracyMeters)
        )
        mapResponse(response) { it.tripId }
    }

    override suspend fun syncBatch(
        tripId: String,
        currentLocalDistanceKm: Double,
        points: List<GpsPoint>,
    ): AppResult<Int> = safeCall {
        val dtos = points.map { p ->
            GpsPointDto(p.timestampIso, p.lat, p.lng, p.speedKmh, p.heading, p.accuracyMeters)
        }
        val response = service.syncBatch(
            tripId,
            SyncBatchRequest(currentLocalDistanceKm, dtos),
        )
        mapResponse(response) { it.syncedPointsCount }
    }

    override suspend fun endTrip(
        tripId: String,
        endedAtIso: String,
        end: LatLng,
        accuracyMeters: Double,
        totalLocalDistanceKm: Double,
    ): AppResult<TripSummary> = safeCall {
        val response = service.endTrip(
            tripId,
            EndTripRequest(endedAtIso, end.lat, end.lng, accuracyMeters, totalLocalDistanceKm),
        )
        mapResponse(response) { dto ->
            TripSummary(
                serverDistanceKm = dto.summary.serverDistanceKm,
                co2Kg = dto.summary.co2Kg,
            )
        }
    }

    // ─── Helpers ───────────────────────────────

    private fun <T, R> mapResponse(
        response: Response<T>,
        transform: (T) -> R,
    ): AppResult<R> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    AppResult.Ok(transform(body))
                } else {
                    AppResult.Err(ErrorKind.UNKNOWN, "Respuesta vacía del servidor")
                }
            }
            response.code() == 401 -> AppResult.Err(ErrorKind.UNAUTHORIZED, "Sesión expirada")
            response.code() == 404 -> AppResult.Err(ErrorKind.NOT_FOUND, "Recurso no encontrado")
            response.code() == 409 -> AppResult.Err(ErrorKind.CONFLICT, "Ya existe un viaje activo")
            response.code() == 422 -> AppResult.Err(ErrorKind.VALIDATION, "Datos inválidos")
            response.code() in 500..599 -> AppResult.Err(ErrorKind.SERVER, "Error del servidor")
            else -> AppResult.Err(ErrorKind.UNKNOWN, "Error desconocido: ${response.code()}")
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> AppResult<T>): AppResult<T> =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                AppResult.Err(ErrorKind.NETWORK, e.message)
            }
        }
}

// ─────────────────────────────────────────────
// Factory / Entry point del módulo
// ─────────────────────────────────────────────

/**
 * Punto de entrada del módulo api-client.
 * Construye una instancia de [MobileApi] lista para usar.
 *
 * @param baseUrl URL base del API (por defecto la URL mock del contrato).
 * @param tokenProvider Lambda que devuelve el JWT actual (null si no hay sesión).
 */
class ApiClientModule(
    baseUrl: String = "https://api.mock.compensatuviaje.com/v1/mobile/",
    tokenProvider: () -> String? = { null },
) {
    val mobileApi: MobileApi = RetrofitMobileApi(
        service = buildRetrofit(baseUrl, tokenProvider)
            .create(TrackerApiService::class.java),
    )
}
