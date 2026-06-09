# feature:map-osm — Equipo Llanteria

**Integrantes:** Katherine (ViewModel y UiState), Alexandra (UI Compose + AndroidView), Víctor (Tests unitarios y documentación)

## Qué implementamos

Módulo que muestra la ruta de un viaje sobre un mapa de OpenStreetMap usando la librería osmdroid.
Dibuja una polyline con todos los puntos GPS del viaje y coloca marcadores diferenciados de inicio y fin.
Maneja cuatro estados de UI: Loading, Empty, Error y Success.

## Cómo correr y probar (en aislado)
./gradlew :feature:map-osm:test

## Decisiones técnicas

- **osmdroid sobre Google Maps:** no requiere API key, lo que simplifica la configuración del módulo.
- **AndroidView como puente:** osmdroid usa el sistema de vistas clásico de Android (View), no Compose. Se envuelve el `MapView` dentro de un `AndroidView` para integrarlo con Jetpack Compose sin romper la arquitectura.
- **FakeGpsPointRepository local:** no existe un fake de `GpsPointRepository` en `:core:testing`, por lo que se creó uno dentro del módulo de test. Usa `MutableStateFlow` para controlar los datos emitidos en cada test.
- **stateIn con WhileSubscribed:** el ViewModel expone el estado como `StateFlow` con `SharingStarted.WhileSubscribed(5_000)` para cancelar la colección automáticamente cuando no hay observadores.

## Contratos

- Consume: `GpsPointRepository` (de `:core:domain`), `GpsPoint` (de `:core:model`)
- Expone: `OsmMapScreen` — composable principal que recibe un `OsmMapViewModel` y un `tripId`

## Limitaciones / pendientes

- El zoom actual centra la cámara en el primer punto de la ruta, no en el bounding box completo de todos los puntos.
- No implementa tiles offline (descarga del área del mapa para uso sin internet).
- El estado `Error` está definido en `OsmMapUiState` pero el ViewModel actual no lo emite (no hay manejo de excepciones en el repositorio).
