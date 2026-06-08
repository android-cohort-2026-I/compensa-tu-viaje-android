package com.compensatuviaje.tracker.feature.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class OnboardingScreenTest {

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModel = OnboardingViewModel()
    }

    @Test
    fun `estado inicial es WELCOME y no completo`() {
        val state = viewModel.uiState.value
        assertThat(state.currentStep).isEqualTo(OnboardingStep.WELCOME)
        assertThat(state.isComplete).isFalse()
    }

    @Test
    fun `goToNextStep avanza al siguiente paso`() {
        viewModel.goToNextStep()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LOCATION_PERMISSION)
    }

    @Test
    fun `goToPreviousStep retrocede al paso anterior`() {
        viewModel.goToNextStep() // WELCOME -> LOCATION_PERMISSION
        viewModel.goToNextStep() // LOCATION_PERMISSION -> NOTIFICATION_PERMISSION
        viewModel.goToPreviousStep()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LOCATION_PERMISSION)
    }

    @Test
    fun `goToPreviousStep en el primer paso no cambia el estado`() {
        viewModel.goToPreviousStep()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.WELCOME)
    }

    @Test
    fun `onLocationPermissionResult true registra permiso concedido`() {
        viewModel.onLocationPermissionResult(true)
        assertThat(viewModel.uiState.value.locationGranted).isTrue()
    }

    @Test
    fun `onLocationPermissionResult false registra permiso denegado`() {
        viewModel.onLocationPermissionResult(false)
        assertThat(viewModel.uiState.value.locationGranted).isFalse()
    }

    @Test
    fun `onNotificationPermissionResult registra correctamente`() {
        viewModel.onNotificationPermissionResult(true)
        assertThat(viewModel.uiState.value.notificationGranted).isTrue()
    }

    @Test
    fun `onBatteryOptimizationResult registra correctamente`() {
        viewModel.onBatteryOptimizationResult(true)
        assertThat(viewModel.uiState.value.batteryOptimizationDisabled).isTrue()
    }

    @Test
    fun `skipStep avanza al siguiente paso sin marcar permiso`() {
        viewModel.goToNextStep() // -> LOCATION_PERMISSION
        viewModel.skipStep()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.NOTIFICATION_PERMISSION)
        assertThat(viewModel.uiState.value.locationGranted).isFalse()
    }

    @Test
    fun `completar todos los pasos marca isComplete como true`() {
        repeat(OnboardingStep.entries.size) {
            viewModel.goToNextStep()
        }
        assertThat(viewModel.uiState.value.isComplete).isTrue()
    }

    @Test
    fun `totalSteps es igual al numero de pasos del enum`() {
        assertThat(viewModel.totalSteps).isEqualTo(OnboardingStep.entries.size)
    }

    @Test
    fun `flujo completo con permisos otorgados termina correctamente`() {
        // WELCOME -> next
        viewModel.goToNextStep()
        // Permiso ubicación concedido
        viewModel.onLocationPermissionResult(true)
        viewModel.goToNextStep()
        // Permiso notificaciones concedido
        viewModel.onNotificationPermissionResult(true)
        viewModel.goToNextStep()
        // Batería optimizada
        viewModel.onBatteryOptimizationResult(true)
        viewModel.goToNextStep()
        // READY -> finish
        viewModel.goToNextStep()

        val state = viewModel.uiState.value
        assertThat(state.isComplete).isTrue()
        assertThat(state.locationGranted).isTrue()
        assertThat(state.notificationGranted).isTrue()
        assertThat(state.batteryOptimizationDisabled).isTrue()
    }

    @Test
    fun `flujo completo omitiendo todos los permisos igual termina`() {
        repeat(OnboardingStep.entries.size) {
            viewModel.skipStep()
        }
        assertThat(viewModel.uiState.value.isComplete).isTrue()
    }
}
