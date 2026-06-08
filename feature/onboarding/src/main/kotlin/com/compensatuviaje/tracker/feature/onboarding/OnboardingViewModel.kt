package com.compensatuviaje.tracker.feature.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Representa cada paso del flujo de onboarding
enum class OnboardingStep(val index: Int, val title: String, val description: String) {
    WELCOME(
        0,
        "Bienvenido a CompensaTuViaje",
        "Registra tus viajes, mide distancias y contribuye a compensar tu huella de carbono."
    ),
    LOCATION_PERMISSION(
        1,
        "Permiso de ubicación",
        "Necesitamos acceso a tu ubicación en todo momento para registrar el recorrido del viaje, incluso con la app en segundo plano."
    ),
    NOTIFICATION_PERMISSION(
        2,
        "Permiso de notificaciones",
        "Te avisaremos cuando un viaje esté sincronizándose o si hay algún problema con la conexión."
    ),
    BATTERY_OPTIMIZATION(
        3,
        "Optimización de batería",
        "Para que el rastreo no se interrumpa, desactiva la optimización de batería para esta app."
    ),
    READY(
        4,
        "¡Todo listo!",
        "Ya puedes iniciar tu primer viaje. El registro se realiza de forma automática y funciona sin conexión."
    )
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val locationGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val isComplete: Boolean = false,
)

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationGranted = granted) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationGranted = granted) }
    }

    fun onBatteryOptimizationResult(disabled: Boolean) {
        _uiState.update { it.copy(batteryOptimizationDisabled = disabled) }
    }

    fun goToNextStep() {
        val steps = OnboardingStep.entries
        val current = _uiState.value.currentStep
        val nextIndex = current.index + 1
        if (nextIndex < steps.size) {
            _uiState.update { it.copy(currentStep = steps[nextIndex]) }
        } else {
            _uiState.update { it.copy(isComplete = true) }
        }
    }

    fun goToPreviousStep() {
        val steps = OnboardingStep.entries
        val current = _uiState.value.currentStep
        val prevIndex = current.index - 1
        if (prevIndex >= 0) {
            _uiState.update { it.copy(currentStep = steps[prevIndex]) }
        }
    }

    // Permite saltar permisos (el usuario puede negar y continuar igual)
    fun skipStep() {
        goToNextStep()
    }

    val totalSteps: Int = OnboardingStep.entries.size
}
