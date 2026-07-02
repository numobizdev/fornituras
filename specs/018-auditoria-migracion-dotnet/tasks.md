---
description: "Task list — 018 Auditoría de cobertura de la migración a .NET"
---

# Tasks: Auditoría de cobertura de la migración a .NET (specs 001–017)

**Input**: Design documents from `/specs/018-auditoria-migracion-dotnet/`

**Prerequisites**: plan.md, spec.md, research.md, findings.md, checklists/cobertura-migracion.md

**Tests**: la Fase A (auditoría) es de solo lectura, sin tests. La Fase B (remediación) no puede
probarse por integración con la infra actual (xUnit unitario + EF InMemory); se cubre por unidad lo
aislable y se valida manualmente (quickstart) — decisión registrada en research.md.

**Organization**: por historia de usuario (US1/US2/US3 = auditoría, ya hecha) y por brecha
(B-1/B-2 = remediación, pendiente). Cada brecha va en **su rama de fix** (AGENTS.md §5.8).

Paths base: `api = fornituras-api-dotnet/src/Fornituras.Api` · `itest = fornituras-api-dotnet/tests/Fornituras.Api.Tests`

---

## Phase 1: Setup

- [X] T001 Confirmar el alcance (specs 001–017 vs `fornituras-api-dotnet/`) y el backend Java (`fornituras-api/`) como referencia — ver spec.md.
- [X] T002 Preparar la matriz de cobertura `specs/018-auditoria-migracion-dotnet/checklists/cobertura-migracion.md`.

## Phase 2: Foundational (recolección de evidencia)

- [X] T003 Inventariar endpoints y autorización del backend .NET (grep `[Http*]`/`[Authorize]` en `api/Controllers`).
- [X] T004 Inventariar endpoints del backend Java de referencia (`fornituras-api/**/*Controller.java`).
- [X] T005 Comparar esquemas: migración EF `InitialCreate`/`AddMediaAsset` vs Flyway `V1..V25`.

---

## Phase 3: User Story 1 - Matriz de cobertura spec→.NET (P1) — HECHA

- [X] T006 [US1] Completar una fila por spec 001–017 con estado (C/P/A/FE) y evidencia en `findings.md` (§Matriz de cobertura).
- [X] T007 [US1] Reflejar el estado por spec en `checklists/cobertura-migracion.md`.

**Checkpoint**: cobertura documentada — todas las specs revisadas (SC-001).

## Phase 4: User Story 2 - Detección de pérdidas vs Java (P1) — HECHA

- [X] T008 [US2] Contrastar endpoints Java↔.NET; registrar diferencias en `findings.md`. Hallazgo: solo `QrWebController` sin equivalente (M-1).
- [X] T009 [US2] Contrastar esquema de datos (tablas/columnas/índices/enums); registrar consolidaciones intencionales (D-1, ADR 0007).
- [X] T010 [US2] Clasificar diferencias como **brecha** (severidad) o **decisión de migración** en `findings.md`.

**Checkpoint**: toda diferencia clasificada (SC-002).

## Phase 5: User Story 3 - Seguridad transversal preservada (P1) — HECHA

- [X] T011 [US3] Verificar authn/authz por endpoint y superficies anónimas (login/activate/forgot/reset, `landing/public`).
- [X] T012 [US3] Verificar cifrado de PII (`PiiCipher` + `BlindIndexer`) y enmascaramiento por rol.
- [X] T013 [US3] Recorrer `docs/02-seguridad.md` §8 sobre .NET y emitir veredictos en `findings.md`.
- [X] T014 [US3] Confirmar la brecha `/qr/**`: **cerrada** en .NET (no existe `QrWebController`) → mejora M-1.

**Resultado Fase A** (findings.md): 🔴 **B-1** rate limiting no portado; 🔴 **B-2** triggers de inmutabilidad de `audit_log` no portados; 🟢 **M-1** brecha `/qr/**` cerrada.

---

