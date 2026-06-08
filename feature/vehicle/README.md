# feature/vehicle — Confirmación de vehículo

## Integrantes

* Mamani Machaca Yuri
* Janely Apaza Sencia
* Morales Puma Gisela
* Luis Washinton Berrio Valencia

## Responsable de esta parte

Wash — Luis Washinton Berrio Valencia

## Módulo asignado

`feature/vehicle`

## Qué se implementó

Pantalla de confirmación del camión activo después del login. El módulo observa la sesión actual, muestra patente y categoría del vehículo, detecta si existe un viaje activo y permite confirmar para continuar sin depender de `app/`.

## Contratos usados

* Consume `SessionRepository` y `TripRepository` de `:core:domain`.
* Consume modelos `Session`, `Truck` y `Trip` de `:core:model`.
* Expone `VehicleScreen`, `VehicleViewModel`, `VehicleUiState` y `VehicleEvent`.

## Cómo probar

```bash
./gradlew :feature:vehicle:test
```

## Limitaciones

* La función pública `VehicleScreen()` sin parámetros se mantiene por compatibilidad con el host, pero la versión real recibe `VehicleViewModel`.
* El modelo `Session` define `truck` como no nulo; el ViewModel mantiene una validación defensiva para sesiones incompletas.
