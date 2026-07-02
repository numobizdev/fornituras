# 0019. Escaneo QR con `@capacitor/barcode-scanner` (extiende ADR 0008)

- **Estado:** **Aceptado**
- **Fecha:** 2026-07-01
- **Feature:** [019-escaneo-qr-capacitor](../../specs/019-escaneo-qr-capacitor/)

## Contexto

ADR 0008 estableció el puerto `OpticalScanner` con implementación `WebBarcodeDetectorScanner`
(cero dependencias, best-effort en Chromium). La operación en campo requiere escaneo fiable en
**Android nativo** y **webcam de escritorio** con selector de cámara.

Opciones evaluadas:

1. **`@capacitor/barcode-scanner`** (oficial Capacitor v8): nativo Android/iOS + web Html5Qrcode.
2. Mantener solo `BarcodeDetector` web: insuficiente para Android empaquetado y Firefox/Safari.
3. **`@capacitor-mlkit/barcode-scanning`**: community; más pesado; descartado por preferir plugin oficial.

## Decisión

1. Introducir **`@capacitor/barcode-scanner`** como implementación **preferida** de `OpticalScanner`
   (`CapacitorBarcodeScanner`).
2. **Conservar** `WebBarcodeDetectorScanner` como fallback cuando el plugin no esté disponible.
3. En web, configurar `scanBarcode({ web: { showCameraSelection: true, scannerFPS: 10 } })`.
   **No** usar `@ionic/pwa-elements` (reservado al plugin Camera en spec 017).
4. Ampliar el puerto con `usesEmbeddedVideo()` para distinguir escaneo modal (Capacitor) vs video
   inline (web fallback).
5. Android: `minSdkVersion = 26`; permiso de cámara en manifest.

## Consecuencias

- **Positivas:** escaneo nativo Android; webcam con selector en navegador; consumidores (014) sin cambio
  de contrato `app-qr-scan`.
- **Negativas:** nueva dependencia npm; requiere `cap sync` en builds nativos.
- **Seguridad:** sin cambio — código opaco, resolución server-side.
