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
import retrofit2.Response

/**
 * Implementación real de [MobileApi] que habla con el servidor REST usando Retrofit.
 *
 * @param baseUrl        URL base del servidor (por defecto la URL de producción mock).
 * @param tokenProvider  Lambda que devuelve el JWT actual (o null si no hay sesión).
 */
class RetrofitMobileApi(
    baseUrl: String,
    tokenProvider: () -> String? = { null },
) : MobileApi {

    private val service: RetrofitApiService = buildRetrofit(baseUrl, tokenProvider)
        .create(RetrofitApiService::class.java)

    // ── login ─────────────────────────────────────────────────────────────────

    override suspend fun login(
        driverId: String,
        pin: String,
        licensePlate: String,
    ): AppResult<Session> = safeCall(
        call = { service.login(LoginRequest(driverId, pin, licensePlate)) },
        map = { body ->
            Session(
                token = body.token,
                driverName = body.driverName,
                truck = Truck(
                    id = body.truck.id,
                    licensePlate = body.truck.licensePlate,
                    category = body.truck.category,
                ),
            )
        },
    )

    // ── startTrip ─────────────────────────────────────────────────────────────

    override suspend fun startTrip(
        startedAtIso: String,
        start: LatLng,
        accuracyMeters: Double,
    ): AppResult<String> = safeCall(
        call = {
            service.startTrip(
                StartTripRequest(
                    startedAt = startedAtIso,
                    startLocation = LatLngDto(start.lat, start.lng),
                    accuracyMeters = accuracyMeters,
                )
            )
        },
        map = { body -> body.tripId },
    )

    // ── syncBatch ─────────────────────────────────────────────────────────────

    override suspend fun syncBatch(
        tripId: String,
        currentLocalDistanceKm: Double,
        points: List<GpsPoint>,
    ): AppResult<Int> = safeCall(
        call = {
            service.syncBatch(
                tripId = tripId,
                body = SyncBatchRequest(
                    currentLocalDistanceKm = currentLocalDistanceKm,
                    points = points.map { p ->
                        GpsPointDto(
                            timestamp = p.timestampIso,
                            lat = p.lat,
                            lng = p.lng,
                            speedKmh = p.speedKmh,
                            heading = p.heading,
                            accuracyMeters = p.accuracyMeters,
                        )
                    },
                )
            )
        },
        map = { body -> body.syncedPointsCount },
    )

    // ── endTrip ───────────────────────────────────────────────────────────────

    override suspend fun endTrip(
        tripId: String,
        endedAtIso: String,
        end: LatLng,
        accuracyMeters: Double,
        totalLocalDistanceKm: Double,
    ): AppResult<TripSummary> = safeCall(
        call = {
            service.endTrip(
                tripId = tripId,
                body = EndTripRequest(
                    endedAt = endedAtIso,
                    endLocation = LatLngDto(end.lat, end.lng),
                    accuracyMeters = accuracyMeters,
                    totalLocalDistanceKm = totalLocalDistanceKm,
                )
            )
        },
        map = { body ->
            TripSummary(
                serverDistanceKm = body.summary.serverCalculatedDistanceKm,
                co2Kg = body.summary.totalCo2Kg,
            )
        },
    )
}

// ── helpers ───────────────────────────────────────────────────────────────────

/**
 * Ejecuta [call] de forma segura y mapea el resultado a [AppResult].
 * Convierte los códigos HTTP a [ErrorKind] correspondiente.
 */
private inline fun <R, T> safeCall(
    call: () -> Response<R>,
    map: (R) -> T,
): AppResult<T> = try {
    val response = call()
    val body = response.body()
    when {
        response.isSuccessful && body != null -> AppResult.Ok(map(body))
        response.code() == 401 -> AppResult.Err(ErrorKind.UNAUTHORIZED, "No autorizado")
        response.code() == 404 -> AppResult.Err(ErrorKind.NOT_FOUND, "Recurso no encontrado")
        response.code() == 409 -> AppResult.Err(ErrorKind.CONFLICT, "Conflicto en el servidor")
        response.code() == 422 -> AppResult.Err(ErrorKind.VALIDATION, "Datos inválidos")
        response.code() in 500..599 -> AppResult.Err(ErrorKind.SERVER, "Error del servidor")
        else -> AppResult.Err(ErrorKind.UNKNOWN, "Error desconocido: ${response.code()}")
    }
} catch (e: Exception) {
    AppResult.Err(ErrorKind.NETWORK, e.message)
}
