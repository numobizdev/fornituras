# 0005. Formato del QR implementado: código corto opaco + impresión por lotes

- **Estado:** Aceptado (refleja la implementación existente) — con **punto abierto de firma**
- **Fecha:** 2026-06-30
- **Reemplaza a:** [0002](./0002-formato-del-qr.md)

## Contexto

El módulo de QR **ya está implementado** en `fornituras-api/` (paquete `modules/qrcodes/`) y es
sobre lo que se va a seguir trabajando. Su diseño real **difiere** del que proponía el
[ADR 0002](./0002-formato-del-qr.md) (UUID v4 + firma HMAC-SHA256). Este ADR documenta lo que
existe para que las specs y el equipo trabajen sobre la realidad.

## Decisión (lo implementado)

1. **Código opaco corto.** Cada QR contiene un **código** con formato **`FOR-XXXXX`** (prefijo
   configurable `app.qr.prefix` + sufijo de longitud `app.qr.suffixLength`, caracteres
   `[0-9A-Z]`), generado con `SecureRandom` y **único** (verificado contra BD y contra el lote).
   No contiene PII ni dato derivable (preserva el núcleo del Principio II: **opacidad**).
2. **Sin firma criptográfica.** El código **no** lleva HMAC ni UUID firmado. La autenticidad no
   se prueba criptográficamente; el código solo "existe o no existe" en BD.
3. **Generación por lotes (`LoteQR`).** Se crean N códigos de una vez con parámetros de
   **impresión física**: `cantidad`, `descripcion`, `qrSizeCm` (1.0–15.0), `paddingCm` (0.0–5.0),
   `labelPosition` (`NONE`/`TOP`/`BOTTOM`) y `mostrarBordes`. Límite por lote `app.qr.maxBatchSize`.
4. **Exportación para grabado/impresión.** PDF (`QrPdfService`) y ZIP de PNG (`QrZipService`),
   con los ajustes del lote o ajustes personalizados (`ReprintQrForm`) sin alterar los códigos.
5. **Etiquetas preimpresas, enlace posterior.** Los códigos se generan **antes** y de forma
   **independiente** de las fornituras; el enlace `código → fornitura` ocurre cuando se da de
   alta/asigna la fornitura escaneando ese código (specs 001/004/014).
6. **Acceso protegido.** Endpoints bajo `/api/v1/qr/**` con `Bearer Authentication` (JWT).

## Divergencia respecto al Principio II (firma) — punto abierto

La Constitución (Principio II) dice que el QR debe contener "un identificador opaco (UUID) **+
una firma (HMAC)**". La implementación **cumple la opacidad** (no hay PII ni dato explotable en
el código) pero **omite la firma**. Implicación de seguridad:

- Sin firma no hay **prueba criptográfica** de que el sistema emitió el código; un tercero podría
  fabricar cadenas `FOR-XXXXX`. El daño está acotado porque (a) la resolución `código → fornitura`
  ocurre **solo en el servidor tras authn+authz**, (b) un código inventado que no exista en BD da
  "no encontrado", y (c) el espacio de códigos + unicidad + rate limiting dificultan adivinar uno
  válido. El riesgo residual es la **enumeración** por un usuario ya autenticado.

**Recomendación (a decidir por el equipo / posible enmienda de la constitución):**
- **Opción A — Aceptar el riesgo:** mantener el código corto sin firma y **enmendar la redacción
  del Principio II** para exigir *opacidad* (no necesariamente HMAC), tratando la firma como
  control opcional. Añadir mitigaciones: rate limiting y auditoría de resoluciones.
- **Opción B — Añadir firma:** incorporar una capa de verificación (p. ej. HMAC del código con
  versión de llave) si el modelo de amenazas lo exige, sin romper los códigos ya impresos.

Mientras no se decida, se documenta el estado como **deuda de seguridad conocida**.

## Consecuencias

- (+) Refleja la realidad: las specs y el trabajo futuro parten de lo que existe.
- (+) Soporta el flujo físico real (lotes, tamaño en cm, PDF/ZIP) que el ADR 0002 no detallaba.
- (+) Mantiene la **opacidad** (sin PII en el QR).
- (−) Pierde la **autenticidad criptográfica** del Principio II → punto abierto arriba.
- (−) Requiere revisar la redacción del Principio II o añadir firma (decisión pendiente).

## Decisiones relacionadas

- Reemplaza [ADR 0002](./0002-formato-del-qr.md) (marcado como reemplazado).
- Enmascaramiento/identidad de la fornitura: specs 001/004; escaneo: spec 014.
- Gestor de secretos (si se adopta la Opción B con HMAC): ADR pendiente.
