# feature:api-client — Cliente del API REST

## Grupo
**Nova**

## Integrantes
| Nombre | Rol |
|---|---|
| Mathias | Implementación completa del módulo api-client |

## Módulo asignado
`feature:api-client` — Cliente Retrofit de los 4 endpoints del contrato + manejo de errores HTTP + JWT.

## Qué implementamos

Implementación completa de la interfaz `MobileApi` del contrato usando Retrofit + kotlinx.serialization.

### Archivos principales

- **`ApiClientModule.kt`** — contiene todo:
  - DTOs de request/response (`@Serializable`)
  - `TrackerApiService` — interfaz Retrofit con los 4 endpoints
  - `RetrofitMobileApi` — implementa `MobileApi`, mapea DTOs ↔ modelos de dominio, maneja errores HTTP
  - `ApiClientModule` — factory/entry point que construye la instancia lista para usar

### Endpoints implementados

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/auth/login` | Login del chofer → `Session` (token JWT + datos del camión) |
| `POST` | `/trips/start` | Inicia un viaje → `trip_id` |
| `POST` | `/trips/{trip_id}/sync` | Sincroniza lote de puntos GPS → cantidad sincronizada |
| `POST` | `/trips/{trip_id}/end` | Finaliza el viaje → `TripSummary` (distancia servidor + CO₂) |

### Manejo de errores

| HTTP | `ErrorKind` |
|---|---|
| 401 | `UNAUTHORIZED` |
| 404 | `NOT_FOUND` |
| 409 | `CONFLICT` |
| 422 | `VALIDATION` |
| 5xx | `SERVER` |
| Timeout/sin red | `NETWORK` |

El chofer nunca ve códigos HTTP ni stack traces — solo se propaga `AppResult.Err` con un `ErrorKind` semántico.

## Cómo correr y probar el módulo en aislado

```bash
# Desde la raíz del proyecto
./gradlew :feature:api-client:test

# Con reporte HTML (opcional)
./gradlew :feature:api-client:test --info
# El reporte queda en: feature/api-client/build/reports/tests/test/index.html
```

Los tests NO requieren dispositivo ni emulador — son tests JVM puros con MockWebServer.

## Decisiones técnicas

- **`RetrofitMobileApi`** está separada de `ApiClientModule` para facilitar el testing (se puede instanciar directamente con cualquier `MockWebServer`).
- **`safeCall`** envuelve todos los llamados en `try/catch` con `Dispatchers.IO`, devolviendo `AppResult.Err(NETWORK)` ante cualquier excepción de red (timeout, sin conexión, etc.).
- Los **DTOs son internos** al módulo — ningún otro módulo los importa. El contrato público es `MobileApi` de `:core:domain`.
- La **base URL es configurable** por constructor para facilitar tests y futura integración con el backend real.
- El interceptor de `Authorization: Bearer` ya viene de `buildRetrofit()` en `:core:network`; este módulo solo provee el `tokenProvider`.

## Contratos que consume y expone

**Consume:**
- `:core:domain` → `MobileApi`, `AppResult`, `ErrorKind`, `TripSummary`
- `:core:model` → `Session`, `Truck`, `GpsPoint`, `LatLng`
- `:core:network` → `buildRetrofit()`

**Expone:**
- Implementación de `MobileApi` a través de `ApiClientModule.mobileApi`

## Cosas avanzadas implementadas
- Interceptor de logging sin exponer datos sensibles (viene de `core:network`)
- Base URL configurable por variable de entorno/constructor

## Limitaciones conocidas / pendientes
- No implementa backoff exponencial en reintentos (el reintento lo maneja el `sync-worker`)
- El backend real aún no existe; se trabaja contra MockWebServer y el mock del contrato
- Caché de respuestas no implementada (fuera del MVP)
