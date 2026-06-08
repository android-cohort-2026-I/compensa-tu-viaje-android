package com.compensatuviaje.tracker.feature.authfirebase

import androidx.lifecycle.ViewModel
import com.compensatuviaje.tracker.domain.AppResult
import com.compensatuviaje.tracker.model.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estados de la pantalla de autenticación.
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val session: Session) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthFirebaseViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Scope manual para evitar dependencia de viewModelScope-ktx
    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email y contraseña son obligatorios")
            return
        }

        scope.launch {
            _uiState.value = AuthUiState.Loading
            
            when (val result = authRepository.login(email, password)) {
                is AppResult.Ok -> {
                    _uiState.value = AuthUiState.Success(result.value)
                }
                is AppResult.Err -> {
                    val errorMsg = result.message ?: "Error de autenticación: ${result.kind}"
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            }
        }
    }

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel() // Limpiamos las corrutinas al destruir el ViewModel
    }
}
