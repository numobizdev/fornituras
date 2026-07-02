# Implementation Plan: Auditoría de cobertura de la migración a .NET (specs 001–017)

**Branch**: `018-auditoria-migracion-dotnet` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/018-auditoria-migracion-dotnet/spec.md`

## Summary

Verificar que las specs **001–017** estén aplicadas en el backend `fornituras-api-dotnet/` y que
la migración Java→.NET **no perdiera nada**. La **auditoría ya se ejecutó** (resultado en
[findings.md](./findings.md)): API y datos con **paridad 1:1**, **2 brechas Alta** (B-1 rate
limiting, B-2 inmutabilidad de auditoría) y **1 mejora** (M-1: brecha anónima `/qr/**` cerrada).
Este plan cierra el ciclo: formaliza la metodología de auditoría (Fase A, hecha) y planifica la
**remediación** de B-1 y B-2 (Fase B), cada una en su rama de fix.

## Technical Context

**Language/Version**: C# / .NET 10 (ASP.NET Core Web API). Referencia de comparación: Java 17 +
Spring Boot (`fornituras-api/`, obsoleto).

**Primary Dependencies**:
- Auditoría (Fase A): solo lectura del código; sin dependencias.
- B-1 rate limiting: **rate limiter nativo** de ASP.NET Core
  (`Microsoft.AspNetCore.RateLimiting` / `System.Threading.RateLimiting`, ya en el runtime); **sin
  nueva dependencia** (reemplaza a Bucket4j de Java, ADR 0010).
- B-2 inmutabilidad: **EF Core** `migrationBuilder.Sql(...)` para recrear los triggers
  `INSTEAD OF UPDATE/DELETE` sobre `audit_log` (ADR 0012). Sin nueva dependencia.

**Storage**: SQL Server 2022. B-2 añade triggers sobre la tabla existente `audit_log` (sin cambio
de columnas). B-1 no toca almacenamiento.

**Testing**: xUnit (`dotnet test`). La verificación de comportamiento de rate limiting y de los
triggers requiere SQL Server / host de integración (no hay infra de integración en el repo hoy);
se documenta la validación manual en quickstart y se cubre por unidad lo que sea aislable
(partición/opciones del limitador; presencia de la migración).

**Target Platform**: API ASP.NET Core (Windows/servidor, path base `/sigefor`, puerto 8080).

**Project Type**: Web application (monorepo: `fornituras-api-dotnet/` backend + `sigefor/` frontend).
Esta feature es de **backend** (auditoría + remediación); sin cambios de frontend.

**Performance Goals**: N/A (la auditoría no corre en runtime; el rate limiter debe añadir overhead
despreciable por petición).

**Constraints**: la auditoría es **no destructiva** (SC-005: 0 cambios en producción durante la
Fase A). La remediación no debe alterar el contrato funcional existente (solo añade 429 por límite
y refuerza la BD).

**Scale/Scope**: 18 controllers / 18 tablas auditados; 2 brechas a remediar.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento |
|-----------|--------------|
| **I. Seguridad y privacidad primero** | La feature **restaura** controles de seguridad perdidos en la migración; su objetivo es no dejar huecos. Cita `docs/02-seguridad.md` §8. ✅ |
| **II. QR nunca expone PII** | No cambia el QR; la auditoría **confirma** M-1 (brecha anónima `/qr/**` cerrada). ✅ |
| **III. Cero secretos en el repo** | Sin secretos nuevos; límites de rate por configuración (`App:RateLimit`). ✅ |
| **IV. Mínimo privilegio y autorización** | B-1 restaura el **rate limiting / anti-abuso** (endurecimiento de API). ✅ |
| **V. Trazabilidad y auditoría sin fugas** | B-2 restaura la **inmutabilidad append-only** de `audit_log` (ADR 0012, ISO 27001). ✅ |
| **VI. ADR y stack congelado** | B-1 y B-2 **re-materializan** decisiones ya aprobadas (ADR 0010 y 0012) en el stack .NET (ADR 0016); **sin dependencias nuevas** (rate limiter y triggers nativos). ✅ |

**Resultado del gate**: PASA. Sin violaciones; no aplica Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/018-auditoria-migracion-dotnet/
├── plan.md                       # Este archivo
├── spec.md                       # QUÉ/POR QUÉ de la auditoría
├── research.md                   # Fase 0: decisiones de remediación (B-1/B-2)
├── quickstart.md                 # Validación de la auditoría y de las remediaciones
├── findings.md                   # Resultado de la auditoría ejecutada (Fase A)
├── checklists/
│   └── cobertura-migracion.md    # Matriz spec-por-spec (001–017)
└── tasks.md                      # Fase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
fornituras-api-dotnet/src/Fornituras.Api/
├── Configuration/RateLimitOptions.cs        # B-1: App:RateLimit (by-codigo, public)
├── Extensions/ServiceCollectionExtensions.cs# B-1: AddRateLimiter (políticas + OnRejected 429)
├── Program.cs                               # B-1: app.UseRateLimiter()
├── Controllers/EquipmentController.cs       # B-1: [EnableRateLimiting("by-codigo")] en by-codigo
├── Controllers/LandingController.cs         # B-1: [EnableRateLimiting("public")] en public
├── appsettings.json                         # B-1: sección App:RateLimit
└── Data/Migrations/*_AddAuditImmutabilityTriggers.cs  # B-2: triggers INSTEAD OF UPDATE/DELETE

fornituras-api-dotnet/tests/Fornituras.Api.Tests/
└── RateLimitTests.cs / AuditImmutability notes  # lo aislable por unidad + notas de validación
```

**Structure Decision**: Web application; backend .NET por capas. La remediación toca configuración,
wiring de DI, `Program.cs`, dos controllers y una migración EF. Cada brecha se implementa en **su
rama de fix** (`fix/rate-limiting-dotnet` para B-1, `fix/audit-inmutable-dotnet` para B-2) y se
fusiona a `dev` por separado (AGENTS.md §5.8).

## Complexity Tracking

> Sin violaciones de la constitución. No aplica.
