# feature:trip-detail — Detalle de Viaje

## Información del Equipo

| Campo       | Detalle                          |
|-------------|----------------------------------|
| **Grupo**   | Grupo U-PAD — Práctica Calificada Android V1 |
| **Módulo**  | Módulo 15 — `feature:trip-detail` |
| **Criticidad** | T3 — Más allá de V1 / extra   |

### Integrantes

| Nombre | Rol / Asignación |
|--------|------------------|
| Angelo Joseph Ricasca Montes | Desarrollo del Módulo 10 (`feature:vehicle`) — ViewModel, UI y lógica |
| Angelo Rodrigo Vargas Jucharo | Desarrollo del Módulo 14 (`feature:history`) — ViewModel, UI y lógica |
| María Fernanda Mansilla Tovar | Desarrollo del Módulo 15 (`feature:trip-detail`) — ViewModel, UI y lógica |
| Anyelina Yolit Mamani Puma | Testing — Tests unitarios y Fake repositories para los 3 módulos |

---

## Módulo asignado

**`feature:trip-detail`** — Pantalla de detalle de un viaje ya finalizado.  
Dado un `tripId`, consulta el viaje en la BD local y muestra sus métricas: distancia total, duración estimada, huella CO₂ y fecha de salida. Incluye un placeholder para el mini-mapa de la ruta. Maneja el caso de ID inexistente con estado de error y opción de reintento.

---

## Qué implementamos

### Arquitectura
Seguimos el patrón **MVVM** con state-hoisting en Compose:

- **`TripDetailViewModel`** — Recibe el `tripId` por constructor. Llama a `TripRepository.get(tripId)` y calcula la duración formateada. Expone un `StateFlow` con el estado de la UI.
- **`TripDetailScreen`** — Screen contenedora que conecta el ViewModel con la UI.
- **`TripDetailContent`** — Composable puro con métricas del viaje y placeholder del mini-mapa.
- **`parseIsoDate`** — Función privada con parseo tolerante a fallos (soporta ISO 8601 con y sin zona horaria).

### Estados de la UI (`TripDetailUiState`)

| Estado | Cuándo ocurre |
|--------|---------------|
| `Loading` | Estado inicial mientras se consulta la BD |
| `Success(trip, durationText)` | El viaje existe — muestra métricas |
| `ErrorNotFound` | El `tripId` no existe en la BD local o hubo una excepción |

### Contratos consumidos
- `TripRepository` — de `:core:domain` (método `suspend get(tripId: String): Trip?`)
- `LoadingState`, `ErrorState` — de `:core:designsystem`
- `Trip`, `TripStatus` — de `:core:model`

> **No se importa código de ningún otro módulo `feature:`.**

---

## Cómo correr y probar el módulo en aislado

### Prerrequisitos
- JDK 17 (no JDK 25+)
- Android Studio Hedgehog o superior

### Ejecutar los tests unitarios

```powershell
# Desde la raíz del proyecto
./gradlew :feature:trip-detail:test
```

**Resultado esperado:** `BUILD SUCCESSFUL` — 2 tests en verde.

### Ver Previews de Compose

Abrir `TripDetailScreen.kt` en Android Studio → panel derecho **"Design"**:

- `TripDetailContentSuccessPreview` — viaje de 42.8 km, 1h 30m, CO₂ 8.4 kg
- `TripDetailContentNotFoundPreview` — estado de error con botón de reintento

### Compilar el módulo aislado

```powershell
./gradlew :feature:trip-detail:assembleDebug
```

---

## Tests unitarios implementados

Archivo: `TripDetailScreenTest.kt`

| Test | Qué verifica |
|------|-------------|
| `cuando el viaje existe en la BD local debe calcular duracion y pasar a estado Success` | Con un viaje de 45 min, el estado es `Success` con `durationText = "45 min"` y el `trip.id` correcto |
| `cuando se busca un ID inexistente debe pasar a estado ErrorNotFound de manera controlada` | Con `stubbedTrip = null`, el estado es `ErrorNotFound` sin lanzar excepción no controlada |

**Estrategia:** Fake manual de `TripRepository` con `stubbedTrip` configurable. Se usa `StandardTestDispatcher` + `advanceUntilIdle()` para controlar las corrutinas.

---

## Decisiones técnicas

- **`parseIsoDate` tolerante a fallos**: maneja tanto `Instant.parse` (formato con `Z`) como `LocalDateTime.parse` (sin zona horaria). Si ambos fallan, retorna `Instant.EPOCH` en lugar de crashear — el chofer nunca ve un stack trace.
- **Cálculo de duración en el ViewModel**: `calculateDuration` formatea horas y minutos (`1h 30m` / `45 min`) y retorna `"En progreso"` si `endedAtIso` es null. La lógica de presentación vive en el ViewModel, no en el composable.
- **`ErrorNotFound` para cualquier fallo**: tanto el caso de ID inexistente como una excepción inesperada de la BD caen al mismo estado, simplificando la UI y evitando mensajes técnicos.
- **Placeholder de mini-mapa**: el área del mapa está implementada como un `Box` con `primaryContainer` listo para integrar el composable del equipo de `feature:map-*` cuando el revisor ensamble el módulo completo.
- **`onRetry` en `ErrorState`**: el botón llama a `loadTripDetails()` del ViewModel, permitiendo reintentar la carga sin salir de la pantalla.
- **Inyección por constructor**: sin Hilt, el módulo es completamente testeable en aislado.
