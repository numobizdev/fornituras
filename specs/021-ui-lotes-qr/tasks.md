# Tasks: UI de lotes QR (SUPER_ADMIN)

**Input**: Design documents from `/specs/021-ui-lotes-qr/`

**Prerequisites**: plan.md, spec.md, ADR 0021

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear rama `021-ui-lotes-qr` desde `dev` y directorio `specs/021-ui-lotes-qr/`
- [X] T002 Redactar `spec.md`, `plan.md`, checklist y ADR 0021 (originalmente 0020; renumerado por colisión)

## Phase 2: Foundational (Blocking Prerequisites)

- [X] T003 [P] Añadir `SUPER_ADMIN` a `Role` enum en [`Enums.cs`](../../fornituras-api-dotnet/src/Fornituras.Api/Data/Entities/Enums.cs)
- [X] T004 [P] Añadir `ManageQrLotes` en [`RolePolicy.cs`](../../fornituras-api-dotnet/src/Fornituras.Api/Security/RolePolicy.cs)
- [X] T005 Cambiar autorización en [`QrController.cs`](../../fornituras-api-dotnet/src/Fornituras.Api/Controllers/QrController.cs)
- [X] T006 Seed usuario SUPER_ADMIN dev en [`DataSeeder.cs`](../../fornituras-api-dotnet/src/Fornituras.Api/Data/DataSeeder.cs)
- [X] T007 [P] Añadir `SUPER_ADMIN` en [`auth.model.ts`](../../sigefor/src/app/core/models/auth.model.ts) y [`role-options.ts`](../../sigefor/src/app/features/usuarios/data/role-options.ts)
- [X] T008 [P] Crear [`super-admin.guard.ts`](../../sigefor/src/app/core/guards/super-admin.guard.ts) y [`operational.guard.ts`](../../sigefor/src/app/core/guards/operational.guard.ts)
- [X] T009 Integrar guards, rutas y menú en [`app.routes.ts`](../../sigefor/src/app/app.routes.ts), [`app-navigation.ts`](../../sigefor/src/app/core/constants/app-navigation.ts), [`login.page.ts`](../../sigefor/src/app/features/auth/pages/login/login.page.ts), [`guest.guard.ts`](../../sigefor/src/app/core/guards/guest.guard.ts), [`app.component.ts`](../../sigefor/src/app/app.component.ts)

## Phase 3: User Story 1 — Generar lote (P1)

- [X] T010 [US1] Crear [`qr-lotes.service.ts`](../../sigefor/src/app/features/qr-lotes/data/qr-lotes.service.ts) y modelos
- [X] T011 [US1] Implementar [`qr-lote-generar.page`](../../sigefor/src/app/features/qr-lotes/pages/qr-lote-generar/) con formulario y overlay de carga

## Phase 4: User Story 2 — Listar lotes (P1)

- [X] T012 [US2] Implementar [`qr-lotes-list.page`](../../sigefor/src/app/features/qr-lotes/pages/qr-lotes-list/)

## Phase 5: User Story 3 — Detalle y reimprimir (P1)

- [X] T013 [US3] Implementar [`qr-lote-detail.page`](../../sigefor/src/app/features/qr-lotes/pages/qr-lote-detail/) con export original y reimpresión

## Phase 6: User Story 4 — Éxito (P2)

- [X] T014 [US4] Implementar [`qr-lote-exito.page`](../../sigefor/src/app/features/qr-lotes/pages/qr-lote-exito/) con auto-descarga

## Phase 7: Polish

- [X] T015 [P] Tests [`QrRolePolicyTests.cs`](../../fornituras-api-dotnet/tests/Fornituras.Api.Tests/QrRolePolicyTests.cs)
- [X] T016 Actualizar [`qr-api.md`](../002-qr-equipos/contracts/qr-api.md) y [`specs/README.md`](../README.md)
- [X] T017 Redactar [`quickstart.md`](./quickstart.md)

## Dependencies

- Phase 2 blocks all user stories.
- US1 before US4 (éxito tras generar).
- US2/US3 pueden paralelizarse tras US1 service.

## Parallel execution examples

```bash
# Tras Phase 2:
T012 (list page) || T013 (detail page)  # diferentes archivos
T003 + T007  # backend enum + frontend type en paralelo
```
