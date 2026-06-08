package com.compensatuviaje.tracker.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.compensatuviaje.tracker.domain.TokenStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun checkPermissions(context: Context) {
        val fine = OnboardingPermissionChecker.hasFineLocation(context)
        val background = OnboardingPermissionChecker.hasBackgroundLocation(context)
        val battery = OnboardingPermissionChecker.isBatteryOptimizationIgnored(context)
        
        _uiState.update { it.copy(
            hasFineLocationPermission = fine,
            hasBackgroundLocationPermission = background,
            isBatteryOptimizationIgnored = battery
        ) }

        if (OnboardingPermissionChecker.allPermissionsGranted(context)) {
            _uiState.update { it.copy(currentStep = OnboardingStep.Success) }
        }
    }

    fun startSplashTimer(context: Context? = null) {
        viewModelScope.launch {
            delay(1500)
            val allGranted = context?.let { OnboardingPermissionChecker.allPermissionsGranted(it) } ?: false
            if (allGranted) {
                _uiState.update { it.copy(currentStep = OnboardingStep.Success) }
            } else {
                _uiState.update { it.copy(currentStep = OnboardingStep.Explanation) }
            }
        }
    }

    fun onExplanationAccepted() {
        _uiState.update { it.copy(currentStep = OnboardingStep.GpsPermission) }
    }

    fun onGpsPermissionHandled(fineGranted: Boolean, backgroundGranted: Boolean) {
        _uiState.update { 
            it.copy(
                hasFineLocationPermission = fineGranted,
                hasBackgroundLocationPermission = backgroundGranted
            ) 
        }
        
        if (fineGranted && backgroundGranted) {
            _uiState.update { it.copy(currentStep = OnboardingStep.BatteryOptimization) }
        }
    }

    fun onBatteryOptimizationHandled(ignored: Boolean) {
        _uiState.update { 
            it.copy(
                isBatteryOptimizationIgnored = ignored,
                currentStep = OnboardingStep.Success
            ) 
        }
    }
}
