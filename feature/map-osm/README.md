# feature:map-osm — Mapa con OpenStreetMap

## Grupo
**Llanteria**

## Integrantes
| Nombre | Rol |
|--------|-----|
| [Rusell Quispe Encarnación] | MapOsmScreen (UI Compose + osmdroid) |
| [Julio Flores y diego suca] | MapOsmViewModel + FakeGpsPointRepository |
| [Jesus diaz diaz y sebastian amarro] | Tests + AndroidManifest + Documentación |

## Módulo asignado
`13 · feature:map-osm` — Mapa alternativo con OpenStreetMap sin API key (T3 Extra)

## Qué implementamos

- **`MapOsmScreen`**: pantalla Compose que muestra la ruta del viaje sobre OpenStreetMap usando `AndroidView` con `MapView` de osmdroid. Incluye polyline azul, marcador de inicio y marcador de fin.
- **`MapOsmViewModel`**: carga los puntos GPS del repositorio como Flow reactivo y expone `MapOsmUiState`.
- **Estados manejados**: Cargando, Sin datos (vacío), Error, Mapa con ruta.
- **Sin API key**: usa tiles públicos MAPNIK de OpenStreetMap.

## Cómo probar en aislado

```bash
./gradlew :feature:map-osm:test
```

## Decisiones técnicas

- `AndroidView` con `MapView` de osmdroid dentro de Compose (no hay wrapper nativo Jetpack Compose para osmdroid).
- `userAgentValue = packageName`: cumple la política de uso de tiles OSM.
- `isTilesScaledToDpi = true`: mejora la legibilidad del mapa en pantallas de alta densidad.
- `update` en `AndroidView`: refresca la polyline cuando llegan nuevos puntos GPS (viaje en vivo).
- ViewModel sin Hilt: testeable directamente con `FakeGpsPointRepository`.

## Contratos que consume
- `GpsPointRepository.pointsForTrip(tripId)` de `:core:domain`
- `GpsPoint` de `:core:model`

## Contratos que expone
- `MapOsmScreen(tripId)` — composable reutilizable desde `feature:trip-detail` u otros módulos

## Permisos requeridos
```xml
INTERNET                  <!-- tiles del mapa -->
ACCESS_NETWORK_STATE      <!-- verificar conectividad -->
WRITE_EXTERNAL_STORAGE    <!-- caché de tiles (API ≤ 28) -->
```

## Cosas avanzadas implementadas
- Actualización en tiempo real de la polyline cuando llegan nuevos puntos.
- Animación de cámara con `animateTo` al último punto recibido.

## Limitaciones conocidas
- Sin internet, los tiles del mapa no se cargan (tiles offline no implementados).
- Tiles offline descargados (bonus) pendiente de implementación.
- El estilo vectorial MapLibre es opcional y no está implementado.
