# feature:export — Grupo ecommerce

## Integrantes
| Nombre | Rol |
|--------|-----|
| Camila Nesa | UI (ExportScreen) + ViewModel |
| [Nombre compañero] | CsvGenerator + Tests |

## Módulo asignado
`feature:export` — Módulo 17 · Exportar (CSV)

## Qué implementamos
Generación y compartición de un archivo CSV con el resumen completo de un viaje.
El flujo incluye: previsualización del resumen del viaje, generación del CSV con encabezado
de resumen y tabla de puntos GPS, y apertura del share intent del sistema Android.

Estados implementados: Loading, Empty, Ready, Exporting, Done y Error.

## Cómo correr y probar en aislado
```bash
./gradlew :feature:export:test
```

## Decisiones técnicas
- `CsvGenerator` es un `object` de lógica pura (sin Android) — facilita testing sin instrumentación.
- `ExportViewModel` recibe repositorios por constructor — testeable sin Hilt.
- El CSV se escribe en `context.cacheDir` y se comparte via `FileProvider` (seguridad Android 7+).
- Sin dependencias adicionales — CSV generado con `StringBuilder` estándar de Kotlin.
- Estado `Empty` cuando no hay viaje o puntos, sin exponer errores técnicos al chofer.

## Contratos
**Consume:**
- `TripRepository` (`:core:domain`)
- `GpsPointRepository` (`:core:domain`)
- `Trip`, `GpsPoint` (`:core:model`)

**Expone:**
- `ExportScreen` — composable principal
- `ExportViewModel` — orquesta carga y exportación
- `CsvGenerator` — lógica pura de generación CSV

## Cosas avanzadas implementadas
- Todos los estados UI (Loading / Empty / Ready / Exporting / Done / Error)
- Previsualización del resumen antes de exportar
- Share intent nativo del sistema

## Limitaciones conocidas
- PDF con mapa estático (bonus) no implementado en esta versión.
