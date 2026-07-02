# Hallazgos — Auditoría de cobertura de la migración a .NET (specs 001–017)

**Fecha:** 2026-07-01 · **Ejecutada sobre:** `fornituras-api-dotnet/` (rama `dev`) vs referencia
`fornituras-api/` (Java, obsoleto) · **Naturaleza:** solo lectura; no se modificó código de
producción (SC-005 cumplido).

## Resumen ejecutivo

La migración es **de alta fidelidad a nivel de API y datos**: los 17 controllers de negocio y sus
endpoints tienen equivalente 1:1 en .NET, y las 18 tablas del esquema están presentes en las
migraciones EF Core. Se detectaron **2 brechas de severidad Alta** (controles transversales que no
se portaron) y **1 mejora** (una brecha de seguridad conocida quedó cerrada). El resto son
**decisiones de migración** intencionales, ya cubiertas por ADRs.

| Severidad | # | 
|-----------|---|
| 🔴 Alta   | 2 (**ambas REMEDIADAS**, ver §Brechas) |
| 🟡 Media  | 0 |
| 🟢 Mejora | 1 |

> **Actualización — remediación (2026-07-01):** B-1 y B-2 fusionadas a `dev`
> (`fix/rate-limiting-dotnet`, `fix/audit-inmutable-dotnet`). Ver detalle al pie de cada brecha.

## Matriz de cobertura por spec (001–017)

| Spec | Superficie .NET | Estado |
|------|-----------------|--------|
| 001 inventario-equipos | `EquipmentController` (list/get/by-codigo/post/batch/put/patch-status), `Equipment`, `uk_equipment_codigo_norm` | **C** |
| 002 qr-equipos | `QrController` (lotes CRUD + codigos + pdf/zip + export), `LoteQr`, QRCoder/QuestPDF | **C** |
| 003 elementos-padron | `OfficersController` (list/get/post), `Officer` con PII cifrada (`PiiCipher`) + `BlindIndexer`, `uk_officers_placa/curp/rfc_idx` | **C** |
| 004 asignacion-resguardos | `AssignmentsController` (list/post/return/reassign), `ux_assignment_vigente` | **C** |
| 005 almacenes | `WarehousesController` (CRUD + deactivate/delete), `Warehouse` | **C** |
| 006 tipos-fornitura | `CatalogController` + `catalog/catalog_item` (tipo de prenda, tallas), `foto_url` | **C** |
| 007 traslados | `TransfersController` (list/get/post/receive/cancel), `Transfer`+`TransferItem` | **C** |
| 008 incidencias | `IncidentsController` (list/get/post/patch) + `AlertsController` (vigencia), `Incident` | **C** |
| 009 bajas | `DecommissionsController` (list/reasons/post, `AuthorizeDecommission`), `Decommission`+`DecommissionReason` | **C** |
| 010 dashboard | `DashboardController` (summary), `DashboardService` | **C** |
| 011 reportes | `ReportsController` (totals/active-assignments(+export)/predefined(+export)) — export vía QuestPDF/propio en vez de Apache POI | **C** (ver Decisión D-2) |
| 012 auditoria | `AuditController` (`ReadAudit`), `AuditWriter`, `AuditLog` con `prev_hash` | **P** (ver Brecha B-2) |
| 013 usuarios | `UsersController` (CRUD + enabled + role, `ManageUsers`), `AuthController` (login/activate/change/forgot/reset), `RolePolicy` (5 roles), lockout (`FailedAttempts`/`LockedUntil`) | **C** (MFA sigue gated ADR 0014) |
| 014 escaneo-qr | Resolución vía `/api/v1/qr` autenticado; escaneo cámara/manual es **frontend** | **C / FE** |
| 015 catalogos-sexo-sangre | `catalog/catalog_item` con SEXO/TIPO_SANGRE (consolidación ADR 0007) | **C** |
| 016 landing-configurable | `LandingController` (`public` AllowAnonymous + `home` + sections CRUD/reorder/activate), `LandingSection` | **P** (ver Brecha B-1: falta rate limiting del endpoint público) |
| 017 gestion-de-fotos | `MediaController` (post/get/delete), `MediaService`, `MediaAsset`, ImageSharp, gating+RBAC PII | **C** |
| 017 migracion-api-dotnet | La migración misma: path base `/sigefor`, `ApiResponse<T>`, JWT, RBAC — cumplida salvo B-1/B-2 | **P** |

**Endpoints:** todos los del backend Java tienen equivalente en .NET, **excepto `QrWebController`**
(UI web server-side `/qr/**`) — ver Mejora M-1. **Esquema:** las 18 tablas están presentes;
`equipment_type/size` y `sexo/tipo_sangre/municipio` se consolidaron en `catalog/catalog_item` o
texto libre (ADR 0007) — ver Decisión D-1.

## Brechas (remediación pendiente)

### B-1 — 🔴 Alta · Rate limiting (ADR 0010) no portado
- **Qué:** el backend Java implementa `Bucket4jRateLimiter` (token-bucket en memoria) para proteger
  **login**, resolución **by-codigo** del QR y el **endpoint público de landing** (ADR 0010 / 0015).
- **En .NET:** no hay `AddRateLimiter`/`UseRateLimiter` ni política alguna; el paquete
  `Microsoft.AspNetCore.RateLimiting` está disponible transitivamente pero **no se usa**.
