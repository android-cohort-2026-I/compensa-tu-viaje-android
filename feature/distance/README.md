# feature:distance — Módulo de Ejemplo

**Módulo asignado:** `:feature:distance`

## Qué implementamos
`HaversineDistanceCalculator` — implementación de `DistanceCalculator` que calcula la distancia
total de un viaje en kilómetros usando la fórmula de Haversine, filtrando puntos con precisión GPS
baja (> 50 m por defecto).

## Cómo correr y probar

```
./gradlew :feature:distance:test
```

## Contratos
- Implementa: `DistanceCalculator` (`:core:domain`)
- Consume: `GpsPoint` (`:core:model`)

## Decisiones técnicas
- Módulo Kotlin/JVM puro (sin dependencias Android).
- Filtrado por `accuracyMeters` configurable en el constructor.
- Tests deterministas con `SampleData.sampleTrack` de `:core:testing`.
