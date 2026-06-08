package com.compensatuviaje.tracker.feature.apiclient

import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.network.MOCK_BASE_URL

/**
 * Punto de entrada del módulo feature:api-client.
 * Construye y expone la implementación real de [MobileApi].
 */
object ApiClientModule {

    /**
     * Crea un [MobileApi] listo para usar.
     *
     * @param baseUrl       URL base del servidor. Por defecto usa la URL mock del proyecto.
     * @param tokenProvider Lambda que devuelve el JWT almacenado (o null si no hay sesión).
     */
    fun create(
        baseUrl: String = MOCK_BASE_URL,
        tokenProvider: () -> String? = { null },
    ): MobileApi = RetrofitMobileApi(baseUrl, tokenProvider)
}