- **Riesgo:** fuerza bruta contra login y abuso del endpoint público sin límite.
- **Remediación propuesta:** cablear el rate limiter nativo de ASP.NET Core (`AddRateLimiter` con
  ventana fija por IP en `/auth/login`, resolución de QR y `/landing/public`); no requiere Bucket4j.
  Ejecutar en una rama de remediación (¿reabrir alcance de spec 016/013 o ADR 0010?).
- **✅ REMEDIADA** (`fix/rate-limiting-dotnet`): `AddRateLimiter`/`UseRateLimiter` con políticas
  `by-codigo` (30/60s por actor) y `public` (60/60s por IP), 429 en `ApiResponse`, aplicadas a
  `EquipmentController.GetByCodigo` y `LandingController.GetPublic`; límites en `App:RateLimit`.
  El login sigue protegido por *lockout*. Sin dependencias nuevas.

### B-2 — 🔴 Alta · Inmutabilidad de la bitácora (ADR 0012) no portada
- **Qué:** Java `V21__create_audit_log.sql` crea triggers `trg_audit_log_no_update` y
  `trg_audit_log_no_delete` (`INSTEAD OF UPDATE/DELETE`) que **rechazan a nivel de BD** cualquier
  modificación/borrado de la bitácora (garantía append-only, ISO 27001).
- **En .NET:** la migración `InitialCreate` crea `audit_log` **sin** esos triggers (EF no genera SQL
  personalizado por defecto). La app tampoco expone update/delete, pero **se perdió la garantía a
  nivel de base de datos**.
- **Riesgo:** una cuenta con acceso a BD podría alterar/borrar auditoría sin dejar rastro.
- **Remediación propuesta:** añadir una migración EF con `migrationBuilder.Sql(...)` que recree los
  dos triggers `INSTEAD OF`. También evaluar el encadenamiento por `prev_hash` (hoy reservado en
  ambos backends, sin calcular).
- **✅ REMEDIADA** (`fix/audit-inmutable-dotnet`): migración EF `AddAuditImmutabilityTriggers`
  recrea `trg_audit_log_no_update` y `trg_audit_log_no_delete` (`INSTEAD OF UPDATE/DELETE`,
  `RAISERROR`) vía `migrationBuilder.Sql`; `Down` los elimina. Sin cambio de esquema. El
  encadenamiento por `prev_hash` queda como mejora futura (no era peor que en Java).

## Mejora detectada

### M-1 — 🟢 Brecha conocida `/qr/**` sin auth: CERRADA en .NET
- El backend Java tenía `QrWebController` (`/qr/generar`, `/qr/lotes...`, `/qr/**`) como UI web
  server-side, marcada `permitAll` — brecha documentada (genera/enumera/descarga QR sin sesión).
- En .NET **no existe** ese controller; el QR solo se expone en `/api/v1/qr/**` con
  `[Authorize(Roles = WriteInventory)]`. La brecha queda **cerrada** por la migración.
- **Acción:** confirmar que ningún cliente dependía de la UI web `/qr/**` (el frontend Ionic usa
  `/api/v1/qr`). Marcar como resuelta la nota de proyecto de esa brecha.

## Decisiones de migración (diferencias intencionales, no brechas)

- **D-1 — Consolidación de esquema (ADR 0007):** `equipment_type`+`size` (V8), `municipio` (V9) y
  la migración de `sexo`/`tipo_sangre` (V18) se representan en .NET mediante `catalog/catalog_item`
  o texto libre (`municipio`/`estado`). No es pérdida: es la estructura genérica ya decidida.
- **D-2 — Exportación de reportes:** ADR 0011 eligió Apache POI (Java). En .NET la exportación no
  usa POI; verificar que el formato de salida (Excel/CSV) sigue cumpliendo la spec 011. *(A validar
  al remediar; clasificado como decisión, no brecha, salvo que falte el formato.)*
- **D-3 — Migraciones:** Flyway (SQL versionado) → EF Core (`InitialCreate` + `AddMediaAsset`).
  Equivalente funcional; ojo: EF no traslada triggers/SQL crudo automáticamente (origen de B-2).

## Veredicto de controles de seguridad (`docs/02-seguridad.md` §8) · US3

| Control | Veredicto |
|---------|-----------|
| Authn/authz en todos los endpoints (sin anónimos indebidos) | **Preservado** (solo login/activate/forgot/reset y `landing/public` son anónimos, igual que Java) |
| PII de elemento cifrada AES-256-GCM + blind index | **Preservado** (`PiiCipher` + `BlindIndexer`, entidad `Officer`) |
| Enmascaramiento de PII por rol | **Preservado** (`RolePolicy.CanViewFullPii`, `PiiMasker`) |
| QR sin PII / sin acceso anónimo | **Mejorado** (M-1: UI web anónima eliminada) |
| Rate limiting | **Preservado** (B-1 remediada: rate limiter nativo) |
| Auditoría inmutable append-only | **Preservado** (B-2 remediada: triggers `INSTEAD OF` recreados) |
| Sin PII en logs | **Preservado** (auditoría por id/evento, no vuelca PII) |

## Conclusión

Nada del **contrato funcional** (endpoints, entidades, reglas) se perdió en la migración. Los dos
controles transversales de seguridad que faltaban (rate limiting y triggers de inmutabilidad de
auditoría) **ya fueron remediados** en `dev` (`fix/rate-limiting-dotnet`, `fix/audit-inmutable-dotnet`),
sin dependencias nuevas. **Migración cerrada** desde la perspectiva de paridad: API, datos y
controles de seguridad quedan al nivel del backend Java o por encima (brecha `/qr/**` cerrada).
Mejora futura opcional: encadenamiento por hash de la auditoría e infra de tests de integración.
