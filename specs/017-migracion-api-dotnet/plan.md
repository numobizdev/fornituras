# Implementation Plan: Migración backend → ASP.NET Core Web API

**Branch**: `017-migracion-api-dotnet` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/017-migracion-api-dotnet/spec.md`

## Summary

Reimplementar el backend REST de SIGEFOR en **`fornituras-api-dotnet/`** (ASP.NET Core Web API,
.NET 10) manteniendo el **contrato HTTP** que `sigefor/` ya consume. La base de datos y usuarios
son desechables: EF Core migrations desde cero, seed de admin nuevo, sin compatibilidad con datos
Java. El backend Java permanece como referencia de comportamiento hasta el cierre.

Entrega por fases: **Fase 1** (auth + login Ionic) desbloquea validación temprana; **Fases 2–5**
completan paridad Ionic; **Fases 6–7** añaden usuarios, email y QR REST; **Fase 8** documenta y
retira Java.

## Technical Context

**Language/Version**: C# / **.NET 10** (SDK 10.0.202).

**Primary Dependencies**: ASP.NET Core Web API, EF Core 10 + SQL Server provider, JWT Bearer
(`Microsoft.AspNetCore.Authentication.JwtBearer`), BCrypt.Net-Next, Swashbuckle/OpenAPI, QRCoder +
librería PDF (QuestPDF o equivalente para fase QR).

**Storage**: SQL Server 2022 local. Esquema vía **EF Core migrations** (referencia lógica: Flyway
V1–V14 en `fornituras-api/src/main/resources/db/migration/`).

**Testing**: xUnit + Moq; tests de integración opcionales con SQL Server local o Testcontainers.

**Target Platform**: API REST local (Windows dev); despliegue futuro TBD.

**Project Type**: Monorepo — nuevo `fornituras-api-dotnet/` junto a `sigefor/` (sin cambios de stack
frontend).

**Performance Goals**: Respuestas paginadas < 2 s en dev; login < 500 ms local.

**Constraints**: `ApiResponse<T>` obligatorio; path base `/sigefor`; PII según ADR 0006; QR sin PII;
secretos fuera del repo; paginación compatible con Spring Data.

**Scale/Scope**: ~11 controllers REST, ~50 endpoints; 7 servicios HTTP en Ionic como consumidor
principal.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse al cierre.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | PII cifrada, RBAC, JWT, sin secretos en repo | ✅ (diseño) |
| II. QR sin PII | Códigos opacos `FOR-XXXXXX`; datos en servidor tras auth | ✅ |
| III. Stack acordado | Requiere ADR (cambio Spring → .NET) | ⚠️ ADR pendiente |
| IV. Documentación en español | Spec/plan/tasks en español; código en inglés | ✅ |
| V. No secretos versionados | User Secrets + env vars | ✅ |
| VI. Dependencias justificadas | NuGet estándar; evaluar PDF/QR libs | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/017-migracion-api-dotnet/
├── spec.md
├── plan.md              # Este archivo
├── tasks.md             # Tareas ordenadas por fase
├── data-model.md        # Entidades EF ↔ tablas
├── quickstart.md        # Cómo levantar y probar
└── contracts/
    └── ionic-api-contract.md
```

### Source Code (repository root)

```text
fornituras-api-dotnet/
├── Fornituras.Api.sln
├── src/
│   ├── Fornituras.Api/              # Host Web API, controllers, middleware, Program.cs
│   ├── Fornituras.Application/      # Services, DTOs, interfaces, validators
│   ├── Fornituras.Domain/           # Entities, enums, domain rules
│   └── Fornituras.Infrastructure/   # EF Core, JWT, crypto PII, email, QR
└── tests/
    └── Fornituras.Application.Tests/
```

**Alternativa v1 (más simple):** proyecto único `Fornituras.Api/` con carpetas `Features/<modulo>/`.
Migrar a capas cuando crezca.

### Referencia Java (comportamiento)

| Área | Ruta |
|------|------|
| Controllers | `fornituras-api/src/main/java/.../modules/*/controller/` |
| Services | `fornituras-api/src/main/java/.../modules/*/service/` |
| Security | `fornituras-api/.../security/` |
| PII crypto | `fornituras-api/.../common/crypto/` |
| Tests | `fornituras-api/src/test/java/.../*Test.java` |

## Fases de implementación

| Fase | Contenido | Gate / hito |
|------|-----------|-------------|
| 0 | ADR, scaffold, ApiResponse, CORS, Swagger, health, path base | Build OK |
| 1 | Auth + seed + JWT | **Login Ionic** |
| 2 | Catálogos (warehouses, types, sizes, municipios, sexos, sangre) | Pantallas Almacenes/Tipos |
| 3 | Equipment (CRUD, batch, status, by-codigo) | Pantalla Fornituras |
| 4 | Officers + PII crypto/masking | Pantalla Elementos |
| 5 | Assignments (assign, return, reassign, 409) | Pantalla Asignación |
| 6 | Users + email (activate, change-password) | Paridad auth extendida |
| 7 | QR REST (lotes, PDF, ZIP) | Paridad módulo qrcodes |
| 8 | Docs, `environment.ts`, retirar Java | Migración cerrada |

**Estimación**: 3–5 semanas (1 dev .NET familiarizado con el dominio).

## Configuración clave

| Setting | Valor / notas |
|---------|---------------|
| Path base | `/sigefor` |
| Puerto dev | Configurable (Java usa 8080; .NET puede usar 8080 u otro + actualizar `apiUrl`) |
| JWT expiration | 86400000 ms (24 h) |
| QR prefix | `FOR-`, sequence length 6 |
| CORS | `localhost:4200`, `localhost:8100`, `capacitor://localhost` |
| BD | `Server=localhost;Database=fornituras;TrustServerCertificate=True` |

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| JSON distinto a Spring (enums, fechas) | `System.Text.Json` explícito; comparar con respuestas Java |
| Paginación incompatible | DTO `Page<T>` manual, no el default ASP.NET |
| PII mal portada | Tests round-trip; ADR 0006 + `docs/02-seguridad.md` |
| Scope creep | Priorizar [contrato Ionic](./contracts/ionic-api-contract.md) |
| .NET 10 reciente | `global.json` con SDK 10.0.202 |

## Entregables al cierre

- [ ] `fornituras-api-dotnet/` operativo
- [ ] ADR stack .NET en `docs/04-decisiones/`
- [ ] `AGENTS.md` / `README.md` / `CLAUDE.md` actualizados
- [ ] `sigefor/src/environments/*.ts` con `apiUrl` correcto
- [ ] `fornituras-api/` marcado obsoleto o eliminado
