package com.compensatuviaje.tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    private val tokenStorage: TokenStorage,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onDriverIdChange(newId: String) {
        _uiState.value = _uiState.value.copy(driverId = newId, errorMessage = null)
    }

    fun onLicensePlateChange(newPlate: String) {
        _uiState.value = _uiState.value.copy(licensePlate = newPlate, errorMessage = null)
    }

    fun onPinChange(newPin: String) {
        if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(pin = newPin, errorMessage = null)
        }
    }

    fun onLoginClick() {
        val state = _uiState.value

        // Validación básica de la UI
        if (state.driverId.isBlank() || state.licensePlate.isBlank() || state.pin.length != 4) {
            _uiState.value = state.copy(errorMessage = "Complete todos los campos correctamente")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Consumimos el contrato del API
            when (val result = mobileApi.login(state.driverId, state.pin, state.licensePlate)) {
                is AppResult.Ok -> {
                    // Éxito: Guardamos token y sesión
                    tokenStorage.save(result.value.token)
                    sessionRepository.setSession(result.value)
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccess = true)
                }
                is AppResult.Err -> {
                    // Error: Traducimos a lenguaje simple para el chofer
                    val message = when (result.kind) {
                        ErrorKind.NETWORK -> "Sin conexión a internet"
                        ErrorKind.UNAUTHORIZED -> "Credenciales inválidas"
                        else -> "Ocurrió un error inesperado"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
                }
            }
        }
    }
}

class AuthViewModelFactory(
    private val mobileApi: MobileApi,
    private val tokenStorage: TokenStorage,
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(mobileApi, tokenStorage, sessionRepository) as T
    }
}
