# Módulo 12: Mapa con Google Maps (feature:map-google)

**Grupo:** Alice  
**Integrantes:** - Rodriguez Cruz Aarón Rodrigo (Desarrollador)

## ¿Qué implementamos?
Desarrollamos el módulo opcional de visualización (T3) encargado de renderizar la ruta del viaje del chofer. Utilizamos **Google Maps Compose** para mostrar un mapa interactivo que traza una línea (Polyline) siguiendo las coordenadas GPS del viaje, ubicando marcadores visuales en el punto de inicio y fin. La cámara se centra automáticamente al inicio del trayecto.

## ¿Cómo correr y probar el módulo en aislado?
1. Configurar la API Key de Google Maps en el archivo `local.properties` del proyecto raíz (`MAPS_API_KEY=tu_llave_aqui`).
2. Abrir el archivo `MapGoogleScreen.kt` y utilizar la vista dividida (Split) para ejecutar el `@Preview`, el cual renderiza un track de ejemplo (mock) en Arequipa sin necesidad de compilar la app completa.
3. Para las pruebas automatizadas, ejecutar `./gradlew :feature:map-google:test` en la terminal o correr la clase `MapGoogleScreenTest` directamente desde Android Studio.

## Decisiones técnicas
* **Offline-first / Fallbacks:** Si el componente recibe una lista vacía de coordenadas, el mapa no arroja excepciones, sino que centra la cámara por defecto en unas coordenadas seguras (Arequipa) esperando la sincronización de datos.
* **Manejo de Estado:** Se implementó `rememberMarkerState` y `rememberCameraPositionState` para evitar recomposiciones innecesarias de la UI en Jetpack Compose, garantizando un rendimiento fluido al mover el mapa.

## Contratos
* **Consume:** `GpsPointRepository` (específicamente la lista de objetos `LatLng` representados en `routePoints`).
* **Expone:** `MapGoogleScreen` (Composable reutilizable).