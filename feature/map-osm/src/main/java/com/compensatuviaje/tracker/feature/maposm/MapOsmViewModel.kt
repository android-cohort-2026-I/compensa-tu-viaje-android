package com.compensatuviaje.tracker.feature.maposm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.model.GpsPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapOsmUiState(
    val puntos: List<GpsPoint> = emptyList(),
    val estaCargando: Boolean = false,
    val error: String? = null,
)

class MapOsmViewModel(
    private val gpsPointRepository: GpsPointRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapOsmUiState())
    val uiState: StateFlow<MapOsmUiState> = _uiState.asStateFlow()

    fun cargarPuntos(tripId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(estaCargando = true, error = null) }
            gpsPointRepository
                .pointsForTrip(tripId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            error = "No se pudo cargar el mapa"
                        )
                    }
                }
                .collect { puntos ->
                    _uiState.update {
                        it.copy(puntos = puntos, estaCargando = false)
                    }
                }
        }
    }

    // Factory simple sin Hilt
    object Factory : ViewModelProvider.Factory {
        // En producción se inyecta la implementación real desde :feature:database
        // En tests se usa MapOsmViewModel(fakeRepo) directamente
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            error(
                "Usa MapOsmViewModel(gpsPointRepository) directamente en tests. " +
                "En producción, configura la Factory con el repositorio real."
            )
        }
    }
}
