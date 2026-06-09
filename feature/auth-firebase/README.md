# feature:auth-firebase — Grupo StridePlanner

**Integrantes:**
- Brian Benjamin Pareja Meruvia (`Me7aBen`) — backend / arquitectura
- Anthony Arana (`anthonyarana-debug`) — backend / Product Owner

## Qué implementamos

Implementación alternativa de `MobileApi` que usa **Firebase Authentication** para el login en lugar del endpoint REST `/auth/login`.

### Arquitectura

```
AuthFirebaseModule.create()
        │
        ▼
FirebaseMobileApi (implementa MobileApi)
        │
        ├─ login()      → FirebaseAuthProvider → Firebase Auth
        └─ startTrip()  ┐
        └─ syncBatch()  ├─ delega a MobileApi REST (api-client)
        └─ endTrip()    ┘
```

### Flujo de login con Firebase
1. Deriva un email: `{driverId}@compensatuviaje.com`
2. Llama a `FirebaseAuth.signInWithEmailAndPassword(email, pin)`
3. Obtiene el **ID Token JWT** de Firebase
4. Construye la `Session` con ese token — compatible con el resto del sistema

### Por qué FirebaseAuthProvider (interfaz)
Firebase Auth usa llamadas estáticas (`FirebaseAuth.getInstance()`) que explotan sin `google-services.json`. El documento prohíbe subir ese archivo. La solución es inyectar Firebase por interfaz:
- **Producción**: `RealFirebaseAuthProvider(FirebaseAuth.getInstance())`
- **Tests**: `FakeFirebaseAuthProvider()` — sin Firebase, sin red, sin config

## Cómo correr y probar (en aislado)

```bash
./gradlew :feature:auth-firebase:test
```

No requiere `google-services.json` ni emulador.

## Decisiones técnicas

- **`FirebaseAuthProvider` como interfaz**: desacopla Firebase del dominio, permite testing puro en JVM.
- **Patrón delegación**: `FirebaseMobileApi` solo reemplaza el `login()`, el resto delega al api-client REST — evita duplicar lógica de red.
- **Email derivado**: `{driverId}@compensatuviaje.com` es una convención interna; en producción se configura el dominio real en Firebase.

## Contratos

- **Consume**: `MobileApi`, `TripSummary`, `AppResult`, `ErrorKind` de `:core:domain`; modelos de `:core:model`
- **Expone**: `AuthFirebaseModule.create()` → `MobileApi`
- **Requiere en producción**: `RealFirebaseAuthProvider(FirebaseAuth.getInstance())` desde `:app`

## Limitaciones / pendientes

- `google-services.json` va en `:app`, no en este módulo (por las reglas del proyecto).
- El campo `truck.category` queda como `"unknown"` — Firebase no provee datos del camión; esos vienen del servidor REST en el flujo normal.
