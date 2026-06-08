# feature:location-service — Servicio GPS en Segundo Plano

## Grupo
**Llanteria**

## Integrantes
| Nombre | Rol |
|--------|-----|
| [Rusell Quispe Encarnación] | LocationForegroundService + LocationTrackerImpl |
| [Julio Flores y Diego suca] | LocationServiceController + ServiceLocator |
| [Jesus Diaz Diaz y Sebastián Amarro] | Tests + AndroidManifest + Documentación |

## Módulo asignado
`02 · feature:location-service` — Foreground Service para captura GPS continua (T1 Crítico)

## Qué implementamos

- **`LocationForegroundService`**: Foreground Service que captura coordenadas GPS durante el viaje con pantalla apagada. Muestra notificación persistente "Viaje en curso 🚛". Sobrevive Doze Mode gracias al tipo `FOREGROUND_SERVICE_TYPE_LOCATION`.
- **`LocationTrackerImpl`**: Implementación del contrato `LocationTracker` usando `FusedLocationProvider`. Alta precisión, intervalo de 5 segundos, mínimo 5 metros de desplazamiento.
- **`LocationServiceController`**: API pública para que otros módulos (feature:trip) inicien y detengan el servicio.
- **`ServiceLocator`**: Proveedor de dependencias ligero sin Hilt, permite inyectar fakes en tests.
- **Filtro de precisión**: descarta automáticamente puntos con `accuracyMeters > 50`.

## Cómo probar en aislado

```bash
./gradlew :feature:location-service:test
```

Para prueba en dispositivo físico se requiere conceder `ACCESS_BACKGROUND_LOCATION`.  
En emulador: usar **Extended Controls → Location** para simular puntos GPS.

## Decisiones técnicas

- `START_STICKY`: el SO reinicia el servicio si lo mata por memoria.
- `SupervisorJob`: un fallo en la colección de puntos no cancela todo el scope.
- `MutableSharedFlow(extraBufferCapacity = 64)`: evita back-pressure si Room tarda en escribir.
- Sin Hilt: cada módulo es testeable por constructor + ServiceLocator para el servicio.

## Contratos que consume
- `LocationTracker` de `:core:domain`
- `GpsPointRepository` de `:core:domain`
- `GpsPoint`, `Trip` de `:core:model`

## Contratos que expone
- `LocationServiceController.iniciar(tripId)` / `.detener()`
- `LocationTrackerImpl` (implementación de `LocationTracker`)

## Permisos requeridos
```xml
ACCESS_FINE_LOCATION
ACCESS_BACKGROUND_LOCATION
FOREGROUND_SERVICE
FOREGROUND_SERVICE_LOCATION
WAKE_LOCK
```

## Limitaciones conocidas
- En emulador se requieren ubicaciones simuladas (mock locations).
- Recuperación automática tras cierre del proceso no implementada (bonus pendiente).
- `ServiceLocator` debe inicializarse desde `Application` en producción antes de arrancar el servicio.
