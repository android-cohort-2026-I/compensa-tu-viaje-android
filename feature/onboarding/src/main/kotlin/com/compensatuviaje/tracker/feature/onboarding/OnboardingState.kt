package com.compensatuviaje.tracker.feature.onboarding

/**
 * Representa cada uno de los pasos obligatorios del flujo de Onboarding.
 */
sealed interface OnboardingStep {
    data object Splash : OnboardingStep
    data object Explanation : OnboardingStep
    data object GpsPermission : OnboardingStep
    data object BatteryOptimization : OnboardingStep
    data object Success : OnboardingStep
}

/**
 * Estado de la UI para el módulo de Onboarding.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Splash,
    val hasFineLocationPermission: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
)
