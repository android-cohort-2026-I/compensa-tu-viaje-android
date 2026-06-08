package com.compensatuviaje.tracker.feature.locationservice

import android.content.Context
import com.compensatuviaje.tracker.domain.GpsPointRepository

/**
 * Proveedor de dependencias simple para el ForegroundService.
 * Permite inyectar el repositorio real en producción y un fake en tests.
 */
object ServiceLocator {

    private var gpsPointRepositoryOverride: GpsPointRepository? = null

    /**
     * En tests: llama a setGpsPointRepository(fake) antes de iniciar el servicio.
     * En producción: la implementación viene de :feature:database.
     */
    fun setGpsPointRepository(repo: GpsPointRepository) {
        gpsPointRepositoryOverride = repo
    }

    fun getGpsPointRepository(context: Context): GpsPointRepository {
        return gpsPointRepositoryOverride
            ?: error(
                "GpsPointRepository no configurado. " +
                "En producción, configúralo desde Application. " +
                "En tests, usa ServiceLocator.setGpsPointRepository(fake)."
            )
    }

    fun reset() {
        gpsPointRepositoryOverride = null
    }
}
