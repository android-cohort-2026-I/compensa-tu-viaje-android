package com.compensatuviaje.tracker.feature.authfirebase

/**
 * Abstracción sobre Firebase Auth para permitir testing sin google-services.json.
 * En producción se implementa con FirebaseAuth real; en tests con un fake.
 */
interface FirebaseAuthProvider {

    /**
     * Intenta autenticar con email + password.
     * @return el UID del usuario si tiene éxito, null si falla.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): String?

    /**
     * Devuelve el ID token JWT del usuario actual, o null si no hay sesión.
     */
    suspend fun getIdToken(): String?

    /**
     * Cierra la sesión del usuario actual.
     */
    fun signOut()

    /**
     * UID del usuario actualmente autenticado, o null.
     */
    val currentUserUid: String?
}
