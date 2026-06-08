# feature:onboarding — Grupo ALICE

**Integrantes:** ALICE (completar con nombres y roles)

## Qué implementamos

Flujo de bienvenida y solicitud de permisos en **5 pasos** antes de que el chofer pueda usar la app:

| Paso | Pantalla | Qué hace |
|------|----------|----------|
| 1 | Welcome | Presentación de la app |
| 2 | Ubicación | Solicita `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` |
| 3 | Notificaciones | Solicita `POST_NOTIFICATIONS` (Android 13+) |
| 4 | Batería | Abre ajustes para desactivar optimización de batería |
| 5 | Listo | Confirma que todo está configurado y redirige al inicio |

Cada paso de permisos tiene un botón **"Omitir"** para que el chofer pueda continuar aunque deniegue algún permiso (offline-first, nunca bloqueamos la operación).

## Arquitectura

```
OnboardingScreen.kt   → UI en Jetpack Compose (5 pasos animados)
OnboardingViewModel.kt → Estado con StateFlow, sin dependencias de Android
```

- El `ViewModel` no tiene dependencias de Android, lo que facilita el testing puro con JUnit.
- El estado es un `data class` inmutable (`OnboardingUiState`).
- Los pasos están definidos en el enum `OnboardingStep`.

## Cómo correr y probar (en aislado)

```bash
./gradlew :feature:onboarding:test
```

## Decisiones técnicas

- **Sin repositorio externo**: el onboarding solo maneja permisos del SO, no necesita `SessionRepository` ni red.
- **Animación entre pasos**: `AnimatedContent` con slide horizontal para una transición natural.
- **Botones ≥ 56dp**: se usa `BigActionButton` del design system y los `TextButton` tienen `height(56.dp)`.
- **Battery optimization**: se abre `ACTION_APPLICATION_DETAILS_SETTINGS` porque `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` requiere permiso especial de Google Play.

## Contratos

- Consume: ningún contrato de `core:domain` (solo permisos del SO)
- Expone: `OnboardingScreen(onFinished: () -> Unit)` para que `:app` navegue al destino siguiente

## Limitaciones / pendientes

- No persiste si el onboarding fue completado (necesitaría `DataStore` en `:core`).
- El resultado de la configuración de batería no es verificable programáticamente sin `PowerManager` (requeriría permiso adicional).
