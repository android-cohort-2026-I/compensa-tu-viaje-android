# feat(cloud-mirror): Módulo 19 — Cloud Mirror [grupo Game_Ayar]

## Resumen

Se implementó el módulo `feature:cloud-mirror`, encargado de realizar una réplica remota de viajes finalizados utilizando Firebase Firestore como almacenamiento en la nube.

La solución implementa el contrato `RemoteMirror` definido en `:core:domain` y permite almacenar información de viajes completados para respaldo y futuras consultas.

## Cambios realizados

* Implementación de `CloudMirrorModule`.
* Implementación de la interfaz `RemoteMirror`.
* Integración con Firebase Firestore.
* Validación para replicar únicamente viajes con estado `COMPLETED`.
* Almacenamiento de información en la colección `trips`.
* Prevención de duplicados mediante el uso de `document(trip.id)`.
* Manejo de errores utilizando `AppResult`.
* Actualización del módulo de pruebas.
* Documentación del módulo en `README.md`.

## Esquema Firestore

Colección:

```text
trips
 └── {tripId}
```

Documento almacenado:

```json
{
  "id": "trip_001",
  "status": "COMPLETED",
  "startedAtIso": "...",
  "endedAtIso": "...",
  "totalLocalDistanceKm": 42.5,
  "serverDistanceKm": 41.8,
  "co2Kg": 12.4,
  "mirroredAt": "2026-06-08T11:35:00Z"
}
```

## Verificación

### Build del módulo

Comando ejecutado:

```bash
./gradlew :feature:cloud-mirror:build
```

Resultado:

```text
BUILD SUCCESSFUL
```

### Tests del módulo

Comando ejecutado:

```bash
./gradlew :feature:cloud-mirror:test
```

Resultado:

```text
BUILD SUCCESSFUL
```

## Evidencias

### Build exitoso

<img width="1075" height="97" alt="Captura de pantalla 2026-06-08 131610" src="https://github.com/user-attachments/assets/653146a1-bd36-480c-87fb-126d804853fd" />

### Tests exitosos

<img width="1045" height="97" alt="Captura de pantalla 2026-06-08 131651" src="https://github.com/user-attachments/assets/edbd6631-db95-46d3-95c4-837fe09c48c4" />

### Implementación de CloudMirrorModule

<img width="800" height="831" alt="Captura de pantalla 2026-06-08 131943" src="https://github.com/user-attachments/assets/0e369f8c-e706-4004-b376-c125300d2103" />

## Restricciones respetadas

* Se trabajó únicamente dentro de `feature/cloud-mirror`.
* No se modificó `:app`.
* No se modificó ningún módulo de `:core`.
* No se modificó `gradle/libs.versions.toml`.
* El módulo compila y se prueba de forma aislada.

## Integrantes

* Diego Huacca Ccaso
* Leo Pinto Garate
* Daniel Jacobo Colque
* Orlando Huacasi Ccopa
* Ariana Pauca Leon
