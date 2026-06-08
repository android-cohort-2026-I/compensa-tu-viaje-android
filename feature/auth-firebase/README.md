# feature:auth-firebase — Grupo MOVA

**Integrantes:**
- Ruberth Edgardo Tapara Hayqui 
- Mathias Cardenas Concha 
- Adriel Totora Vilca 
- Ivan 
- Maryori

**Módulo asignado:** Módulo 18 — Autenticación alternativa (Firebase Auth)

## Qué implementamos
Se ha desarrollado un sistema de autenticación completo utilizando Firebase Auth como alternativa al login REST tradicional. La implementación sigue el patrón MVVM y es totalmente reactiva.

## Cómo correr y probar (en aislado)
Para ejecutar los tests unitarios del módulo:
```bash
./gradlew :feature:auth-firebase:test
```
Para compilar el módulo de forma independiente:
```bash
./gradlew :feature:auth-firebase:assembleDebug
```

## Decisiones técnicas
- **Arquitectura:** MVVM (Model-View-ViewModel) para separar la lógica de Firebase de la interfaz de usuario.
- **Scope Manual:** Se implementó un `CoroutineScope` manual en el ViewModel para cumplir con la restricción de no añadir dependencias adicionales (como `lifecycle-viewmodel-ktx`).
- **Estados de UI:** Uso de `Sealed Class` (`AuthUiState`) para gestionar de forma robusta los estados de Loading, Success y Error.
- **UI:** Jetpack Compose utilizando componentes del `core:designsystem` para asegurar botones de ≥ 56dp.

## Contratos
- **Consume:** `AppResult`, `Session`, `Truck` de `:core:model` y `:core:domain`.
- **Expone:** `AuthFirebaseScreen`, `AuthFirebaseViewModel` y `FirebaseAuthRepository`.
- **Contrato Local:** `AuthRepository` (definido en el módulo para evitar modificar core).

## Cosas avanzadas implementadas
- Mapeo exhaustivo de excepciones de Firebase (`FirebaseAuthException`) a `ErrorKind` del dominio.
- Gestión de ciclo de vida de corrutinas manual para optimización de memoria.
- Validación previa de campos antes de invocar el servicio de red.

## Limitaciones / pendientes
- Requiere el archivo `google-services.json` en la carpeta `/app` para ejecución en dispositivo real (no incluido por reglas de seguridad).
- Pendiente: Implementar Biometría (Cosa avanzada opcional).
- Pendiente: Recuperación de contraseña.
