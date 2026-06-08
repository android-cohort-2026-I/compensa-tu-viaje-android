# Módulo 10 — feature:vehicle — Confirmación de Vehículo

## Catálogo de Módulo y Responsabilidad
* **Módulo:** feature:vehicle
* **Criticidad:** T2 — Importante
* **Responsabilidad:** Interfaz de usuario y lógica para confirmar el camión activo asignado al chofer mediante los datos obtenidos en el inicio de sesión.

## Información del Grupo
* **Nombre del Grupo:** U-PAD
* **Integrantes:**
  * Angelo (Desarrollador Android)
  * Anyelina Yolit Mamani Puma (QA / Co-autora)

## Implementación y Decisiones Técnicas
Se ha estructurado la pantalla bajo el patrón de arquitectura MVI y State-Hoisting, desacoplando por completo la vista de los modelos simulados en producción.
* **State-Hoisting Estricto:** La vista pura procesa el estado inmutable provisto de forma externa, aislando los fakes.
* **Control de Concurrencia (Anti-Double Tap):** Se integró la bandera isProcessing para evitar múltiples escrituras simultáneas en Room.
* **Flujo Offline-First:** La UI observa la sesión local y no interrumpe el flujo principal por falta de red.

## Contratos Consumidos
Este módulo consume exclusivamente las interfaces del núcleo central:
* com.compensatuviaje.tracker.domain.SessionRepository
* com.compensatuviaje.tracker.domain.TripRepository
