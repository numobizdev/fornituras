# Research: Escaneo QR con Capacitor

Feature: `019-escaneo-qr-capacitor` · Fecha: 2026-07-01

## 1. Plugin de escaneo

- **Decisión**: `@capacitor/barcode-scanner` (oficial Capacitor v8, OutSystems).
- **Rationale**: Soporte nativo Android (ZXING/MLKIT), web vía Html5Qrcode con `web.showCameraSelection`,
  alineado con stack Capacitor congelado. Extiende ADR 0008 sin invalidarlo.
- **Alternativas**: `@capacitor-mlkit/barcode-scanning` (community, más pesado); solo `BarcodeDetector` web
  (actual, limitado a Chromium).

## 2. PWA Elements vs web del barcode scanner

- **Decisión**: **No** usar `@ionic/pwa-elements` para QR. PWA Elements es para `@capacitor/camera`
  (`pwa-camera-modal`). El barcode scanner trae UI web propia.
- **Config web**: `{ web: { showCameraSelection: true, scannerFPS: 10 } }`.

## 3. Puerto `OpticalScanner`

- **Decisión**: Añadir `usesEmbeddedVideo(): boolean`. `CapacitorBarcodeScanner` devuelve `false` y
  `scan()` invoca `scanBarcode()` (modal). `WebBarcodeDetectorScanner` devuelve `true` (video inline).
- **Rationale**: El plugin Capacitor no usa el `<video>` del componente; forzarlo rompería la UX.

## 4. Selección de implementación

- **Decisión**: Factory en `main.ts`: si `Capacitor.isPluginAvailable('CapacitorBarcodeScanner')`,
  usar `CapacitorBarcodeScanner`; si no, `WebBarcodeDetectorScanner`.
- **Fallback**: HID + manual siempre disponibles.

## 5. Asignación y almacén

- **Decisión**: Sin selector de almacén (≠ Traslados 007). `DISPONIBLE` = en almacén; mostrar
  `almacenNombre` del `EquipmentDetail` ya devuelto por la API.
- **Backend**: sin cambios en MVP.

## 6. Android

- **Decisión**: `minSdkVersion = 26` cuando exista carpeta `android/`; permiso `CAMERA` en manifest.
- **Acción**: Documentar en quickstart; `npx cap add android` fuera del MVP web si no hay plataforma aún.
