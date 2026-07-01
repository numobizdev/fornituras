# Quickstart / Validación — Bitácora de auditoría (012)

Resultados de la validación de los escenarios de `spec.md`/`plan.md`. La bitácora es el puerto
transversal del Principio V: el `AuditWriter` que ya consumían todas las features ahora **persiste**
en `audit_log` (append-only), redactando PII. Inmutabilidad y retención: [ADR 0012](../../docs/04-decisiones/0012-inmutabilidad-y-retencion-auditoria.md).

## Contrato

- `GET /api/v1/audit` → eventos paginados y filtrables (actor, acción, entidad, rango de fechas).
  **Solo ADMIN**. No existe endpoint de escritura: la bitácora se alimenta internamente vía
  `AuditWriter`.

## Escenarios validados

| Escenario | Cómo se valida | Estado |
|-----------|----------------|--------|
| Una operación sensible genera exactamente 1 registro con los campos requeridos (SC-001) | `AuditCaptureTest` (acceso a ficha → 1 registro VIEW_OFFICER con actor, entidad_id, timestamp) | ✅ |
| Cero PII/secretos en claro (SC-002) | `AuditNoPiiTest` (un detalle con CURP se persiste redactado a `***`) + `AuditRedactor` | ✅ |
| Los accesos denegados también se auditan (FR-006) | `AuditDeniedTest` (rol operativo consulta la bitácora → 403 + evento `ACCESS_DENIED`) | ✅ |
| Inmutabilidad: ningún usuario de app edita/borra (SC-003) | `AuditImmutabilityTest` (el repositorio no expone update/delete) + triggers `INSTEAD OF UPDATE/DELETE` en la migración (SQL Server) | ✅ |
| Consulta paginada y filtrable (FR-004) | `AuditQueryContractTest` (paginación + filtro por acción) | ✅ |
| Consulta restringida a auditoría/admin (FR-004) | `AuditQueryAuthTest` (CAPTURISTA → 403; ADMIN → 200) | ✅ |

## Captura automática (US2)

- **Todas** las operaciones que ya llamaban a `AuditWriter` (alta/edición/baja de fornituras,
  asignación/devolución, traslados, incidencias, acceso a PII, exportaciones, etc.) ahora quedan
  **persistidas** — sin cambiar sus servicios (dependían del puerto).
- **Login** se audita en `AuthService`; **accesos denegados** vía `AuthorizationDeniedEvent`
  (publicador de eventos de autorización en `SecurityConfig`).
- La escritura usa transacción propia (`REQUIRES_NEW`) para no perder el registro aunque el llamador
  esté en solo-lectura o su transacción falle.

## Frontend

- Página `/auditoria` (menú **Bitácora de Auditoría**, visible **solo para ADMIN** por el filtrado de
  menú por rol): tabla paginada con filtros colapsables (actor, acción, entidad, rango de fechas),
  skeletons de carga y estado vacío (patrones ui-ux-pro-max, consistentes con 010/011).

## Alcance / decisiones (ADR 0012)

- **Inmutabilidad** en dos capas: repositorio sin update/delete (app) + triggers en BD (SQL Server).
- **`prev_hash`** queda **reservado**; el **hashing encadenado** (T021) es un refuerzo **posterior**,
  no MVP.
- **Retención**: mínimo 24 meses en línea; automatización de purga/archivado pendiente (ADR 0012).
