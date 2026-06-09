package com.compensatuviaje.tracker.feature.auth


data class AuthUiState(
    val driverId: String = "",       // Patente/ID
    val licensePlate: String = "",   // Lo que escribe en la patente del camión
    val pin: String = "",            // PIN
    val isLoading: Boolean = false,  // Cargando
    val errorMessage: String? = null,// Credenciales inválidas
    val isLoginSuccess: Boolean = false // Saber cuando navegar a Home
)