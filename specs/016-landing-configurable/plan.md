# Implementation Plan: Landing configurable + tour guiado

**Branch**: `016-landing-configurable` | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/016-landing-configurable/spec.md`

## Summary

Añadir un **motor de contenido configurable** para dos caras de bienvenida que comparten
modelo y edición: (1) una **landing pública** pre-login con branding institucional y acceso al
login, y (2) un **home post-login** (`/inicio`) que reemplaza el placeholder actual con hero,
avisos y accesos rápidos ordenables. El contenido lo administra el rol **ADMIN** desde un
editor CRUD dentro de la app, se persiste en SQL Server (nuevo módulo `modules/landing`) y se
sirve vía API. Se suma un **tour guiado** del home en la primera visita (librería `driver.js`).

Enfoque técnico: nuevo módulo backend siguiendo la arquitectura por feature
(`controller/service/repository/entity/dto/mapper`), reutilizando `BaseEntity`, `ApiResponse`,
`AuditWriter`, el patrón de seguridad de `CatalogController` y el filtro **Bucket4j** existente
(rate limiting de `by-codigo`). En el frontend, nuevo feature standalone `features/landing`
(servicio de datos, landing pública, home dinámico, editor admin) + un `admin.guard` de rol y
un `tour.service` que envuelve `driver.js`. La cara pública **no expone PII** y todo el
contenido se renderiza como **texto auto-escapado** (anti-XSS).

## Technical Context

**Language/Version**: Java 21 (Spring Boot) backend; TypeScript 5.9 / Angular 20.3 + Ionic 8
frontend.

**Primary Dependencies**: Backend — Spring Web, Spring Security, Spring Data JPA, Flyway,
Bucket4j (ya presente). Frontend — `@ionic/angular` 8, `@capacitor/preferences` 8, y **nueva**
`driver.js ^1.6.0` (tour guiado, MIT, sin dependencias).

**Storage**: Microsoft SQL Server 2022. Nueva tabla `landing_section` vía migración Flyway
`V19`. Flag de "tour visto" en Capacitor Preferences (cliente).

**Testing**: Backend — JUnit + MockMvc + H2 (patrón de contrato/integración existente).
Frontend — Karma/Jasmine (specs de servicio).

**Target Platform**: API REST (contenedor/servidor); app Ionic (web/PWA + Capacitor móvil).

**Project Type**: Web application (monorepo: `fornituras-api/` backend + `sigefor/` frontend).

**Performance Goals**: Home visible < 2 s en red normal (SC-006); endpoints de lectura de
landing con respuesta típica < 300 ms.

**Constraints**: Cara pública **sin PII** y limitada por tasa; contenido renderizado como texto
(sin `innerHTML` de contenido de usuario); idioma español; paleta institucional GOBMX;
edición restringida a ADMIN; auditar escrituras sin filtrar PII.

**Scale/Scope**: Corporación policial (cientos–miles de usuarios). Alcance acotado: 1 tabla
nueva, 1 módulo backend, 1 feature frontend, ~4 tipos de sección, 1 tour del home.

## Constitution Check

*GATE: Debe pasar antes de Phase 0. Re-verificado tras Phase 1.*

| Principio | Cumplimiento en este feature |
|-----------|------------------------------|
| **I. Seguridad y privacidad primero** | Cara pública sin PII; contenido como texto auto-escapado (anti stored-XSS); rate limiting en el endpoint público; se cita `docs/02-seguridad.md` y se añade ADR. ✅ |
| **II. QR nunca expone PII** | No aplica (el feature no toca QR). N/A |
| **III. Cero secretos** | No se introducen secretos ni credenciales; nada que versionar. ✅ |
| **IV. Mínimo privilegio y authz** | Lectura pública solo de contenido no sensible; `/home` autenticado; CRUD solo `ADMIN` (`@PreAuthorize`); validación de entrada en el borde; rechazo por defecto. ✅ |
| **V. Trazabilidad sin fugas** | Altas/ediciones/bajas de secciones auditadas con `AuditWriter` (quién/qué/cuándo); el contenido no es PII, así que auditar/loguear no filtra datos personales. ✅ |
| **VI. ADR y stack congelado** | Stack sin cambios (Angular/Ionic + Spring). Nueva dependencia `driver.js` y decisiones de exposición pública/anti-XSS → **ADR obligatorio** en `docs/04-decisiones/` antes de implementar. ✅ (condicionado al ADR) |

**Puerta:** PASA, con la condición de crear el ADR (dependencia `driver.js` + endpoint público
no-PII + saneo anti-XSS) como primera tarea de implementación.

> Nota de roles: la constitución menciona `ADMIN/SUPERVISOR/OPERADOR` como ejemplo, pero el
> código real define `ADMIN` y `CAPTURISTA` (`modules/users/entity/Role.java`). Este plan usa
> los roles reales: **ADMIN** edita; **CAPTURISTA** solo consume el home.

## Project Structure

### Documentation (this feature)

```text
specs/016-landing-configurable/
├── plan.md              # Este archivo
├── research.md          # Phase 0 — decisiones técnicas (driver.js, anti-XSS, rutas)
├── data-model.md        # Phase 1 — entidad LandingSection y reglas
├── quickstart.md        # Phase 1 — guía de validación end-to-end
├── contracts/           # Phase 1 — contrato REST de /api/v1/landing
│   └── landing-api.md
└── tasks.md             # Phase 2 — /speckit-tasks (no lo crea /speckit-plan)
```

### Source Code (repository root)

```text
fornituras-api/src/main/java/com/numobiz/solutions/fornituras/
├── modules/landing/
│   ├── entity/        # LandingSection, LandingScope, LandingSectionType
│   ├── dto/           # LandingSectionPublic, LandingSectionAdmin, Create/UpdateRequest, QuickLinkItem
│   ├── controller/    # LandingController (público + home + CRUD admin)
│   ├── service/       # LandingService (@Transactional, audit en escrituras)
│   ├── repository/    # LandingSectionRepository
│   └── mapper/        # LandingSectionMapper
├── security/SecurityConfig.java        # permitAll GET /api/v1/landing/public
└── (rate limiting Bucket4j reutilizado del patrón by-codigo)

