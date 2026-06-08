package com.compensatuviaje.tracker.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTokenStorage: FakeTokenStorage
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTokenStorage = FakeTokenStorage()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cuando inicia, el primer estado debe ser Splash`() = runTest {
        viewModel = OnboardingViewModel(fakeTokenStorage)
        assertEquals(OnboardingStep.Splash, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `despues del tiempo de splash, debe pasar a Explanation`() = runTest {
        viewModel = OnboardingViewModel(fakeTokenStorage)
        viewModel.startSplashTimer(null) // Lanzamos manualmente con contexto nulo
        
        testDispatcher.scheduler.advanceTimeBy(1600)
        
        assertEquals(OnboardingStep.Explanation, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `al aceptar la explicacion, debe pasar a GpsPermission`() = runTest {
        viewModel = OnboardingViewModel(fakeTokenStorage)
        viewModel.startSplashTimer(null)
        testDispatcher.scheduler.advanceTimeBy(1600)
        
        viewModel.onExplanationAccepted()
        
        assertEquals(OnboardingStep.GpsPermission, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `en el paso de GPS, si solo se concede Fine, no debe avanzar de pantalla`() = runTest {
        viewModel = OnboardingViewModel(fakeTokenStorage)
        viewModel.startSplashTimer(null)
        testDispatcher.scheduler.advanceTimeBy(1600)
        viewModel.onExplanationAccepted()
        
        viewModel.onGpsPermissionHandled(fineGranted = true, backgroundGranted = false)
        
        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.GpsPermission, state.currentStep)
        assertEquals(true, state.hasFineLocationPermission)
        assertEquals(false, state.hasBackgroundLocationPermission)
    }

    @Test
    fun `en el paso de GPS, si se conceden ambos, debe pasar a BatteryOptimization`() = runTest {
        viewModel = OnboardingViewModel(fakeTokenStorage)
        viewModel.startSplashTimer(null)
        testDispatcher.scheduler.advanceTimeBy(1600)
        viewModel.onExplanationAccepted()
        
        viewModel.onGpsPermissionHandled(fineGranted = true, backgroundGranted = true)
        
        assertEquals(OnboardingStep.BatteryOptimization, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `al finalizar optimizacion de bateria, debe pasar a Success`() = runTest {
        viewModel = OnboardingViewModel(fakeTokenStorage)
        viewModel.startSplashTimer(null)
        testDispatcher.scheduler.advanceTimeBy(1600)
        viewModel.onExplanationAccepted()
        viewModel.onGpsPermissionHandled(fineGranted = true, backgroundGranted = true)
        
        viewModel.onBatteryOptimizationHandled(ignored = true)
        
        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.Success, state.currentStep)
        assertEquals(true, state.isBatteryOptimizationIgnored)
    }
}
