package com.compensatuviaje.tracker.feature.cloudmirror

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.RemoteMirror
import com.compensatuviaje.tracker.model.Trip
import com.compensatuviaje.tracker.model.TripStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant

class CloudMirrorModule(
    private val firestore: FirebaseFirestore
) : RemoteMirror {

    override suspend fun mirror(trip: Trip): AppResult<Unit> {

        if (trip.status != TripStatus.COMPLETED) {
            return AppResult.Err(
                ErrorKind.VALIDATION,
                "Only completed trips can be mirrored"
            )
        }

        return try {

            val document = mapOf(
                "id" to trip.id,
                "status" to trip.status.name,
                "startedAtIso" to trip.startedAtIso,
                "endedAtIso" to trip.endedAtIso,
                "totalLocalDistanceKm" to trip.totalLocalDistanceKm,
                "serverDistanceKm" to trip.serverDistanceKm,
                "co2Kg" to trip.co2Kg,
                "mirroredAt" to Instant.now().toString()
            )

            firestore
                .collection("trips")
                .document(trip.id)
                .set(document)
                .await()

            AppResult.Ok(Unit)

        } catch (e: Exception) {

            AppResult.Err(
                ErrorKind.UNKNOWN,
                e.message
            )
        }
    }
}