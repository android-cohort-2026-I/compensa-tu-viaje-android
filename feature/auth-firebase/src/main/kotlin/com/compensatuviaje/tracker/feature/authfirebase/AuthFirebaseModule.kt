package com.compensatuviaje.tracker.feature.authfirebase

import com.compensatuviaje.tracker.domain.MobileApi

/**
 * Punto de entrada del módulo feature:auth-firebase.
 *
 * En producción, desde :app:
 * ```kotlin
 * val api = AuthFirebaseModule.create(
 *     authProvider = RealFirebaseAuthProvider(FirebaseAuth.getInstance()),
 *     delegate = ApiClientModule.create(tokenProvider = { session?.token }),
 * )
 * ```
 */
object AuthFirebaseModule {

    fun create(
        authProvider: FirebaseAuthProvider,
        delegate: MobileApi,
    ): MobileApi = FirebaseMobileApi(authProvider, delegate)
}
