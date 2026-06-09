# feature:sync-worker — Grupo DevPulse

**Integrantes:** Yuri (Worker de sincronización batch - T1 Crítico)

## Qué implementamos
- **SyncManagerImpl**: Orquestación y planificación de tareas en segundo plano usando `WorkManager` (sincronizaciones periódicas en lotes y cierres de viaje).
- **SyncWorkerImpl**: `CoroutineWorker` que gestiona el envío en lote (batch) de puntos GPS acumulados en Room hacia el servidor:
  - Valida el estado activo del viaje y la conectividad del dispositivo.
  - Envía la telemetría acumulada (GPS y distancia local calculada) de una sola vez a `/sync` (HTTP POST).
  - Al completar con éxito (HTTP 200), marca de forma atómica en Room los puntos como sincronizados.
  - Maneja y reintenta el cierre automático de viajes pendientes (`PENDING_END`) llamando a `/end`.
- **SyncWorkerModule**: Registro manual de dependencias inyectadas para facilitar el desacoplamiento sin requerir Hilt.

## Cómo correr y probar (en aislado)
Para ejecutar todas las pruebas unitarias del módulo:
```powershell
.\gradlew :feature:sync-worker:testDebugUnitTest
```

## Decisiones técnicas
- **Inyección de Dependencias Manual**: Dado que el proyecto no incluye un framework de DI preconfigurado, implementamos un patrón Service Locator a través del object `SyncWorkerModule` para proveer repositorios y API de forma desacoplada y facilitar la inyección de fakes en los tests.
- **Robustez de Red / Flujo Reactivo**: El worker se conecta a `ConnectivityMonitor.isOnline`. Si este flujo emite valores nulos transitorios al inicio, implementamos un filtrado de nulos y un tiempo de espera límite (timeout de 5 segundos) antes de considerar que no hay red, previniendo crashes y falsos fallos en el ciclo de sincronización.
- **Idempotencia y Envío Consistente**: Para evitar el spam de peticiones individuales, el worker agrupa todos los puntos pendientes en un solo payload y actualiza la base de datos de manera atómica para evitar duplicaciones.
- **Stubbing de WorkManager en Tests**: Instanciamos `WorkerParameters` usando reflexión dinámica de Java y proxies para adaptarnos a las firmas del constructor de WorkManager 2.10.0 en JUnit puro, sin requerir dependencias pesadas como Robolectric.

## Contratos
- **Consume:**
  - `GpsPointRepository` (Lectura y marcado de puntos de GPS).
  - `TripRepository` (Estado y datos del viaje activo).
  - `ConnectivityMonitor` (Estado de conexión del dispositivo).
  - `MobileApi` (Endpoints `/sync` y `/end`).
  - `DistanceCalculator` (Cálculo de kilometraje recorrido).
- **Expone:**
  - `SyncManager` (Interfaz pública para agendar o forzar sincronizaciones).

## Limitaciones / pendientes
- Sincronización en sub-lotes si la cantidad de puntos excede los 500 (pendiente de optimización en etapas posteriores).
