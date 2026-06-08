# feature:database — Grupo <BlackDog>

**Integrantes:** Jeferson Huittoccollo Sucapuca C24-B (Integrante 1 - Encargado de Persistencia y Base de Datos Local), Nombre (rol), ...

## Qué implementamos
<resumen>;
Se ha diseñado y desplegado la capa de persistencia local del módulo utilizando **Room Database**. Como Integrante 1, la implementación abarca exclusivamente la infraestructura de datos:
- **Entidades de Datos:** Modelado y creación de las tablas relacionales `TripEntity` (información del viaje) y `GpsPointEntity` (coordenadas geográficas asociadas).
- **Objetos de Acceso a Datos (DAOs):** Interfaces `TripDao` y `GpsPointDao` con funciones de suspensión asíncronas para operaciones seguras en hilos secundarios.
- **Configuración Central:** Estructuración de la base de datos `TripDatabase` para el mapeo nativo en SQLite.
- **Módulo de Provisión:** Diseño nativo de la clase `DatabaseModule` bajo el patrón Singleton para exponer las instancias de los DAOs de manera segura.
## Cómo correr y probar (en aislado)
```
./gradlew :feature:database:test
```

## Decisiones técnicas
<...>
- **Patrón Singleton Manual en `DatabaseModule`:** Para respetar el aislamiento estricto del módulo sin modificar el archivo de dependencias global (`libs.versions.toml`), se implementó la inicialización de Room mediante un bloque nativo `synchronized` y la anotación `@Volatile`. Esto garantiza una única instancia de la base de datos en memoria, evitando fugas y conflictos de hilos.
- **Integridad Referencial en Cascada:** Se configuró una clave foránea (`ForeignKey`) en `GpsPointEntity` apuntando a `TripEntity` con la instrucción `onDelete = ForeignKey.CASCADE`. Así, al borrar un viaje, el motor de SQLite purga en automático sus puntos GPS del almacenamiento.
- **Pruebas Unitarias Puras de Arquitectura:** Al estar el entorno local restringido de librerías de simulación de Android, se adaptó `DatabaseModuleTest` para validar el acceso a las estructuras esenciales usando código puramente nativo de Kotlin/Java.

## Contratos
- Consume: Modelos base de datos provenientes de `:core:model` e interfaces de operación de la capa `:core:domain`.
- Expone: Métodos de acceso público para persistencia local de datos (`TripDao` y `GpsPointDao`), quedando listos para ser consumidos por los repositorios o casos de uso de las capas superiores.

## Limitaciones / pendientes
<...>
- **Pendiente de Integración:** Queda pendiente que los siguientes integrantes (Capa de Dominio y Capa de Presentación) conecten sus repositorios y ViewModels utilizando las funciones estáticas de acceso que provee este módulo.
- **Consumo de DAOs para los compañeros:** Forzosamente se debe acceder a las instancias a través de las funciones estáticas del objeto del módulo inyectando el contexto de la aplicación, siguiendo la sintaxis: `DatabaseModule.provideTripDao(context)` y `DatabaseModule.provideGpsPointDao(context)`.
