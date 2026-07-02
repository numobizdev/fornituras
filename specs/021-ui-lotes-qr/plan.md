# Implementation Plan: UI de lotes QR (SUPER_ADMIN)

**Branch**: `021-ui-lotes-qr` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/021-ui-lotes-qr/spec.md`

## Summary

Portar a Ionic las cuatro pantallas del módulo QR del backend Java obsoleto (generar, listar,
detalle/reimprimir, éxito), consumiendo la API .NET existente (`/api/v1/qr/**`). Introducir el
rol **`SUPER_ADMIN`** (QR-only) en backend y frontend; restringir API, menú, rutas y login
redirect. Registrar decisión en [ADR 0020](../../docs/04-decisiones/0020-rol-super-admin-qr.md).

## Technical Context

**Language/Version**: C# / .NET 10 (ASP.NET Core Web API); TypeScript 5.9 / Angular 20 + Ionic 8.

**Primary Dependencies**: Backend — JWT Bearer, EF Core, QRCoder (QR existente). Frontend —
`@ionic/angular`, `HttpClient` (blob downloads).

**Storage**: SQL Server 2022 — tablas `lote_qr` / `codigo_qr` ya existentes (spec 002).

**Testing**: Backend — xUnit (`Fornituras.Api.Tests`). Frontend — compilación + flujo manual
(quickstart).

**Target Platform**: API en contenedor/servidor; app Ionic web/PWA + Capacitor.

**Constraints**: QR sin PII; JWT obligatorio; solo `SUPER_ADMIN` en módulo QR; español en UI.

## Constitution Check

| Principio | Cumplimiento |
|-----------|--------------|
| I. Seguridad primero | Rol dedicado; API autenticada; sin UI anónima |
| II. QR sin PII | Sin cambio al formato; heredado de spec 002 |
| III. Cero secretos | Seed dev documentado en quickstart, no en repo |
| IV. Mínimo privilegio | SUPER_ADMIN QR-only; guards en cliente y servidor |
| V. Trazabilidad | `GENERATE_QR_BATCH` ya auditado en `LoteQrService` |

**Puerta:** PASA.

## Project Structure

### Documentation

```text
specs/021-ui-lotes-qr/
├── spec.md
├── plan.md
├── tasks.md
├── quickstart.md
└── checklists/requirements.md
```

### Source Code

```text
fornituras-api-dotnet/src/Fornituras.Api/
├── Data/Entities/Enums.cs          # + SUPER_ADMIN
├── Security/RolePolicy.cs          # + ManageQrLotes
├── Controllers/QrController.cs     # Authorize ManageQrLotes
└── Data/DataSeeder.cs              # + seed SUPER_ADMIN dev

sigefor/src/app/
├── core/guards/super-admin.guard.ts
├── core/guards/operational.guard.ts
├── core/constants/app-navigation.ts
├── features/qr-lotes/
│   ├── qr-lotes.routes.ts
│   ├── data/qr-lotes.service.ts
│   └── pages/ (generar, list, detail, exito)
└── app.routes.ts
```

## Phase 0 — Backend RBAC

- Añadir `SUPER_ADMIN` a enum `Role`.
- `RolePolicy.ManageQrLotes = "SUPER_ADMIN"`.
- Cambiar `[Authorize]` en `QrController`.
- Seed usuario dev `superadmin@fornituras.local`.
- Test unitario de política de roles.

## Phase 1 — Frontend infra

- Tipo `UserRole` + `role-options` + guards.
- Rutas `/qr-lotes/**` con `superAdminGuard`.
- `operationalGuard` en rutas operativas.
- Login/guest redirect para SUPER_ADMIN.
- Menú filtrado.

## Phase 2 — Páginas Ionic (paridad Java)

| Pantalla Ionic | Java Thymeleaf | API |
|----------------|----------------|-----|
| `qr-lotes-list` | `lotes.html` | GET `/lotes` |
| `qr-lote-generar` | `generar.html` | POST `/lotes` |
| `qr-lote-detail` | `lote-detalle.html` | GET `/lotes/{id}`, export |
| `qr-lote-exito` | `exito.html` | GET `/lotes/{id}/pdf\|zip` |

Descargas: `responseType: 'blob'` + `triggerDownload()` (patrón reportes).

## Phase 3 — Documentación

- Actualizar `specs/002-qr-equipos/contracts/qr-api.md` (autorización SUPER_ADMIN).
- Quickstart manual en `specs/021-ui-lotes-qr/quickstart.md`.
