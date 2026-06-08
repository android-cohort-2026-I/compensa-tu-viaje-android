# Compensa Tu Viaje — Android

App Android multi-módulo para el registro GPS de viajes de camiones y compensación de kilometraje.

## Compilar

**Prerequisitos:** JDK 17, Android SDK (cmdline-tools o Android Studio), Gradle 8.11+.

```bash
./gradlew :feature:distance:test     # módulo de ejemplo
./gradlew assembleDebug              # build completo
```

## Módulos

| Módulo | Descripción |
|---|---|
| `:core:model` | Data classes compartidas |
| `:core:domain` | Contratos / interfaces |
| `:core:common` | Utilidades (Iso8601, formatKm) |
| `:core:testing` | Fakes + SampleData + MockWebServer |
| `:core:designsystem` | Tema + componentes operario (Compose) |
| `:core:network` | Retrofit factory con auth interceptor |
| `:feature:distance` | Haversine (ejemplo completo) |
| `:feature:database` | Room: TripRepository, GpsPointRepository |
| `:feature:location-service` | LocationTracker (Foreground Service) |
| `:feature:sync-worker` | SyncManager (WorkManager) |
| `:feature:api-client` | MobileApi (Retrofit, 4 endpoints) |
| `:feature:trip` | Pantallas del flujo de viaje |
| `:feature:auth` | Login REST + sesión |
| `:feature:onboarding` | Permisos + batería |
| `:feature:session` | TokenStorage, SessionRepository |
| `:feature:vehicle` | Confirmación de vehículo |
| `:feature:connectivity` | ConnectivityMonitor |
| `:feature:map-google` | MapScreen Google Maps |
| `:feature:map-osm` | MapScreen OSMDroid |
| `:feature:history` | Historial de viajes |
| `:feature:trip-detail` | Detalle de viaje |
| `:feature:stats` | Estadísticas |
| `:feature:export` | Exportación de datos |
| `:feature:auth-firebase` | Auth Firebase |
| `:feature:cloud-mirror` | Réplica Firestore |

Ver [CONTRIBUTING.md](CONTRIBUTING.md) para las reglas de cada equipo.
## Revisión del proyecto

Se realizó una revisión básica de la estructura del proyecto Android, considerando los módulos principales como `app`, `core` y `feature`.

Este aporte ayuda a identificar de forma rápida la organización inicial del repositorio y facilita la comprensión del proyecto para nuevos colaboradores.

