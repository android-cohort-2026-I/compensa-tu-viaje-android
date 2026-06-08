# feature:api-client — Grupo StridePlanner

**Integrantes:**
- Brian Benjamin Pareja Meruvia (`Me7aBen`) — backend / arquitectura
- Anthony Arana (`anthonyarana-debug`) — backend / Product Owner

## Qué implementamos

Implementación completa de la interfaz `MobileApi` (definida en `:core:domain`) usando **Retrofit + OkHttp + kotlinx.serialization**.

Cubre los 4 endpoints del contrato REST:

| Método | Endpoint | Devuelve |
|---|---|---|
| `login()` | `POST /auth/login` | `AppResult<Session>` |
| `startTrip()` | `POST /trips/start` | `AppResult<String>` (trip_id) |
| `syncBatch()` | `POST /trips/{id}/sync` | `AppResult<Int>` (puntos sincronizados) |
| `endTrip()` | `POST /trips/{id}/end` | `AppResult<TripSummary>` |

Manejo de errores HTTP: 401 → `UNAUTHORIZED`, 404 → `NOT_FOUND`, 409 → `CONFLICT`, 422 → `VALIDATION`, 5xx → `SERVER`, excepciones de red → `NETWORK`.

## Cómo correr y probar (en aislado)

```bash
./gradlew :feature:api-client:test
```

Los tests corren en JVM puro (sin emulador) usando `MockWebServer` + `MockApiDispatcher` del `:core:testing`.

## Decisiones técnicas

- **`safeCall` helper inline**: toda llamada HTTP pasa por una función genérica que convierte `Response<T>` en `AppResult<T>`, evitando repetición de manejo de errores.
- **DTOs separados de los modelos de dominio**: los `@Serializable` data classes viven solo en este módulo; el mapeo a modelos de `:core:model` ocurre dentro de `RetrofitMobileApi`, manteniendo el dominio libre de anotaciones de serialización.
- **`ApiClientModule` como factory object**: punto de entrada limpio para que otros módulos obtengan un `MobileApi` sin conocer los detalles de Retrofit.
- **Sin Hilt**: se usa inyección por constructor para mantener el módulo compilable y testeable en aislado sin contexto Android.

## Contratos

- **Consume**: `MobileApi`, `TripSummary`, `ErrorKind`, `AppResult` de `:core:domain`; modelos `Session`, `Truck`, `GpsPoint`, `LatLng` de `:core:model`; `buildRetrofit`, `MOCK_BASE_URL` de `:core:network`
- **Expone**: `ApiClientModule.create()` → `MobileApi`

## Limitaciones / pendientes

- El `tokenProvider` debe ser conectado al módulo `feature:session` en la capa de app.
- Retry automático con backoff exponencial no implementado (queda en WorkManager del `feature:sync-worker`).
