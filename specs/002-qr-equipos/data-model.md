# Phase 1 — Data Model: QR opaco y firmado

> Esta feature **no** crea entidades nuevas: extiende `equipment` (definida en
> [001-inventario-equipos](../001-inventario-equipos/spec.md) y
> [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md)). El cambio se aplica con una
> **migración versionada** (Flyway/Liquibase, según ADR pendiente).

## Cambios en `equipment`

| Campo            | Tipo                | Reglas                                                        |
|------------------|---------------------|--------------------------------------------------------------|
| `qr_opaque_id`   | UNIQUEIDENTIFIER     | UUID v4. **Único**. Nullable hasta que se genera el QR.      |
| `qr_key_version` | INT                  | Versión de llave HMAC usada al emitir. Nullable hasta emitir.|
| `qr_issued_at`   | DATETIME2            | Marca de emisión. Nullable hasta emitir.                     |

- **No** se almacena la firma HMAC: se **recalcula** y verifica con la llave de su versión.
- **No** se almacena la llave: vive en el gestor de secretos / variable de entorno.
- `qr_opaque_id` se indexa como único; es el ancla del lookup `QR → equipo`.

## Value Objects (dominio, no persistencia)

- **OpaqueId**: envoltura de `UUID` (validación de formato v4).
- **KeyVersion**: entero ≥ 1; identifica la llave HMAC.
- **QrPayload**: `version` + `opaqueId` + `signature`; sabe serializar/parsear el formato
  `v<version>.<base64url(uuid)>.<base64url(hmac)>` y **no** contiene datos del equipo.

## Reglas de integridad

- Un equipo tiene **a lo sumo un** `qr_opaque_id` vigente (FR-006). La reemisión genera uno
  nuevo, invalida el anterior y queda auditada.
- Solo equipos en estado asignable/activo pueden emitir QR; equipos dados de baja, no.
- Generación concurrente del QR de un mismo equipo debe resolverse a **un** identificador
  (control de unicidad a nivel de BD + manejo idempotente en el servicio).

## Auditoría asociada (entra en `audit_log`)

| Acción         | entidad   | entidad_id        | Notas                          |
|----------------|-----------|-------------------|--------------------------------|
| `QR_GENERATE`  | equipment | equipment.id      | Sin PII.                       |
| `QR_REISSUE`   | equipment | equipment.id      | Incluye versión de llave.      |
| `QR_EXPORT`    | equipment | equipment.id      | Individual o por lote.         |
