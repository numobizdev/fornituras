# Implementation Plan: Escaneo y captura de QR (cámara, lector, manual)

**Branch**: `dev` (feature **014-escaneo-qr**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/014-escaneo-qr/spec.md`

## Summary

Construir **un único componente/servicio de captura de QR** (estilo LEGO) reutilizado por Asignación
(**004**), Traslados (**007**), Alta de fornituras (**001**) y Bajas (**009**), en lugar de duplicar la
lógica de escaneo en cada pantalla. Acepta tres orígenes con **detección automática**: lector HID
(emula teclado), cámara (Capacitor en móvil; webcam de PC best-effort) y **tecleo manual**. Entrega
siempre el **mismo valor** (`FOR-XXXXX`) al consumidor y **no resuelve datos en el cliente**: la
relación `código → fornitura/elemento` se resuelve en el servidor tras sesión + rol (Principios II, IV).

Es una feature **casi exclusivamente frontend** (`sigefor/`): no añade tablas ni endpoints; consume la
resolución server-side que exponen 001/004. La validación de **formato** local (`^FOR-[0-9A-Z]{5}$`) es
opcional antes de enviar (los códigos no llevan firma, ADR 0005).

## Technical Context

**Language/Version**: TypeScript + Angular/Ionic 8 (`sigefor/`). Sin trabajo de backend nuevo.

**Primary Dependencies**: Capacitor (cámara, stack congelado — Principio VI). Para el escaneo óptico se
evalúa el plugin de barcode/QR de Capacitor (community) o el de ML Kit; **introducir dependencia
requiere justificar necesidad/licencia/mantenimiento** (Principio VI) y registrar la elección. Detección
de lector HID por **velocidad de entrada + carácter terminador** (sin librería). Sin dependencia para el
modo manual.

**Storage**: ninguno. El componente produce un valor en memoria que entrega al consumidor.

**Testing**: pruebas de componente/servicio Angular (Karma/Jasmine): los tres medios entregan el mismo
valor; degradación sin cámara/permisos; el componente **no** llama a resolución de datos por su cuenta.

**Target Platform**: app Ionic (web + móvil Capacitor). Webcam de PC es best-effort.

**Project Type**: Web — monorepo; el trabajo vive en `sigefor/` (frontend).

**Performance Goals**: captura fluida; lector HID detectado por patrón de entrada rápida + terminador;
cámara abre y reconoce en condiciones normales (ver 002 SC-006 para tamaño físico).

**Constraints**: mismo valor por los tres medios (SC-001); funciona sin cámara por lector/manual
(SC-002); **cero resolución de datos en el cliente** sin verificación del servidor (SC-003, FR-004);
sin registrar PII (FR-006).

**Scale/Scope**: 1 componente + 1 servicio reutilizables; integrado en 4 pantallas consumidoras.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | El componente no maneja PII; solo un código opaco | ✅ |
| II. QR sin PII / resolución server-side | No resuelve datos en cliente; entrega el código y el servidor resuelve tras authn+authz | ✅ |
| III. Cero secretos | Sin secretos | ✅ |
| IV. Mínimo privilegio | La resolución la hace el backend con el rol del usuario; el componente no decide visibilidad | ✅ |
| V. Auditoría sin fugas | No registra PII; la auditoría de la resolución vive en el backend consumidor | ✅ |
| VI. ADR / stack congelado | Cámara vía Capacitor; cualquier plugin de escaneo nuevo se justifica y registra | ⚠️ ver research |

**Resultado del gate**: PASA con una decisión a registrar: **qué plugin de escaneo óptico** se usa
(licencia/mantenimiento) si no basta una API web. No es violación; es elección de dependencia (Principio VI).

## Project Structure

### Documentation (this feature)

```text
specs/014-escaneo-qr/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

### Source Code (repository root)

```text
sigefor/
└── src/app/core/qr-scan/            # componente/servicio compartido (no por feature → core)
    ├── qr-scan.component.ts         # campo único + botón cámara; emite (codeCaptured)
    ├── qr-scan.component.html/scss
    ├── qr-scan.service.ts           # orquesta orígenes (HID/cámara/manual) y normaliza salida
    ├── hid-detector.ts              # detección por velocidad de entrada + terminador
    └── qr-scan.types.ts             # contrato del valor entregado

# Consumidores (solo integran el componente, no duplican lógica):
#   features/fornituras (001), features/asignacion (004), features/traslados (007), features/bajas (009)
```

**Structure Decision**: el componente vive en **`core/qr-scan/`** (no en una feature), porque es
**transversal** y reutilizable — encaja con el principio LEGO y evita duplicación. Expone un único
output `codeCaptured: string` (el código opaco) y eventos de error/permiso; **no** inyecta ningún
servicio de resolución de datos. Cada consumidor decide qué hacer con el código (llamar a su
resolución server-side).

## Phase 0 — Research

Decisiones / incógnitas:
- **Escaneo óptico**: evaluar `@capacitor/barcode-scanner`/ML Kit vs API web (`BarcodeDetector`) para
  webcam de PC. Elegir la opción con mejor licencia/mantenimiento y registrar la decisión (Principio VI).
- **Detección de lector HID**: heurística de **velocidad entre keystrokes** + **carácter terminador**
  (Enter/Tab) para distinguir lector de tecleo humano; umbral configurable.
- **Permisos de cámara** (Capacitor): manejar denegación con mensaje claro y **fallback** a lector/manual.
- **Webcam de PC**: best-effort; si no hay API/hardware, degradar sin romper el flujo.

## Phase 1 — Design & Contracts

- **Contrato del componente** (inline): `@Output() codeCaptured: EventEmitter<string>` (código opaco);
  `@Output() captureError` (sin cámara/permiso denegado); `@Input() autoFocus`, `placeholder`. Validación
  de formato `^FOR-[0-9A-Z]{5}$` **opcional** antes de emitir.
- **Servicio** (inline): `QrScanService.startCamera()/stopCamera()`, manejo de HID, normalización del
  valor (trim/upper). No expone resolución de datos.
- **Quickstart** (inline): integrar el componente en una pantalla de prueba; capturar el mismo código por
  lector, cámara y manual → valor idéntico; denegar cámara → fallback a manual.

Re-check Constitution tras diseño: el componente no resuelve datos ni maneja PII; la resolución es
server-side en cada consumidor. **Gate sigue en PASA** (con la elección de plugin como acción/ADR).

## Complexity Tracking

> Sin violaciones. La única decisión abierta (plugin de escaneo óptico) se resuelve registrando la
> dependencia elegida (Principio VI); no añade complejidad estructural.
