package com.compensatuviaje.tracker.feature.authfirebase

import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.model.Session

/**
 * Contrato para la autenticación en el sistema.
 * Definido localmente en este módulo para respetar la restricción de no tocar core:domain.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<Session>
    suspend fun logout(): AppResult<Unit>
    suspend fun getCurrentSession(): Session?
}
