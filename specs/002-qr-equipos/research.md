> ⚠️ **OBSOLETO (2026-06-30).** Research del enfoque UUID+HMAC, no implementado. La realidad
> (código `FOR-XXXXX` por lotes, sin firma) está en [spec.md](./spec.md) y el
> [ADR 0005](../../docs/04-decisiones/0005-formato-qr-implementado.md). Histórico.

# Phase 0 — Research: QR opaco y firmado

## Decisiones cerradas

### D1. Formato del QR (UUID v4 + HMAC-SHA256 + versión de llave)
- **Decisión:** ver [ADR 0002](../../docs/04-decisiones/0002-formato-del-qr.md).
- **Payload:** `v<version>.<base64url(uuid)>.<base64url(hmac)>`.
- **Rationale:** opacidad total + integridad/autenticidad + rotación sin re-grabar.
- **Alternativas rechazadas:** token autocontenido (más superficie), UUID sin firma
  (falsificable), hash del número de serie (adivinable, ata el QR a dato sensible).

### D2. Librería de renderizado del QR → ZXing
- **Decisión:** `com.google.zxing:core` + `javase` para producir PNG/SVG.
- **Rationale:** estándar de facto en JVM, **Apache-2.0**, mantenido; cumple el criterio de
  dependencias del Principio VI (necesidad/licencia/mantenimiento justificados).
- **Alternativas:** `qrcode-kotlin`, servicios externos (descartados: dependencia de red para
  algo offline y trivial localmente).

### D3. Firma e identificador con librería estándar de Java
- **Decisión:** `java.util.UUID.randomUUID()` (v4) y `javax.crypto.Mac` (HmacSHA256). Sin
  dependencia adicional.
- **Rationale:** menos superficie, primitivas probadas del JDK.

### D4. Rotación de llave HMAC
- **Decisión:** mantener un mapa `keyVersion → secretKey` cargado desde el entorno/secret
  manager. El payload porta la versión; la verificación selecciona la llave por versión. La
  emisión usa siempre la **versión activa** (la más reciente).
- **Rationale:** permite rotar sin invalidar QR ya grabados (SC-004).

## Incógnitas abiertas (no bloquean el plan)

### U1. Gestor de secretos concreto
- **Estado:** ADR pendiente (Azure Key Vault vs HashiCorp Vault vs variables de entorno).
- **Mitigación temporal:** cargar la(s) llave(s) desde variable de entorno
  (`QR_HMAC_KEYS` con formato `version:base64key[,version:base64key...]` y
  `QR_HMAC_ACTIVE_VERSION`). El nombre va en `.env.example`; el valor nunca se versiona.

### U2. Parámetros físicos del QR para grabado
- **Estado:** depende del material (tela/placa) y del láser/impresora del cliente.
- **Plan:** usar nivel de corrección de errores alto (ECC = Q o H) y validar con una **prueba
  de lectura real** por cámara y por lector manual (SC-006) antes del grabado en serie.

## Notas de seguridad (recordatorio de gates)
- El render del QR **no** debe escribir el contenido firmado en logs.
- `verify` y la resolución devuelven respuestas **genéricas** ante fallo (sin distinguir
  "firma inválida" de "no existe") para no filtrar información (FR-005).
- La llave HMAC nunca aparece en respuestas, logs ni mensajes de error.
