package com.compensatuviaje.tracker.feature.onboarding

import com.compensatuviaje.tracker.domain.TokenStorage

/**
 * Implementación de prueba para permitir que el módulo compile de forma aislada
 * y no rompa la navegación principal si no se inyecta una real.
 */
class FakeTokenStorage : TokenStorage {
    private var token: String? = null
    override fun save(token: String) { this.token = token }
    override fun get(): String? = token
    override fun clear() { token = null }
}
