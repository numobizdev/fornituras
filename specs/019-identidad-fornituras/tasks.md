# Tasks: Identidad del sistema — Sistema Integral de Gestión de Fornituras

**Input**: Design documents from `specs/019-identidad-fornituras/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: tareas agrupadas por user story para permitir implementación y prueba
independientes. No se generan tests nuevos (cambio de textos/marca); se actualizan los
existentes que asuman los textos antiguos.

## Phase 1: Setup

- [X] T001 Registrar la línea base: buscar en el repo las apariciones de "Sistema de Gestión de Blindajes", "Gobierno de México" y "Contenido de bienvenida" (excluyendo `Planeacion.md`, `fornituras-api/` obsoleto y specs históricas) y confirmar la lista de archivos a tocar contra plan.md

## Phase 2: Foundational (bloqueante — principio VI de la constitución)

- [X] T002 Crear ADR `docs/04-decisiones/0019-identidad-sigefor.md`: identidad oficial "Sistema Integral de Gestión de Fornituras", revierte la justificación del commit `6b99f21` (fix/landing-hero-title) y de la V24 del backend Java obsoleto; alcance nombre/marca (no dominio); enlazarlo en `docs/04-decisiones/README.md` si existe índice

**Checkpoint**: decisión documentada — las user stories pueden ejecutarse

## Phase 3: User Story 1 — Visitante ve la nueva identidad en la landing pública (P1) 🎯 MVP

**Goal**: hero, footer y pestaña del navegador muestran la nueva identidad; desaparece "Gobierno de México"

**Independent Test**: abrir la landing sin sesión y validar hero/footer/pestaña (quickstart #1–#4)

- [X] T003 [P] [US1] Cambiar `<title>` a "SIGEFOR | Sistema Integral de Gestión de Fornituras" en `sigefor/src/index.html`
- [X] T004 [P] [US1] Actualizar fallbacks `heroTitle()` → "Sistema Integral de Gestión de Fornituras" y `heroSubtitle()` → subtítulo coherente con fornituras en `sigefor/src/app/features/landing/pages/public-landing/public-landing.page.ts`
- [X] T005 [P] [US1] Actualizar footer a "SIGEFOR · Sistema Integral de Gestión de Fornituras" en `sigefor/src/app/features/landing/pages/public-landing/public-landing.page.html`
- [X] T006 [US1] Buscar y actualizar tests/specs del frontend que asuman los textos antiguos de US1 (`sigefor/src/**/*.spec.ts`, p. ej. specs de `public-landing`)

**Checkpoint**: US1 completa y verificable por sí sola (con API caída incluso, vía fallbacks)

## Phase 4: User Story 2 — Instalaciones existentes reciben el nuevo título (P2)

**Goal**: BD sembradas con el título antiguo se actualizan; ediciones del administrador se respetan; instalaciones nuevas nacen con el nuevo título

**Independent Test**: quickstart #5–#6 (migración sobre valor sembrado / sobre valor editado)

- [X] T007 [P] [US2] Actualizar la semilla del HERO público a "Sistema Integral de Gestión de Fornituras" en `fornituras-api-dotnet/src/Fornituras.Api/Data/DataSeeder.cs` (método `SeedLandingAsync`)
- [X] T008 [US2] Crear migración EF Core de datos `UpdateLandingHeroTitleToFornituras` (`dotnet ef migrations add` en `fornituras-api-dotnet/src/Fornituras.Api`) con el `UPDATE` acotado de data-model.md (idempotente, respeta ediciones; `Down` restaura el valor anterior con el filtro inverso)
- [X] T009 [US2] Buscar y actualizar tests del backend que asuman el título antiguo (`fornituras-api-dotnet/tests/**`), y ejecutar `dotnet test` en `fornituras-api-dotnet/`

**Checkpoint**: US2 completa — `dotnet ef database update` deja la BD con la nueva identidad

## Phase 5: User Story 3 — El ADMIN encuentra el módulo de configuración de la landing (P3)

**Goal**: la entrada de menú se llama "Configurar landing"; gating ADMIN intacto

**Independent Test**: quickstart #7–#8 (menú como ADMIN / como rol no-ADMIN)

- [X] T010 [US3] Renombrar la entrada `'Contenido de bienvenida'` → `'Configurar landing'` en `sigefor/src/app/core/constants/app-navigation.ts` (conservar `url`, `icon` y `roles: ['ADMIN']`), y actualizar tests que referencien la etiqueta antigua

**Checkpoint**: US3 completa — módulo descubrible, mismo control de acceso

## Phase 6: Polish & Cross-Cutting

- [X] T011 [P] Actualizar `README.md` (título → "Sistema Integral de Gestión de Fornituras (SIGEFOR)")
- [X] T012 [P] Enmienda PATCH de `.specify/memory/constitution.md` (título nominal + versión 1.0.0 → 1.0.1 con nota en el Sync Impact Report)
- [X] T013 [P] Actualizar mención del nombre en `.github/copilot-instructions.md` si aplica
- [X] T014 Verificación final: grep de "Gobierno de México" y "Sistema de Gestión de Blindajes" (solo deben quedar en `Planeacion.md`, backend Java obsoleto, specs/ADRs históricos y el propio ADR 0019); ejecutar `dotnet test` y `npm test`; validar quickstart.md end-to-end

## Dependencies

- T001 → T002 → (US1 | US2 | US3 en cualquier orden; son independientes)
- Dentro de US1: T003, T004, T005 en paralelo → T006
- Dentro de US2: T007 → T008 → T009 (la migración asume el seeder ya actualizado para coherencia)
- Polish (T011–T014) al final; T011–T013 en paralelo → T014

## Implementation Strategy

- **MVP**: Phase 1–3 (US1) — la cara pública ya muestra la identidad correcta en instalaciones nuevas y vía fallbacks.
- **Incremento 2**: US2 — corrige las instalaciones existentes (el caso reportado por el usuario).
- **Incremento 3**: US3 + Polish — descubribilidad del módulo y documentación canónica.
