# 0008. Estrategia de escaneo óptico de QR (`BarcodeDetector` web tras un puerto)

- **Estado:** **Aceptado**
- **Fecha:** 2026-06-30
- **Feature:** [014-escaneo-qr](../../specs/014-escaneo-qr/)

## Contexto

La feature 014 provee **un único componente/servicio de captura de QR** (estilo LEGO) reutilizado
por Alta de fornituras (001), Asignación (004) y, a futuro, Traslados (007) y Bajas (009). Acepta
tres orígenes: **lector HID** (emula teclado), **tecleo manual** y **cámara** (escaneo óptico).

- El lector HID y el modo manual **no requieren dependencias**: se resuelven con detección por
  cadencia de pulsaciones + terminador y con un campo de texto.
- El **escaneo óptico por cámara** sí exige una decisión de dependencia (Principio VI): elegir un
  mecanismo con licencia y mantenimiento aceptables, sin comprometer el stack congelado (Capacitor).

Opciones consideradas para el escaneo óptico:

1. **API web `BarcodeDetector`** (`getUserMedia` + `BarcodeDetector`): **cero dependencias**;
   soportada en Chromium/Edge y Android WebView; no soportada en Firefox/Safari (degradable).
2. **Plugin nativo Capacitor/ML Kit** (`@capacitor-mlkit/barcode-scanning`): robusto en móvil
   nativo, pero **añade dependencia nativa**, requiere sincronización de plataformas y pruebas en
   dispositivo real, y su licencia/mantenimiento deben evaluarse antes de introducirlo.

## Decisión

1. **Programar contra un puerto** `OpticalScanner` (abstracción), no contra una implementación
   concreta. El componente y los consumidores dependen del puerto (LEGO / DIP).
2. **Implementación por defecto:** `WebBarcodeDetectorScanner` sobre la API web `BarcodeDetector`,
   con **cero dependencias nuevas**. Es best-effort (móvil web + webcam de PC), coherente con el
   alcance de 014 (la cámara de PC es "mejor esfuerzo").
3. **Degradación garantizada:** si la plataforma no soporta `BarcodeDetector` o se deniega el
   permiso de cámara, el componente **degrada a lector/manual** sin romper el flujo (FR-005) y
   emite un `captureError` legible (sin PII, FR-006).
4. **Extensión futura sin romper consumidores:** un plugin nativo (ML Kit) se podrá introducir
   como **otra implementación de `OpticalScanner`** (nuevo `provide`), registrando entonces la
   dependencia y su licencia. No cambia el contrato del componente ni las pantallas consumidoras.

## Consecuencias

- **Positivas:** no se introduce ninguna dependencia ahora (Principio VI); MVP entregable y
  probado (lector + manual + cámara web); la resolución `código → dato` permanece **server-side**
  (Principios II/IV); el puerto deja la puerta abierta a nativo sin refactor de consumidores.
- **Negativas / límites:** en navegadores sin `BarcodeDetector` (Firefox/Safari) el escaneo por
  cámara no está disponible y se usa lector/manual; el escaneo nativo en iOS/Android empaquetado
  con Capacitor queda pendiente de un ADR posterior si el negocio lo prioriza.
- **Seguridad:** el componente no registra el código ni PII; solo entrega el valor opaco al
  consumidor, que lo resuelve tras sesión + rol.
