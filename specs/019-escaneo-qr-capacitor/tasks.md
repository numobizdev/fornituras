---
description: "Task list — Escaneo QR Capacitor (019)"
---

# Tasks: Escaneo QR con Capacitor

**Input**: Design documents from `specs/019-escaneo-qr-capacitor/`

## Path Conventions

- **Frontend**: `sigefor/src/app/core/qr-scan/`, `sigefor/src/app/features/asignacion/`

---

## Phase 1: Setup

- [X] T001 Crear spec, plan, research, tasks y ADR 0019; actualizar `specs/README.md`
- [X] T002 `npm install @capacitor/barcode-scanner` en `sigefor/`

## Phase 2: Escaneo Capacitor (US1)

- [X] T003 Implementar `CapacitorBarcodeScanner` con `web.showCameraSelection: true`
- [X] T004 Ampliar puerto `OpticalScanner` (`usesEmbeddedVideo`) y factory en `main.ts`
- [X] T005 Adaptar `QrScanComponent` para escaneo modal vs video inline
- [X] T006 Tests de `CapacitorBarcodeScanner` y `QrScanComponent`

## Phase 3: Asignación (US2)

- [X] T007 Mostrar `almacenNombre` y mensajes de estado en `asignacion.page`
- [X] T008 Tests de flujo disponible / no disponible (si aplica)

## Phase 4: Verificación

- [ ] T009 Quickstart manual: `ionic serve` + Android (cuando haya plataforma)

**Checkpoint**: US1 + US2 independientes; consumidores 001/007/009 heredan scanner vía `app-qr-scan`.
