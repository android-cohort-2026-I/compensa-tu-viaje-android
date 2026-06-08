# feature:cloud-mirror — Grupo Ecommerce

## Integrantes

* Diego Huacca Ccaso
* Leo Pinto Garate
* Daniel Jacobo Colque
* Orlando Huacasi Ccopa
* Ariana Pauca Leon

## Qué implementamos

Implementación de la interfaz `RemoteMirror` utilizando Firebase Firestore como mecanismo de respaldo en la nube para viajes finalizados.

La implementación replica únicamente viajes con estado `COMPLETED` y almacena la información en la colección `trips` de Firestore.

## Esquema Firestore

Colección:

```text
trips
 └── {tripId}
```

Documento:

```json
{
  "id": "trip_001",
  "status": "COMPLETED",
  "startedAtIso": "2026-06-08T10:00:00Z",
  "endedAtIso": "2026-06-08T11:30:00Z",
  "totalLocalDistanceKm": 42.5,
  "serverDistanceKm": 41.8,
  "co2Kg": 12.4,
  "mirroredAt": "2026-06-08T11:35:00Z"
}
```

## Cómo correr y probar

```bash
./gradlew :feature:cloud-mirror:build
```

```bash
./gradlew :feature:cloud-mirror:test
```

## Decisiones técnicas

* Firebase Firestore se utiliza como almacenamiento remoto.
* Se replica únicamente información de viajes completados.
* Se utiliza el identificador del viaje como ID del documento en Firestore.
* Se implementa idempotencia mediante `document(trip.id)` para evitar duplicados.
* Los errores son manejados mediante `AppResult`.

## Contratos

Consume:

* `RemoteMirror`
* `Trip`
* `AppResult`

Expone:

* Implementación concreta de `RemoteMirror` mediante `CloudMirrorModule`.

## Limitaciones / pendientes

* Requiere configuración de Firebase y archivo `google-services.json`.
* No incluye pruebas con Firestore Emulator.
* No implementa sincronización automática mediante WorkManager.
* No implementa resolución de conflictos entre datos locales y remotos.
