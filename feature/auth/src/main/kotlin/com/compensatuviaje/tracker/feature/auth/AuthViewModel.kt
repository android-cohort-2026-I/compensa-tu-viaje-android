package com.compensatuviaje.tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.domain.ErrorKind
import com.compensatuviaje.tracker.domain.MobileApi
import com.compensatuviaje.tracker.domain.SessionRepository
import com.compensatuviaje.tracker.domain.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val mobileApi: MobileApi,
    private val sessionRepository: SessionRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(driverId: String, pin: String, licensePlate: String) {
        if (driverId.isBlank() || pin.isBlank() || licensePlate.isBlank()) {
            _uiState.value = AuthUiState.Error("Por favor, completa todos los campos.")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            when (val result = mobileApi.login(driverId, pin, licensePlate)) {
                is AppResult.Ok -> {
                    //guardar token ((yisus no le muevas))
                    tokenStorage.save(result.value.token)
                    sessionRepository.setSession(result.value)
                    _uiState.value = AuthUiState.Success
                }
                is AppResult.Err -> {
                    val friendlyMessage = when (result.kind) {
                        ErrorKind.NETWORK -> "No hay conexión a internet. Revisa tu red e intenta de nuevo."
                        ErrorKind.UNAUTHORIZED -> "Credenciales incorrectas. Verifica tu ID o PIN."
                        ErrorKind.SERVER -> "Problema en el servidor. Intenta más tarde."
                        else -> "Ocurrió un error inesperado al iniciar sesión."
                    }
                    _uiState.value = AuthUiState.Error(friendlyMessage)
                }
            }
        }
    }
}