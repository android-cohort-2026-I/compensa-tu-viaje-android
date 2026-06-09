package com.compensatuviaje.tracker.feature.authfirebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Implementación real que delega en [FirebaseAuth].
 * Requiere google-services.json en el módulo :app (no en este módulo).
 * Se instancia solo en producción desde el módulo :app.
 */
class RealFirebaseAuthProvider(
    private val firebaseAuth: FirebaseAuth,
) : FirebaseAuthProvider {

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): String? = try {
        val result = firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()
        result.user?.uid
    } catch (e: Exception) {
        null
    }

    override suspend fun getIdToken(): String? = try {
        firebaseAuth.currentUser
            ?.getIdToken(/* forceRefresh = */ false)
            ?.await()
            ?.token
    } catch (e: Exception) {
        null
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override val currentUserUid: String?
        get() = firebaseAuth.currentUser?.uid
}
