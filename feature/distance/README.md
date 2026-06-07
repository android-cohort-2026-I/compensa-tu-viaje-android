# feature:distance — Cálculo de Distancia Local (Haversine)

## Grupo
**Llanteria**

## Integrantes
| Nombre | Rol |
|--------|-----|
| [RUsell Abad Quispe Encarnación] | HaversineDistanceCalculator |
| [Julio Flores y Diego suca] | Tests unitarios |
| [Jesús Diaz Diaz Y Sebastián Amarro] | Documentación |

## Módulo asignado
`06 · feature:distance` — Cálculo de distancia acumulada entre puntos GPS (T1 Crítico)

## Qué implementamos

Implementación de la interfaz `DistanceCalculator` usando la **fórmula Haversine**.
Calcula la distancia acumulada en kilómetros entre puntos GPS consecutivos,
ignorando automáticamente los puntos con baja precisión (`accuracyMeters > 50`).

## Cómo probar en aislado

```bash
./gradlew :feature:distance:test
```

## Decisiones técnicas

- **Haversine con radio terrestre 6371.0088 km**: radio medio cuadrático, más preciso que 6371.0.
- **`asin(min(1.0, sqrt(h)))`**: evita NaN por errores de punto flotante cuando h > 1.
- **Filtro de precisión integrado**: descarta puntos con accuracy > 50 m antes de calcular.
- **Lógica pura**: sin dependencias Android, 100% testeable con JUnit estándar. Ideal para TDD.

## Contratos que consume
- `List<GpsPoint>` de `:core:model`

## Contratos que expone
- Implementación de `DistanceCalculator.totalKm(points)` de `:core:domain`

## Limitaciones conocidas
- No detecta saltos imposibles por velocidad irreal (bonus no implementado).
- No aplica suavizado de trayectoria (bonus no implementado).
