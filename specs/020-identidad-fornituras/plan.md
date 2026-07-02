# Implementation Plan: Identidad del sistema — Sistema Integral de Gestión de Fornituras

**Branch**: `020-identidad-fornituras` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/020-identidad-fornituras/spec.md`

## Summary

Rebrand del nombre visible del sistema a **"Sistema Integral de Gestión de Fornituras"**
(expansión oficial del acrónimo SIGEFOR), eliminación de "Gobierno de México" del título del
documento, y renombrado de la entrada de menú del editor de landing a "Configurar landing".
Técnicamente: cambios de texto en el frontend (Ionic/Angular), actualización del seeder y una
**migración EF Core de datos** acotada al valor sembrado anterior para instalaciones
existentes, más actualización de documentación canónica y un ADR que revierte la decisión del
commit `6b99f21`.

## Technical Context

**Language/Version**: C# / .NET 10 (backend), TypeScript / Angular 20 + Ionic 8 (frontend)

**Primary Dependencies**: ASP.NET Core Web API, EF Core (migraciones), Angular standalone components

**Storage**: SQL Server 2022 — tabla `landing_section` (columnas `scope` nvarchar(10),
`type` nvarchar(20), `titulo` nvarchar(160), `updated_at` datetime2), creada en la migración
`20260701235121_InitialCreate`

**Testing**: `dotnet test` (xUnit, backend), `npm test` (Karma/Jasmine, frontend)

**Target Platform**: Web (Ionic serve / despliegue institucional), API en puerto 8080 path `/sigefor`

**Project Type**: Web application (backend `fornituras-api-dotnet/` + frontend `sigefor/`)

**Performance Goals**: N/A — cambio de contenido/branding sin impacto de rendimiento

**Constraints**: la migración de datos debe ser idempotente y no sobrescribir ediciones
deliberadas del administrador (se acota al valor exacto sembrado anterior)

**Scale/Scope**: ~6 archivos de código, 1 migración EF Core, 4 documentos, 1 ADR

## Constitution Check

*GATE: verificación contra los principios I–VI de `.specify/memory/constitution.md` (v1.0.0).*

| Principio | Cumplimiento |
|-----------|--------------|
| I. Seguridad y privacidad primero | ✅ No se toca PII, auth ni QR. Solo textos de marca y una fila de contenido público no sensible. |
| II. QR sin datos personales | ✅ No aplica — el QR no se modifica. |
| III. Cero secretos en el repo | ✅ No se introducen secretos; la migración solo contiene texto público. |
| IV. Mínimo privilegio y authz | ✅ El gating ADMIN del módulo de landing **se conserva sin cambios** (menú `roles: ['ADMIN']` + `adminGuard` + `[Authorize(Roles = ManageLanding)]`). Solo cambia la etiqueta visible. |
| V. Trazabilidad sin fugas | ✅ La migración actualiza `updated_at`; no se loguea PII. |
| VI. Decisiones documentadas (ADR) | ✅ Se registra **ADR 0020** (identidad del sistema) que revierte la justificación del commit `6b99f21`. La constitución recibe una enmienda PATCH (solo su título nominal). |
| Migraciones versionadas | ✅ El cambio de datos va en una migración EF Core, nunca manual. |

**Resultado**: PASS — sin violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/020-identidad-fornituras/
├── spec.md              # Especificación
├── plan.md              # Este archivo
├── research.md          # Fase 0 — decisiones y alternativas
├── data-model.md        # Fase 1 — entidad landing_section y regla de actualización
├── quickstart.md        # Fase 1 — guía de validación end-to-end
├── checklists/
│   └── requirements.md  # Checklist de calidad de la spec
└── tasks.md             # Fase 2 (/speckit-tasks)
```

*(Sin `contracts/`: esta feature no crea ni modifica interfaces — los endpoints de landing
de la spec 016 quedan intactos; ver `specs/016-landing-configurable/contracts/landing-api.md`.)*

### Source Code (repository root)

```text
fornituras-api-dotnet/src/Fornituras.Api/
├── Data/
│   ├── DataSeeder.cs                         # Título sembrado del HERO público (línea ~238)
│   └── Migrations/
│       └── <timestamp>_UpdateLandingHeroTitleToFornituras.cs   # NUEVA migración de datos

sigefor/src/
├── index.html                                # <title> (quita "Gobierno de México")
└── app/
    ├── core/constants/app-navigation.ts      # Etiqueta de menú "Configurar landing"
    └── features/landing/pages/public-landing/
        ├── public-landing.page.ts            # Fallbacks heroTitle()/heroSubtitle()
        └── public-landing.page.html          # Footer

README.md                                     # Título del proyecto
.specify/memory/constitution.md               # Título nominal (enmienda PATCH)
.github/copilot-instructions.md               # Mención del nombre
docs/04-decisiones/0020-identidad-sigefor.md  # NUEVO ADR
```

**Structure Decision**: web application existente (backend + frontend en monorepo); no se
crean módulos nuevos — solo se editan textos, un seeder, una migración de datos y documentación.

## Fase 0 — Research

Ver [research.md](./research.md). Decisión clave: la actualización de datos se implementa como
**migración EF Core con `migrationBuilder.Sql`** (patrón heredado de la V24 de Flyway del
backend Java obsoleto), acotada con `WHERE scope='PUBLIC' AND type='HERO' AND titulo='Sistema
de Gestión de Blindajes'` para garantizar idempotencia y respeto a ediciones del administrador.

## Fase 1 — Design

Ver [data-model.md](./data-model.md) (sin cambios de esquema; solo regla de actualización de
datos) y [quickstart.md](./quickstart.md) (validación end-to-end).

**Re-check Constitution post-diseño**: PASS — el diseño no añadió superficies nuevas.

## Complexity Tracking

Sin violaciones — tabla no requerida.
