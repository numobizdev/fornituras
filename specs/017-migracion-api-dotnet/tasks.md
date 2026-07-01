---
description: "Task list — Migración backend ASP.NET Core (017)"
---

# Tasks: Migración backend → ASP.NET Core Web API

**Input**: Design documents from `specs/017-migracion-api-dotnet/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [data-model.md](./data-model.md),
[contracts/ionic-api-contract.md](./contracts/ionic-api-contract.md)

> **Alcance:** migración **completa** del backend REST. Fase 1 desbloquea login Ionic; Fases 2–5
> completan pantallas core; Fases 6–8 cierran paridad y gobernanza.

**Tests**: unitarios por servicio crítico; smoke manual contra Ionic tras cada gate.

**Organization**: por fase de migración; tareas `[P]` pueden paralelizarse.

## Path Conventions

- **.NET API**: `<dotnet>/` = `fornituras-api-dotnet/`
- **Java (referencia)**: `<java>/` = `fornituras-api/`
- **Frontend**: `<fe>/` = `sigefor/`

---

## Phase 0: Gobernanza y scaffold

**Goal**: Proyecto .NET compilable con infraestructura transversal.

- [ ] T001 Crear ADR en `docs/04-decisiones/` documentando cambio Spring Boot → ASP.NET Core Web API (.NET 10)
- [ ] T002 [P] Scaffold solución `fornituras-api-dotnet/` (`dotnet new sln`, Web API, xUnit)
- [ ] T003 [P] Añadir `global.json` fijando SDK 10.0.202
- [ ] T004 Implementar `ApiResponse<T>` y serialización JSON (camelCase) en `<dotnet>/`
- [ ] T005 Implementar middleware de excepciones global (400/401/403/404/409/500) en `<dotnet>/`
- [ ] T006 Configurar path base `/sigefor`, CORS, Swagger (Development), health check
- [ ] T007 Configurar EF Core + SQL Server; primera migración vacía o `users` mínima
- [ ] T008 Documentar User Secrets en [quickstart.md](./quickstart.md)

**Checkpoint**: `dotnet build` OK; Swagger accesible en `/sigefor/swagger`.

---

## Phase 1: Auth + seed (🎯 primer hito — login Ionic)

**Goal**: Login JWT compatible con `<fe>/core/services/auth.service.ts`.

**Independent Test**: Ionic login con admin seed contra API .NET.

### Tests

- [ ] T009 [P] Tests unitarios: hash BCrypt, generación JWT, login válido/inválido
- [ ] T010 [P] Test: seed admin idempotente

### Implementation

- [ ] T011 Entidades `User`, `VerificationToken`, `PasswordResetToken` + migración EF
- [ ] T012 `AuthService`: login, forgot-password, reset-password (email log-only en dev)
- [ ] T013 `AuthController`: endpoints `/api/v1/auth/*` según contrato
- [ ] T014 JWT Bearer middleware + policies `ADMIN`, `CAPTURISTA`
- [ ] T015 Seed admin configurable (`App:Seed:Admin`) en startup
- [ ] T016 Actualizar `<fe>/src/environments/environment.ts` con URL .NET (si cambia puerto)
- [ ] T017 Smoke: login + logout + 401 en Ionic

**Checkpoint**: **Login Ionic funciona** — primer hito completado.

---

## Phase 2: Catálogos (P1 — Almacenes y Tipos en Ionic)

**Goal**: warehouses, equipment-types, sizes, municipios, sexos, tipos-sangre.

- [ ] T018 [P] Entidades + migraciones: `Warehouse`, `EquipmentType`, `Size`, `Municipio`, `Sexo`, `TipoSangre`
- [ ] T019 [P] Services + controllers con paginación Spring-compatible
- [ ] T020 RBAC: ADMIN para mutaciones de catálogo; GET autenticado para todos
- [ ] T021 Tests unitarios: normalización de nombres, deactivate
- [ ] T022 Smoke Ionic: pantallas Almacenes y Tipos

**Checkpoint**: catálogos operativos desde Ionic.

---

## Phase 3: Equipment / inventario (P1 — Fornituras)

**Goal**: CRUD fornituras, batch, status, by-codigo, filtros.

- [ ] T023 Entidad `Equipment` + migración; enums `EquipmentStatus`, `ExpiryStatus`
- [ ] T024 `EquipmentService`: CRUD, batch, changeStatus, specifications/filtros
- [ ] T025 `EquipmentController`: endpoints según [contrato Ionic](./contracts/ionic-api-contract.md)
- [ ] T026 Tests: lifecycle, expiry calculator, by-codigo 404
- [ ] T027 Smoke Ionic: pantalla Fornituras

**Checkpoint**: inventario operativo desde Ionic.

---

## Phase 4: Officers + PII (P1 — Elementos)

**Goal**: padrón con cifrado, blind index, enmascaramiento por rol.

- [ ] T028 Implementar `PiiCipher` (AES-256-GCM) y `BlindIndexer` (HMAC) en `<dotnet>/`
- [ ] T029 Entidad `Officer` + migración; relaciones municipio/sexo/tipo_sangre
- [ ] T030 `OfficerService`: create, list (filtros), detail (masking por rol)
- [ ] T031 `OfficerController` + `OfficerCatalogController` (`/sexos`, `/tipos-sangre`)
- [ ] T032 Tests: round-trip cifrado, búsqueda blind index, PiiMasker
- [ ] T033 Smoke Ionic: pantalla Elementos

**Checkpoint**: padrón operativo con PII protegida.

---

## Phase 5: Assignments (P1 — Asignación)

**Goal**: assign, return, reassign; concurrencia 409.

- [ ] T034 Entidad `Assignment` + índice único asignación vigente por equipment
- [ ] T035 `AssignmentService`: assign, return, reassign; actualiza estado equipment
- [ ] T036 `AssignmentController`: endpoints según contrato
- [ ] T037 Tests: 409 concurrente, return idempotente, reassign transaccional
- [ ] T038 Smoke Ionic: flujo completo asignación

**Checkpoint**: **paridad Ionic P1 completa** (todas las pantallas core).

---

## Phase 6: Users + auth extendida (P2)

**Goal**: paridad `UserController` y auth no cableada en Ionic aún.

- [ ] T039 `UserService` + `UserController`: GET/POST users, GET by id
- [ ] T040 Endpoints `/auth/activate`, `/auth/change-password`
- [ ] T041 EmailService (SMTP configurable; log en dev)
- [ ] T042 Tests: RBAC users, activate token expiry

**Checkpoint**: paridad auth/users Java.

---

## Phase 7: QR REST (P3)

**Goal**: lotes, PDF, ZIP — paridad `modules/qrcodes`.

- [ ] T043 Entidad `LoteQr` + migración; generador secuencial con lock
- [ ] T044 `QrController`: POST/GET lotes, export pdf/zip
- [ ] T045 Servicios PDF/ZIP (QRCoder + librería PDF)
- [ ] T046 Tests: formato `FOR-XXXXXX`, unicidad, layout PDF básico

**Checkpoint**: módulo QR REST operativo.

---

## Phase 8: Cierre de migración

**Goal**: documentación, gobernanza, retiro Java.

- [ ] T047 Actualizar `AGENTS.md`, `README.md`, `CLAUDE.md` (stack .NET)
- [ ] T048 Actualizar `specs/README.md` con estado de migración
- [ ] T049 Smoke test completo Ionic (login → asignación) documentado en quickstart
- [ ] T050 Marcar `<java>/` obsoleto o eliminar tras sign-off
- [ ] T051 Re-verificar Constitution Check en [plan.md](./plan.md)

**Checkpoint**: migración cerrada.

---

## Resumen de gates

| Gate | Fase | Criterio |
|------|------|----------|
| Build | 0 | Compila + Swagger |
| **Login Ionic** | 1 | Primer hito |
| Catálogos | 2 | Almacenes + Tipos |
| Inventario | 3 | Fornituras |
| PII | 4 | Elementos |
| **Paridad Ionic P1** | 5 | Asignación end-to-end |
| Auth extendida | 6 | Users + activate |
| QR | 7 | Lotes PDF/ZIP |
| **Migración completa** | 8 | Docs + retiro Java |
