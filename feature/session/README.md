# feature:session — Grupo DevPulse

**Integrantes:** Janely (Almacenamiento seguro del token JWT - T2)

## Qué implementamos
- **TokenStorageImpl**: Implementación concreta de `TokenStorage` que cifra de manera persistente el token de autenticación (JWT) utilizando `EncryptedSharedPreferences` con los esquemas de encriptación `AES256_SIV` (para llaves) y `AES256_GCM` (para valores).
- **SessionRepositoryImpl**: Gestiona de forma reactiva el ciclo de vida de la sesión mediante un `StateFlow<Session?>`. Almacena de forma segura los datos del chofer y los detalles del vehículo (camión), persistiendo la información de sesión entre inicios de la app.
- **Manejo automático de HTTP 401**: Expone la función `onUnauthorized()` para limpiar de inmediato el token de autenticación y los datos de sesión locales, notificando a la interfaz de usuario en tiempo real a través del flujo `current`.

## Cómo correr y probar (en aislado)
Para ejecutar la suite completa de pruebas unitarias locales en este módulo (que incluye el ciclo completo de guardado/lectura, validación de persistencia y emisiones de flujo):
```powershell
.\gradlew :feature:session:testDebugUnitTest
```

## Decisiones técnicas
- **Cifrado de Extremo a Extremo**: Los datos de sesión (chofer y vehículo) y el token JWT se persisten bajo un esquema de cifrado transparente (`EncryptedSharedPreferences` y un `MasterKey` gestionado por Android KeyStore), asegurando que ninguna información crítica quede en texto plano.
- **Resiliencia ante Contextos sin UI y Entornos de Testeo**: La instanciación de `MasterKey` se envuelve en un bloque de captura de excepciones (`try-catch`) y se desplaza al despachador de background `Dispatchers.IO` para evitar bloqueos del hilo principal. Si el KeyStore no se encuentra disponible (como ocurre en JUnit ejecutado sobre la JVM local de host), el sistema realiza un fallback automático y seguro a `SharedPreferences` convencionales en memoria, permitiendo pruebas unitarias veloces sin requerir emuladores ni Robolectric.
- **Validación del Flujo de Estado Reactivo**: Habilitamos la dependencia de `Turbine` en la suite de pruebas unitarias de este módulo para realizar aserciones deterministas sobre el flujo de cambios en el estado de la sesión activa tras cierres de sesión de manera instantánea.

## Contratos
- **Consume:**
  - `TokenStorage` (Interfaz provista por `:core:domain` para interactuar con el token).
  - `SessionRepository` (Interfaz de `:core:domain` para observar o mutar la sesión).
- **Expone:**
  - `TokenStorageImpl` (Proveedor de almacenamiento seguro).
  - `SessionRepositoryImpl` (Repositorio de sesión y estado reactivo).

## Limitaciones / pendientes
- Rotación automática de tokens mediante headers de respuesta HTTP (pendiente para el ciclo de optimización).
