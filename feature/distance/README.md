# Módulo 6: Distance

*Módulo asignado:* feature:distance — AuthCares

Integrantes: Torres Willy (Lógica Haversine), Cori Johs (Testing), Choque Karla (Revisión de contratos), Soto Ayelen (Documentación)

## Qué implementamos
HaversineDistanceCalculator — implementación de DistanceCalculator que calcula la distancia total de un viaje en kilómetros usando la fórmula de Haversine, filtrando puntos con precisión GPS baja (> 50 m por defecto). Maneja correctamente los casos límite de listas vacías o de un solo punto.

## Cómo correr y probar


./gradlew :feature:distance:test


## Contratos
- Implementa: DistanceCalculator (:core:domain)
- Consume: GpsPoint (:core:model)

## Decisiones técnicas
- Módulo Kotlin/JVM puro (sin dependencias Android).
- Filtrado por accuracyMeters configurable en el constructor para facilitar pruebas.
- Uso de min(1.0, sqrt(h)) en la fórmula para evitar errores de redondeo numérico.
- Tests deterministas con SampleData.sampleTrack de :core:testing.