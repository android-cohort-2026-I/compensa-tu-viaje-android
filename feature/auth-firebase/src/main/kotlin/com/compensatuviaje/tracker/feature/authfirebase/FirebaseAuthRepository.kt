package com.compensatuviaje.tracker.feature.authfirebase

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.model.Session
import com.compensatuviaje.tracker.model.Truck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun login(email: String, password: String): AppResult<Session> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                // En un escenario real, los datos del camión vendrían de un perfil de usuario o custom claims.
                // Para este MVP, creamos una sesión con datos base.
                val session = Session(
                    token = user.uid, // Usamos el UID como token para Firebase
                    driverName = user.displayName ?: user.email ?: "Chofer Firebase",
                    truck = Truck(
                        id = "FB_TRUCK_01",
                        licensePlate = "FIRE-001",
                        category = "Cistern"
                    )
                )
                AppResult.Ok(session)
            } else {
                AppResult.Err(ErrorKind.UNKNOWN, "Usuario nulo tras autenticación")
            }
        } catch (e: FirebaseAuthException) {
            val kind = when (e.errorCode) {
                "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> ErrorKind.UNAUTHORIZED
                "ERROR_NETWORK_REQUEST_FAILED" -> ErrorKind.NETWORK
                else -> ErrorKind.UNKNOWN
            }
            AppResult.Err(kind, e.localizedMessage)
        } catch (e: Exception) {
            AppResult.Err(ErrorKind.UNKNOWN, e.localizedMessage)
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        return try {
            firebaseAuth.signOut()
            AppResult.Ok(Unit)
        } catch (e: Exception) {
            AppResult.Err(ErrorKind.UNKNOWN, e.localizedMessage)
        }
    }

    override suspend fun getCurrentSession(): Session? {
        val user = firebaseAuth.currentUser ?: return null
        return Session(
            token = user.uid,
            driverName = user.displayName ?: user.email ?: "Chofer Firebase",
            truck = Truck(
                id = "FB_TRUCK_01",
                licensePlate = "FIRE-001",
                category = "Cistern"
            )
        )
    }
}
