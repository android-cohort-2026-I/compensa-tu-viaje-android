package com.compensatuviaje.tracker.feature.authfirebase

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.TripSummary
import com.compensatuviaje.tracker.model.GpsPoint
import com.compensatuviaje.tracker.model.LatLng
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck

/**
 * Implementación de [MobileApi] que usa Firebase Auth para el login
 * y delega las operaciones de viaje a otro [MobileApi] (el api-client REST).
 *
 * Flujo:
 * 1. login() → Firebase signIn con email derivado del driverId → obtiene JWT de Firebase
 * 2. startTrip / syncBatch / endTrip → delegan al [delegate] REST con el JWT de Firebase
 *
 * @param authProvider  Abstracción de Firebase Auth (inyectable, testeable).
 * @param delegate      MobileApi REST que ejecuta las operaciones de viaje.
 *                      En producción es RetrofitMobileApi; en tests es FakeMobileApi.
 */
class FirebaseMobileApi(
    private val authProvider: FirebaseAuthProvider,
    private val delegate: MobileApi,
) : MobileApi {

    // ── login ─────────────────────────────────────────────────────────────────

    override suspend fun login(
        driverId: String,
        pin: String,
        licensePlate: String,
    ): AppResult<Session> {
        // Derivamos un email de Firebase a partir del driverId y la patente
        val email = "$driverId@compensatuviaje.com"

        val uid = authProvider.signInWithEmailAndPassword(email, pin)
            ?: return AppResult.Err(ErrorKind.UNAUTHORIZED, "Firebase auth falló para $driverId")

        // Obtenemos el ID token de Firebase como JWT
        val token = authProvider.getIdToken()
            ?: return AppResult.Err(ErrorKind.UNAUTHORIZED, "No se pudo obtener el ID token de Firebase")

        // Construimos la sesión con el token de Firebase
        val session = Session(
            token = token,
            driverName = driverId,
            truck = Truck(
                id = uid,
                licensePlate = licensePlate,
                category = "unknown",
            ),
        )
        return AppResult.Ok(session)
    }

    // ── delegación al REST api-client ─────────────────────────────────────────

    override suspend fun startTrip(
        startedAtIso: String,
        start: LatLng,
        accuracyMeters: Double,
    ): AppResult<String> = delegate.startTrip(startedAtIso, start, accuracyMeters)

    override suspend fun syncBatch(
        tripId: String,
        currentLocalDistanceKm: Double,
        points: List<GpsPoint>,
    ): AppResult<Int> = delegate.syncBatch(tripId, currentLocalDistanceKm, points)

    override suspend fun endTrip(
        tripId: String,
        endedAtIso: String,
        end: LatLng,
        accuracyMeters: Double,
        totalLocalDistanceKm: Double,
    ): AppResult<TripSummary> = delegate.endTrip(tripId, endedAtIso, end, accuracyMeters, totalLocalDistanceKm)
}