## Phase 6: Remediación B-1 — Rate limiting (ADR 0010) · rama `fix/rate-limiting-dotnet`

**Goal**: restaurar el rate limiting perdido en la migración con el limitador nativo de ASP.NET Core.

- [X] T015 Crear `api/Configuration/RateLimitOptions.cs` (`ByCodigo` 30/60s, `Public` 60/60s) y `RateLimitPolicies` (nombres); añadir `RateLimit` a `api/Configuration/AppOptions.cs`.
- [X] T016 Registrar el limitador en `api/Extensions/ServiceCollectionExtensions.cs`: `AddRateLimiter` con políticas `by-codigo` (partición por actor) y `public` (partición por IP), `OnRejected` → **429** con `ApiResponse`.
- [X] T017 Añadir `app.UseRateLimiter()` en `api/Program.cs` (tras `UseAuthentication`/`UseAuthorization`, antes de `MapControllers`).
- [X] T018 Aplicar `[EnableRateLimiting("by-codigo")]` en `EquipmentController.GetByCodigo` y `[EnableRateLimiting("public")]` en `LandingController.GetPublic` (`api/Controllers`).
- [X] T019 Añadir la sección `App:RateLimit` a `api/appsettings.json`.
- [X] T020 [P] Test unitario aislable en `itest/RateLimitTests.cs` (resolución de opciones/partición); documentar validación manual (quickstart §B).
- [X] T021 `dotnet build` + `dotnet test` verdes; commit en `fix/rate-limiting-dotnet` y merge a `dev`.

**Checkpoint**: B-1 cerrada — `by-codigo` y `landing/public` responden 429 al exceder el límite.

## Phase 7: Remediación B-2 — Inmutabilidad de auditoría (ADR 0012) · rama `fix/audit-inmutable-dotnet`

**Goal**: restaurar la garantía append-only de `audit_log` a nivel de BD.

- [X] T022 Crear migración EF `AddAuditImmutabilityTriggers` (`dotnet ef migrations add`) con `migrationBuilder.Sql(...)` que cree `trg_audit_log_no_update` y `trg_audit_log_no_delete` (`INSTEAD OF UPDATE/DELETE`); `Down` los elimina.
- [X] T023 Verificar que el snapshot EF no cambia el esquema (solo triggers) y que `InitialCreate` sigue intacto.
- [X] T024 `dotnet build` + `dotnet test` verdes; documentar validación manual en SQL Server (quickstart §C); commit en `fix/audit-inmutable-dotnet` y merge a `dev`.

**Checkpoint**: B-2 cerrada — `UPDATE`/`DELETE` sobre `audit_log` rechazados por la BD.

---

## Phase 8: Polish & Cross-Cutting

- [X] T025 Actualizar `findings.md` marcando B-1 y B-2 como **remediadas** (con commits/ramas) y la nota de proyecto de la brecha `/qr/**` como resuelta (M-1).
- [X] T026 [P] Actualizar la memoria del proyecto (estado de la auditoría 018 y cierre de brechas).
- [X] T027 Confirmar que la Fase A no tocó código de producción (SC-005) y que la remediación no altera el contrato Ionic (quickstart §D).

---

## Dependencies & Execution Order

- **Fase A (Ph1–Ph5)**: completada (auditoría no destructiva).
- **B-1 (Ph6)** y **B-2 (Ph7)**: independientes entre sí; pueden ir en paralelo en ramas de fix distintas.
- **Polish (Ph8)**: tras cerrar B-1 y B-2.

### Within B-1
T015 → T016 → T017 → T018 → T019 → T020 → T021 (secuencial salvo T020 [P]).

### Parallel Opportunities
- B-1 y B-2 en paralelo (ramas y archivos distintos).

---

## Implementation Strategy

1. **Fase A**: ✅ hecha — findings.md es el entregable de la auditoría.
2. **Remediación**: B-1 y B-2, cada una en su rama de fix, merge independiente a `dev`.
3. **Cierre**: actualizar findings/memoria y validar no-regresión del contrato.
