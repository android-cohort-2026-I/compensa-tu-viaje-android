package com.compensatuviaje.tracker.feature.authfirebase

/**
 * Fake de [FirebaseAuthProvider] para tests unitarios.
 * No requiere google-services.json ni conexión a Firebase.
 */
class FakeFirebaseAuthProvider(
    private val shouldSucceed: Boolean = true,
    private val fakeUid: String = "fake-uid-123",
    private val fakeToken: String = "eyJfakeFirebaseToken",
) : FirebaseAuthProvider {

    var signInCallCount = 0
    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): String? {
        signInCallCount++
        lastEmail = email
        lastPassword = password
        return if (shouldSucceed) fakeUid else null
    }

    override suspend fun getIdToken(): String? =
        if (shouldSucceed) fakeToken else null

    override fun signOut() {}

    override val currentUserUid: String?
        get() = if (shouldSucceed) fakeUid else null
}
