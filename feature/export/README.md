# Módulo Export - Grupo ALICE

**Integrante:** Alvaro Parisaca Encinas

## ¿Qué se hizo?
En este módulo agregamos la opción para descargar o guardar el historial de los viajes en dos formatos diferentes:
1. **Formatos CSV:** Una tabla ordenada con títulos para que se pueda abrir en Excel o Google Sheets.
2. **Texto plano:** Un resumen en texto limpio y ordenado con toda la información del viaje.

### ¿Cómo funciona en la pantalla?
- Primero entras a la pantalla y te sale un resumen de los viajes (cuántos kilómetros hiciste y el CO₂).
- Eliges si lo quieres en CSV o Texto y le das al botón "Generar".
- Te sale una vista previa del texto y un botón para **Copiar**. Al presionarlo, todo se guarda en el portapapeles del celular para que lo pegues donde quieras (así nos evitamos pedir permisos raros de almacenamiento al celular).

## Arquitectura (Cómo se organizó el código)
- `ExportScreen.kt`: Es toda la parte visual y los botones hechos con Jetpack Compose.
- `ExportViewModel.kt`: Es el que hace el trabajo pesado y convierte los datos a texto sin mezclarse con la vista.

## ¿Cómo probarlo?
Para correr las pruebas unitarias y ver que todo funcione bien y no falle nada, pon este comando en la terminal:

```bash
./gradlew :feature:export:test