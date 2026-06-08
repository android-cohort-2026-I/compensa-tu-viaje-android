package com.compensatuviaje.tracker.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compensatuviaje.tracker.designsystem.BigActionButton
import com.compensatuviaje.tracker.domain.TokenStorage

@Composable
fun OnboardingScreen(
    tokenStorage: TokenStorage = FakeTokenStorage(),
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(tokenStorage)
    )
    val uiState by viewModel.uiState.collectAsState()

    // BONUS: Detección en caliente (Hot detection)
    // Verifica permisos cada vez que el usuario vuelve a la app (ej: desde Ajustes)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Timer inicial solo si estamos en Splash
    LaunchedEffect(Unit) {
        if (uiState.currentStep == OnboardingStep.Splash) {
            viewModel.startSplashTimer(context)
        }
    }

    when (uiState.currentStep) {
        OnboardingStep.Splash -> {
            SplashView()
        }
        OnboardingStep.Explanation -> {
            ExplanationView(onNext = viewModel::onExplanationAccepted)
        }
        OnboardingStep.GpsPermission -> {
            GpsPermissionView(
                hasFine = uiState.hasFineLocationPermission,
                hasBackground = uiState.hasBackgroundLocationPermission,
                onPermissionsResult = viewModel::onGpsPermissionHandled
            )
        }
        OnboardingStep.BatteryOptimization -> {
            BatteryOptimizationView(
                onResult = viewModel::onBatteryOptimizationHandled
            )
        }
        OnboardingStep.Success -> {
            SuccessView(onFinish = onFinish)
        }
    }
}

@Composable
fun SplashView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Compensa Tu Viaje",
                style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ExplanationView(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenido, Compañero", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Para registrar tu ruta de forma automática y calcular tu compensación, necesitamos activar el GPS en todo momento.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        BigActionButton(text = "Entendido, Continuar", onClick = onNext)
    }
}

@Composable
fun GpsPermissionView(
    hasFine: Boolean,
    hasBackground: Boolean,
    onPermissionsResult: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    
    // Launcher para ubicación básica (Fine + Coarse)
    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        onPermissionsResult(granted, hasBackground)
    }

    // Launcher para segundo plano (Requiere Fine previamente concedido en Android 11+)
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionsResult(hasFine, granted)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasFine) Icons.Default.GpsFixed else Icons.Default.GpsOff,
            contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Permiso de Ubicación", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        
        val instructionText = when {
            !hasFine -> "Necesitamos acceso a tu ubicación precisa para iniciar el registro."
            !hasBackground -> "¡Casi listo! Ahora debes seleccionar 'Permitir todo el tiempo' en los ajustes de ubicación para que no se detenga el rastreo al apagar la pantalla."
            else -> "Permisos de ubicación concedidos."
        }
        
        Text(text = instructionText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        
        BigActionButton(
            text = if (!hasFine) "Conceder Ubicación" else "Permitir Siempre",
            onClick = {
                if (!hasFine) {
                    fineLocationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        onPermissionsResult(true, true)
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text("Ir a Ajustes del Sistema")
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun BatteryOptimizationView(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Optimización de Batería", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Para que el viaje no se detenga por error, necesitamos que la app pueda funcionar siempre, incluso con poca batería.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        BigActionButton(
            text = "Configurar Batería",
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                try { context.startActivity(intent) } catch (_: Exception) { }
                onResult(true)
            }
        )
    }
}

@Composable
fun SuccessView(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null, modifier = Modifier.size(100.dp), tint = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("¡Todo Listo!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Has configurado los permisos correctamente. Ya puedes empezar a registrar tus rutas.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        BigActionButton(text = "Comenzar", onClick = onFinish)
    }
}
