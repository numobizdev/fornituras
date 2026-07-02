---
description: "Task list — Migración backend ASP.NET Core (017)"
---

# Tasks: Migración backend → ASP.NET Core Web API

**Estado:** Implementación de código completada (2026-07-01). Pendiente: smoke tests manuales con SQL Server + Ionic.

---

## Phase 0: Gobernanza y scaffold ✅

- [X] T001 ADR `docs/04-decisiones/0016-backend-aspnetcore.md`
- [X] T002 Scaffold `fornituras-api-dotnet/`
- [X] T003 `global.json` SDK 10.0.202
- [X] T004 `ApiResponse<T>`, JSON camelCase
- [X] T005 `ExceptionHandlingMiddleware`
- [X] T006 Path base `/sigefor`, CORS, OpenAPI, health
- [X] T007 EF Core + migración `InitialCreate`
- [X] T008 [quickstart.md](./quickstart.md)

## Phase 1: Auth + seed ✅

- [X] T009–T015 Auth, JWT, seed admin
- [X] T016 `environment.ts` (mismo puerto 8080)
- [ ] T017 Smoke Ionic login *(manual)*

## Phase 2: Catálogos ✅

- [X] T018–T021 Catalog API + warehouses (ADR 0007)
- [ ] T022 Smoke Ionic Almacenes/Tipos *(manual)*

## Phase 3: Equipment ✅

- [X] T023–T026 Equipment CRUD/batch/status
- [ ] T027 Smoke Ionic Fornituras *(manual)*

## Phase 4: Officers + PII ✅

- [X] T028–T032 PiiCipher, BlindIndexer, OfficerService
- [ ] T033 Smoke Ionic Elementos *(manual)*

## Phase 5: Assignments ✅

- [X] T034–T037 AssignmentService
- [ ] T038 Smoke Ionic asignación *(manual)*

## Phase 6: Users + auth extendida ✅

- [X] T039–T042 Users, activate, change-password, EmailService

## Phase 7: QR REST ✅

- [X] T043–T046 LoteQr, PDF/ZIP export

## Phase 8: Cierre ✅

- [X] T047 `AGENTS.md`, `README.md`, `CLAUDE.md`
- [X] T048 `specs/README.md`
- [X] T050 `fornituras-api/OBSOLETE.md`
- [ ] T049 Smoke completo Ionic *(manual)*
- [X] T051 Constitution Check en plan.md

---

## Módulos adicionales (Ionic actual, más allá del contrato mínimo spec)

Implementados para paridad con Java actual:

- Dashboard, Transfers, Incidents, Alerts, Decommissions, Reports, Audit, Landing