fornituras-api/src/main/resources/db/migration/
└── V19__create_landing.sql             # tabla landing_section + seed por defecto

sigefor/src/app/
├── features/landing/
│   ├── data/          # landing.service.ts, landing.model.ts
│   ├── pages/public-landing/           # landing pública pre-login
│   └── pages/landing-admin/            # editor CRUD (solo ADMIN)
├── features/inicio/   # home dinámico (reemplaza placeholder) + botón "Ver tutorial"
├── core/
│   ├── tour/tour.service.ts            # wrapper driver.js (ciclo de vida + flag 1ª visita)
│   ├── guards/admin.guard.ts           # nuevo guard de rol ADMIN
│   └── constants/app-navigation.ts     # ítem de menú admin (solo ADMIN)
├── app.routes.ts                       # ruta pública + ajuste de arranque para invitados
├── global.scss                         # import CSS driver.js
└── theme/                              # popover del tour tematizado GOBMX

docs/04-decisiones/
└── 00XX-landing-configurable.md        # ADR: driver.js + endpoint público no-PII + anti-XSS
```

**Structure Decision**: Web application (Opción 2) sobre el monorepo existente. Se respeta la
arquitectura por feature del backend y el patrón standalone/señales del frontend; no se crean
capas ni proyectos nuevos, solo un módulo backend y un feature frontend.

## Complexity Tracking

> Sin violaciones de constitución que justificar. El feature reutiliza patrones existentes
> (arquitectura por módulo, `ApiResponse`, `AuditWriter`, Bucket4j, servicios Angular) y añade
> una única dependencia (`driver.js`) que se justifica en el ADR.
