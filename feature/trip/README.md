# feature:trip — AuthCares

**Integrantes:** Soto Ayelen (Orquestación ViewModel), Choque Karla (UI 5 vistas Compose), Cori Johs (Testing y Fakes), Torres Willy (Gestión de Estados y Documentación)

## Qué implementamos
Flujo completo de la jornada del chofer orquestado por TripViewModel. Se implementaron 5 etapas usando un enum TripStage (IDLE, IN_PROGRESS, CONFIRM_END, PROCESSING, SUMMARY). Garantiza un inicio de viaje offline-first y una finalización con confirmación explícita estricta.

## Cómo correr y probar (en aislado)
```
./gradlew :feature:trip:test
```

## Decisiones técnicas
- **Offline-first:** Al iniciar, se genera un ID temporal (`local_`), se guarda en Room y se navega instantáneamente sin esperar al servidor.
- **Confirmación estricta:** El estado `CONFIRM_END` obliga al usuario a validar la finalización del viaje antes de procesar.
- **Recuperación de viaje:** En el bloque `init` del ViewModel se observa `activeTrip()`, permitiendo volver al estado `IN_PROGRESS` si la app se reinicia con un viaje en curso.
- Cumplimiento de UX: Todos los botones principales de acción usan `Modifier.height(56.dp)`.

## Contratos
- Consume: `TripRepository`, `LocationTracker`, `DistanceCalculator`, `SyncManager`, `ConnectivityMonitor`, `MobileApi` (`:core:domain`)
- Expone: `TripScreen` (Composable UI), `TripViewModel`, `TripUiState`, `TripStage`

## Limitaciones / pendientes
- El cronómetro en vivo muestra un placeholder "00:00:00", falta lógica de tiempo transcurrido.
- Flujo `pending_end` estructurado, pero falta lógica de reintento automático si el cierre falla por red.
