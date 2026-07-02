# Implementation Plan: Escaneo QR con Capacitor

**Branch**: `019-escaneo-qr-capacitor` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

## Summary

Integrar `@capacitor/barcode-scanner` como implementación preferida del puerto `OpticalScanner`
(014), con fallback a `WebBarcodeDetectorScanner`. Mejorar `/asignacion` para mostrar el almacén
de la fornitura escaneada y bloquear asignación si no está `DISPONIBLE`. Sin cambios de backend.

## Technical Context

**Language/Version**: TypeScript, Ionic 8 + Angular, Capacitor 8.

**Primary Dependencies**: `@capacitor/barcode-scanner` (nuevo, oficial); `@capacitor/core` (existente).

**Testing**: Karma/Jasmine — mock `CapacitorBarcodeScanner`, flujo asignación disponible/no disponible.

**Target Platform**: Web (`ionic serve`) + Android nativo (cuando exista plataforma).

## Constitution Check

| Principio | Cumplimiento |
|-----------|--------------|
| II. QR opaco / server-side | ✅ Componente solo entrega código |
| VI. ADR / dependencias | ✅ ADR 0019 registra el plugin |
| V. Sin PII en logs | ✅ |

## Project Structure

```text
sigefor/src/app/core/qr-scan/
├── optical-scanner.ts              # puerto ampliado
├── capacitor-barcode-scanner.ts    # NUEVO
├── web-barcode-detector-scanner.ts # renombrar/extraer de optical-scanner.ts
└── qr-scan.component.ts            # modal vs inline

sigefor/src/app/features/asignacion/pages/asignacion/
├── asignacion.page.html            # + almacenNombre
└── asignacion.page.ts
```

## Dependencies

- Extends [014](../014-escaneo-qr/spec.md), [004](../004-asignacion-resguardos/spec.md)
- [ADR 0019](../../docs/04-decisiones/0019-escaneo-qr-capacitor-barcode-scanner.md)
