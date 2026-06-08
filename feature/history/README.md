# feature:history — Historial de Viajes

## Información del Equipo

| Campo       | Detalle                          |
|-------------|----------------------------------|
| **Grupo**   | Grupo U-PAD — Práctica Calificada Android V1 |
| **Módulo**  | Módulo 14 — `feature:history`    |
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

**`feature:history`** — Pantalla de historial de viajes completados.  
Lista todos los viajes finalizados leídos de la base de datos local, ordenados por fecha descendente. Cada ítem muestra distancia, duración e indicador de sincronización. Al tocar un viaje navega al detalle.

---

## Qué implementamos

### Arquitectura
Seguimos el patrón **MVVM** con state-hoisting en Compose:

- **`HistoryViewModel`** — Suscribe a `TripRepository.completedTrips()` (Flow reactivo). Ordena los viajes por `startedAtIso` de forma descendente antes de emitir el estado.
- **`HistoryScreen`** — Screen contenedora que conecta el ViewModel con la UI y expone el callback de navegación al detalle.
- **`HistoryContent`** — Composable puro con `LazyColumn` para la lista.
- **`TripHistoryItem`** — Componente de tarjeta individual con distancia, duración calculada e indicador de sync (ícono verde/naranja).

### Estados de la UI (`HistoryUiState`)

| Estado | Cuándo ocurre |
|--------|---------------|
| `Loading` | Estado inicial mientras se carga la lista |
| `Empty` | No hay viajes completados en la BD local |
| `Success(trips: List<Trip>)` | Lista no vacía, ordenada por fecha descendente |

### Contratos consumidos
- `TripRepository` — de `:core:domain` (método `completedTrips(): Flow<List<Trip>>`)
- `LoadingState`, `EmptyState` — de `:core:designsystem`
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
./gradlew :feature:history:test
```

**Resultado esperado:** `BUILD SUCCESSFUL` — 2 tests en verde.

### Ver Previews de Compose

Abrir `HistoryScreen.kt` en Android Studio → panel derecho **"Design"**:

- `HistoryContentSuccessPreview` — lista con 2 viajes (uno sincronizado, uno pendiente)
- `HistoryContentEmptyPreview` — estado vacío sin viajes

### Compilar el módulo aislado

```powershell
./gradlew :feature:history:assembleDebug
```

---

## Tests unitarios implementados

Archivo: `HistoryScreenTest.kt`

| Test | Qué verifica |
|------|-------------|
| `verificar que el estado pasa a Success y los viajes se ordenan cronologicamente de forma descendente` | Con 3 viajes desordenados, el estado es `Success` y la lista llega ordenada: más reciente primero |
| `verificar que cuando no existen registros en la base de datos el estado sea Empty` | Con lista vacía, el ViewModel emite `HistoryUiState.Empty` |

**Estrategia:** Fake manual de `TripRepository` con `mockTrips` configurable. Se usa `StandardTestDispatcher` + `advanceUntilIdle()` para controlar las corrutinas.

---

## Decisiones técnicas

- **Ordenamiento en el ViewModel**: los viajes se ordenan por `startedAtIso` descendente dentro del ViewModel (no en la UI), manteniendo la lógica de presentación fuera del composable.
- **`catch` en el Flow**: si `completedTrips()` lanza una excepción (por ejemplo, error de BD), el estado cae a `Empty` silenciosamente sin exponer tecnicismos al chofer.
- **Duración calculada en `remember`**: `TripHistoryItem` calcula la duración con `remember(startedAtIso, endedAtIso)` para no recalcular en cada recomposición.
- **Indicador de sync visual**: se muestra `CloudDone` (verde) o `CloudUpload` (naranja) según `trip.isSyncedToServer`, dando feedback inmediato al chofer sobre el estado de su dato.
- **`LazyColumn` con `spacedBy`**: eficiente para listas largas de viajes sin cargar todos los items en memoria.
- **Inyección por constructor**: sin Hilt, el módulo es completamente testeable en aislado.
