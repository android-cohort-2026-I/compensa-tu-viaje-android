# Módulo 7: Auth

*Integrantes:* Torres Willy (ViewModel y Lógica de sesión), Choque Karla (UI Compose), Soto Ayelen (Testing y Fakes), Cori Johs (Estado y Documentación)

## Qué implementamos
Pantalla de Login (AuthScreen) y su orquestación con AuthViewModel. Permite al chofer iniciar sesión con Patente/ID y PIN numérico de 4 dígitos. Traduce errores HTTP a lenguaje simple para el operario y guarda el token de forma segura usando los contratos del núcleo.

## Cómo correr y probar (en aislado)

./gradlew :feature:auth:test


## Decisiones técnicas
- Validación estricta en el ViewModel: el PIN solo acepta 4 dígitos numéricos.
- Traducción de ErrorKind a mensajes simples (ej. UNAUTHORIZED -> "Credenciales inválidas"). No se exponen tecnicismos al chofer.
- Botón de "Iniciar Sesión" con Modifier.height(56.dp) para cumplimiento de UX operario.
- Uso de FakeMobileApi y Fakes locales para tests unitarios aislados.

## Contratos
- Consume: MobileApi, TokenStorage, SessionRepository (:core:domain)
- Expone: AuthScreen (Composable UI), AuthViewModel, AuthUiState

## Limitaciones / pendientes
- Sin recuperación de contraseña (según spec V1).