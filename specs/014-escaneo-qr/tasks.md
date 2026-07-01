---
description: "Task list — Escaneo y captura de QR (014)"
---

# Tasks: Escaneo y captura de QR (cámara, lector, manual)

**Input**: Design documents from `specs/014-escaneo-qr/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de "mismo valor por los tres medios", degradación sin cámara/permisos y
**cero resolución de datos en el cliente**.

> **Estilo LEGO:** un solo componente/servicio en `core/qr-scan/` que las pantallas consumen. No
> duplicar lógica de escaneo en cada feature.

**Organization**: una sola user story (P1) con el componente reutilizable; la integración en cada
consumidor se lista como tareas separadas.

## Path Conventions

- **Frontend (núcleo)**: `<fe-core>/qr-scan/` = `sigefor/src/app/core/qr-scan/`.
- **Consumidores**: `sigefor/src/app/features/{fornituras,asignacion,traslados,bajas}/`.
- Pruebas: junto al componente (`*.spec.ts`).

---

## Phase 1: Setup

- [X] T001 Crear la carpeta del componente compartido en `<fe-core>/qr-scan/` (component, service, types, `hid-detector.ts`)
- [X] T002 [P] **Decidir y registrar** el plugin de escaneo óptico (Capacitor barcode/ML Kit vs `BarcodeDetector` web) con justificación de licencia/mantenimiento (Principio VI) — **ADR 0008**: `BarcodeDetector` web (cero dependencias) tras el puerto `OpticalScanner`; nativo ML Kit queda como implementación futura sin romper consumidores

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: el resto no puede empezar hasta definir el contrato del componente.

- [X] T003 Definir el contrato en `qr-scan.types.ts` (`codeCaptured: string`, `captureError`, inputs `placeholder`/`autoFocus`; el componente **no** expone resolución de datos) en `<fe-core>/qr-scan/`
- [X] T004 [P] Implementar la **normalización** del valor (trim/upper) y la validación opcional de formato `^FOR-[0-9A-Z]{5}$` en `<fe-core>/qr-scan/qr-scan.service.ts`

**Checkpoint**: contrato estable; los consumidores pueden programar contra él.

---

## Phase 3: User Story 1 - Captura con detección automática de origen (Priority: P1) 🎯 MVP

**Goal**: capturar por lector HID, cámara y manual con detección automática, entregando el mismo valor;
degradar con gracia sin cámara/permisos; nunca resolver datos en el cliente.

**Independent Test**: capturar el mismo código por los tres medios → valor idéntico; denegar cámara →
fallback a manual sin romper; el componente no llama a ninguna resolución de datos.

### Tests for User Story 1

- [X] T005 [P] [US1] Test: lector, cámara (mock) y manual entregan **el mismo valor** (SC-001) en `<fe-core>/qr-scan/qr-scan.service.spec.ts` (+ `qr-scan.component.spec.ts` para la cámara)
- [X] T006 [P] [US1] Test: sin cámara/permiso denegado → emite `captureError` y permite manual/lector (SC-002, FR-005) en `<fe-core>/qr-scan/qr-scan.component.spec.ts`
- [X] T007 [P] [US1] Test: el componente **no** invoca resolución de datos (no inyecta servicios de fornitura/elemento) — solo emite el código (SC-003, FR-004) en `<fe-core>/qr-scan/qr-scan.component.spec.ts`

### Implementation for User Story 1

- [X] T008 [US1] Implementar `hid-detector.ts` (heurística de velocidad entre keystrokes + terminador Enter/Tab; umbral configurable) en `<fe-core>/qr-scan/`
- [X] T009 [US1] Implementar `QrScanService` (normaliza salida; formato opcional) + puerto `OpticalScanner` (`scan()/stop` vía `AbortSignal`) en `<fe-core>/qr-scan/`. **Desviación:** la orquestación de lifecycle (`startCamera()/stopCamera()`, HID) vive en `QrScanComponent`; el servicio queda como lógica pura (LEGO/SRP)
- [X] T010 [US1] Implementar `QrScanComponent` (campo único con placeholder "Escanee con el lector o teclee el código" + botón cámara; emite `codeCaptured`; maneja errores/permits) en `<fe-core>/qr-scan/`
- [X] T011 [US1] Manejar permisos de cámara (denegado/no-camera/unsupported → mensaje claro + fallback a lector/manual) y webcam de PC best-effort (`WebBarcodeDetectorScanner`) en `<fe-core>/qr-scan/`

**Checkpoint**: componente reutilizable funcional y probado.

---

## Phase 4: Integración en consumidores

> Cada consumidor **solo integra** el componente y resuelve el código en su backend (server-side).

- [X] T012 [P] Integrar `QrScanComponent` en el alta/lote de **001** (`features/fornituras/`): `fornitura-form` (código en alta) y `fornitura-lote` (captura repetida con `clearOnCapture`)
- [X] T013 [P] Integrar en el paso 1 de **004** (`features/asignacion/`), resolviendo la fornitura disponible server-side vía `GET /equipment/by-codigo`
- [X] T014 [P] Integrar en el alta de traslado **007** (`features/traslados/`) — integrado en `traslado-form` al implementar la spec 007
- [~] T015 [P] Integrar en la búsqueda de baja **009** (`features/bajas/`) — **bloqueada:** la feature 009 aún no existe; se integra al implementarla

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T016 [P] Asegurar que el componente **no registra PII** ni el código en logs del cliente (FR-006): el componente/servicio no llaman a `console.*`; solo emiten el valor al consumidor
- [~] T017 Validar el quickstart (mismo valor por 3 medios; fallback sin cámara) en un dispositivo real y registrar resultados — **pendiente:** requiere lector HID + dispositivo con cámara. La equivalencia de valor y el fallback están cubiertos por 12 pruebas unitarias/de componente

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (componente, MVP) → Integración en consumidores → Polish.**
- La integración (Phase 4) depende de que exista la resolución server-side de cada consumidor (001/004/
  007/009); puede hacerse incrementalmente conforme esas features avancen.

### Parallel Opportunities

- US1: tests T005–T007 en paralelo; T004 con T003.
- Integración: T012–T015 en paralelo (features distintas).

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **Un solo componente** reutilizable (LEGO); cero resolución de datos en el cliente.
- Cualquier dependencia nueva (plugin de escaneo) se justifica y registra (Principio VI).
- Commit por tarea o grupo lógico; TDD (specs en rojo antes de implementar).

### Estado de implementación (2026-06-30)

- **Completado:** componente/servicio `core/qr-scan/` (contrato, normalización, HID, cámara web
  best-effort tras puerto `OpticalScanner`, manejo de permisos/fallback), **12 pruebas** verdes
  (`hid-detector`, `qr-scan.service`, `qr-scan.component`), ADR 0008, e integración en **001**
  (`fornitura-form`, `fornitura-lote`) y **004** (`asignacion`).
- **Diferido (`[~]`):** T014/T015 (integración en 007/009) porque esas features aún no existen;
  T017 (quickstart en dispositivo real con lector/cámara).
- **Nota fuera de alcance:** `app.component.spec.ts` falla en `dev` desde antes de esta feature
  (inyecta `AuthService → HttpClient` sin proveerlo en el test); no lo toca 014.
