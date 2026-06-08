# feature:trip

**Grupo:** [BIOSECURE]
**Integrantes:** Nicolas Carrillo (ViewModel + lógica offline-first), Marco Figueroa (UI Compose + navegación)
**Módulo:** `feature:trip`

## Qué implementamos

Flujo completo de gestión de viajes para el conductor:

| Estado UI | Descripción |
|---|---|
| `Idle` | Pantalla de inicio con ícono GPS y botón "Iniciar Ruta" |
| `Active` | Contador HH:MM:SS en vivo, km acumulados, ícono de sincronización |
| `ConfirmingEnd` | Diálogo de confirmación antes de cerrar el viaje |
| `Processing` | Pantalla de espera mientras se comunica con el servidor |
| `Summary` | Resumen final: distancia (servidor o provisional), duración, patente |
| `Error` | Mensaje de error en español sin códigos HTTP ni stack traces |

**Archivos creados:**

- `TripUiState.kt` — sealed class con los 6 estados
- `TripViewModel.kt` — MVVM con corrutinas, offline-first, timer, observers
- `TripScreen.kt` — Jetpack Compose, Material 3, BackHandler para interceptar retroceso
- `TripNavigation.kt` — extensión `NavGraphBuilder.tripGraph(navController)`
- `TripViewModelTest.kt` — 5 tests JUnit 4 + coroutines-test

## Cómo correr en aislado

```bash
./gradlew :feature:trip:test
```

Para ver todos los resultados:

```bash
./gradlew :feature:trip:test --info
```

## Contratos que consume

De `core:domain`:

| Interfaz | Uso |
|---|---|
| `TripRepository` | Crear, actualizar y consultar viajes en Room |
| `LocationTracker` | Recibir puntos GPS y estado de tracking |
| `DistanceCalculator` | Calcular distancia acumulada (Haversine) |
| `MobileApi` | Registrar inicio/fin de viaje en el servidor |
| `SyncManager` | Programar envío de puntos por WorkManager |
| `ConnectivityMonitor` | Observar estado de red en tiempo real |
| `SessionRepository` | Obtener patente del camión y manejar cierre de sesión |

De `core:common`: `formatKm()`, `formatDuration()`, `Iso8601.now()`, `Iso8601.of()`  
De `core:designsystem`: `BigActionButton`, `GpsStatusIcon`, `SyncStatusIcon`, `LoadingState`, `ErrorState`

## Contratos que expone

- `TripScreen(navController, vm)` — composable principal, sin args obligatorios
- `NavGraphBuilder.tripGraph(navController)` — registro de rutas en el NavHost del app

## Decisiones técnicas

**Offline-first en `startTrip()`:** el estado cambia a `Active` síncronamente antes de lanzar la corrutina que llama al servidor. El viaje se graba en Room primero; la respuesta del servidor es best-effort.

**Manejo de fallo en `confirmEndTrip()`:** si `MobileApi.endTrip` falla con error de red, el viaje queda en `TripStatus.PENDING_END` en Room para que un WorkManager lo reintente. La UI muestra un resumen provisional con distancia local.

**401 → sesión inválida:** cualquier respuesta `UNAUTHORIZED` llama a `SessionRepository.logout()` y transiciona a `TripUiState.Error` con mensaje en español, sin exponer códigos HTTP.

**Timer:** `startLiveTimer()` corre en `viewModelScope`, actualiza `elapsedSeconds` cada segundo. Se pausa automáticamente en `ConfirmingEnd` (el `update {}` ignora estados que no sean `Active`) y se cancela en `confirmEndTrip()`.

**Fakes locales en tests:** `FakeSessionRepository`, `FakeDistanceCalculator` y `FakeSyncManager` se definen dentro de `TripViewModelTest.kt` porque no están en `core:testing`. Los fakes de `FakeMobileApi`, `FakeTripRepository`, `FakeLocationTracker` y `FakeConnectivityMonitor` vienen de `core:testing`.

## Limitaciones conocidas

- `viewModel()` en `TripScreen` requiere un `ViewModelProvider.Factory` registrado en el módulo `:app`. Sin inyección de dependencias (Hilt/Koin), la pantalla no instancia el ViewModel en producción sin ese factory.
- `updateGpsAvailable()` se llama desde `LaunchedEffect` con el estado del `LocationManager` al entrar a la pantalla, pero no reacciona en tiempo real a cambios de GPS mientras la app está abierta (necesitaría un `BroadcastReceiver` o un flow del sistema).
- `isSyncing` en `TripUiState.Active` siempre es `false` en esta implementación; requiere que `SyncManager` exponga un `Flow<Boolean>` de estado de sincronización activa.
