---
description: "Task list for Landing configurable + tour guiado"
---

# Tasks: Landing configurable + tour guiado

**Input**: Design documents from `/specs/016-landing-configurable/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/landing-api.md

**Tests**: SÍ se incluyen (el proyecto sigue TDD y ya tiene tests de contrato/integración
H2/MockMvc — ver ADR 0009). Escribir los tests antes de la implementación de cada historia.

**Organization**: agrupadas por historia de usuario para implementación y prueba independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1–US4 según spec.md
- Rutas base: backend `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/`;
  frontend `sigefor/src/app/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: prerrequisitos de gobernanza y dependencias.

- [ ] T001 Crear ADR `docs/04-decisiones/0011-landing-configurable.md` cubriendo: alta de
  dependencia `driver.js` (necesidad, licencia MIT, mantenimiento), endpoint público no-PII
  con rate limiting, y estrategia anti-XSS (texto plano + escape de Angular). **Bloquea** la
  implementación (Constitución VI).
- [ ] T002 [P] Añadir `driver.js@^1.6.0` a `sigefor/package.json` y `npm install`.
- [ ] T003 [P] Crear la estructura de paquetes del módulo backend en
  `modules/landing/{entity,dto,controller,service,repository,mapper}`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: núcleo compartido por todas las historias. **⚠️ Ninguna historia empieza hasta
completar esta fase.**

**Backend — persistencia y modelo**

- [ ] T004 Migración Flyway `fornituras-api/src/main/resources/db/migration/V19__create_landing.sql`:
  tabla `landing_section` (columnas de `data-model.md`) + seed por defecto (hero público,
  hero/aviso/accesos del home).
- [ ] T005 [P] Enums `LandingScope` (PUBLIC|HOME) y `LandingSectionType`
  (HERO|ANNOUNCEMENT|QUICK_LINKS|RICH_TEXT) en `modules/landing/entity/`.
- [ ] T006 [P] Entidad `LandingSection extends BaseEntity` en
  `modules/landing/entity/LandingSection.java` (campos y longitudes de `data-model.md`).
- [ ] T007 `LandingSectionRepository` en `modules/landing/repository/` con
  `findByScopeAndActiveTrueOrderByOrdenAsc` y `findByScopeOrderByOrdenAsc` (depende de T006).
- [ ] T008 [P] DTOs en `modules/landing/dto/`: `LandingSectionPublic`, `LandingSectionAdmin`,
  `QuickLinkItem`, `LandingSectionCreateRequest`, `LandingSectionUpdateRequest`,
  `ReorderRequest` (records, con anotaciones de validación).
- [ ] T009 `LandingSectionMapper` en `modules/landing/mapper/` (entidad↔DTO, serialización de
  `configJson`↔`QuickLinkItem[]`, proyección Public sin PII) (depende de T006, T008).
- [ ] T010 Esqueleto `LandingService` en `modules/landing/service/` (`@Transactional(readOnly=true)`,
  inyecta repo, mapper y `AuditWriter`) (depende de T007, T009).
- [ ] T011 Esqueleto `LandingController` (`@RequestMapping("/api/v1/landing")`) en
  `modules/landing/controller/` (depende de T010).

**Frontend — capa de datos compartida**

- [ ] T012 [P] Modelos `sigefor/src/app/features/landing/data/landing.model.ts`
  (`LandingSection`, `LandingScope`, `LandingSectionType`, `QuickLinkItem`, requests).
- [ ] T013 `sigefor/src/app/features/landing/data/landing.service.ts` con métodos
  `getPublic/getHome/listSections/createSection/updateSection/deactivateSection/reorder`
  (patrón `ApiResponse<T>` + `map(r => r.data)`, como `equipment.service.ts`) (depende de T012).

**Checkpoint**: fundamentos listos — pueden empezar las historias.

---

## Phase 3: User Story 1 — Home configurable (Priority: P1) 🎯 MVP

**Goal**: al iniciar sesión, `/inicio` muestra las secciones HOME activas (hero, avisos,
accesos rápidos) en orden, en vez del placeholder.

**Independent Test**: con el seed de la V19, iniciar sesión y ver el home poblado y ordenado;
desactivar una sección (por BD/seed) y comprobar que desaparece tras recargar.

### Tests for User Story 1 ⚠️

- [ ] T014 [P] [US1] Test de contrato/integración de `GET /api/v1/landing/home` en
  `fornituras-api/src/test/.../modules/landing/` (autenticado → 200 con HOME activas ordenadas;
  sin sesión → 401).

### Implementation for User Story 1

- [ ] T015 [US1] `LandingService.getHome()` (secciones HOME activas ordenadas → `LandingSectionPublic[]`)
  en `modules/landing/service/LandingService.java`.
- [ ] T016 [US1] Endpoint `GET /landing/home` con `@PreAuthorize("isAuthenticated()")` en
  `modules/landing/controller/LandingController.java`.
- [ ] T017 [US1] Reescribir `sigefor/src/app/features/inicio/` para cargar `getHome()` y
  renderizar dinámicamente por tipo de sección (hero/aviso/accesos), con **interpolación**
  (sin `innerHTML`).
- [ ] T018 [US1] Estado vacío coherente + navegación de los accesos rápidos en la página de
  inicio (`features/inicio/`).

**Checkpoint**: US1 funcional y testeable con datos sembrados (MVP).

---

## Phase 4: User Story 2 — Edición por ADMIN (Priority: P1)

**Goal**: un ADMIN da de alta/edita/reordena/activa/desactiva secciones desde un editor en la
app; los cambios se reflejan para los usuarios. No-ADMIN no accede.

**Independent Test**: como ADMIN crear/editar/reordenar/desactivar una sección y ver el efecto;
como CAPTURISTA, el editor está denegado; inyectar `<script>` y ver que se muestra literal.

### Tests for User Story 2 ⚠️

- [ ] T019 [P] [US2] Tests de contrato/integración del CRUD `/landing/sections` en
  `fornituras-api/src/test/.../modules/landing/` (403 sin ADMIN; 201/200 con ADMIN; 400 en
  validación: URL peligrosa `javascript:`, `QUICK_LINKS` sin items, longitudes excedidas).

### Implementation for User Story 2

- [ ] T020 [US2] Métodos de escritura en `LandingService` (`list(scope)`, `create`, `update`,
  `deactivate`, `reorder`) con `@Transactional` + `audit.record(...)` (quién/qué/cuándo).
- [ ] T021 [US2] Endpoints admin en `LandingController` (`GET /sections?scope=`, `POST /sections`,
  `PUT /sections/{id}`, `PATCH /sections/{id}/deactivate`, `PATCH /sections/reorder`) con
  `@PreAuthorize("hasRole('ADMIN')")`.
- [ ] T022 [US2] Validación en el borde: anotaciones en los request DTOs + validador de esquema
  de URL (`http/https` o ruta interna `/`; rechaza `javascript:`/`data:`) en `modules/landing/dto/`.
- [ ] T023 [P] [US2] `sigefor/src/app/core/guards/admin.guard.ts` (rol ADMIN vía
  `AuthService.currentUser`).
- [ ] T024 [US2] Página editor `sigefor/src/app/features/landing/pages/landing-admin/`
  (lista por scope, alta/edición con formulario, activar/desactivar, reordenar) usando las
  páginas de catálogos como plantilla.
- [ ] T025 [US2] Ruta de `landing-admin` bajo `admin.guard` (en `features/landing/*.routes.ts`
  y `app.routes.ts`) + ítem de menú admin en
  `sigefor/src/app/core/constants/app-navigation.ts` (visible solo a ADMIN).

**Checkpoint**: US1 + US2 funcionan; el contenido del home es editable.

---

## Phase 5: User Story 3 — Landing pública pre-login (Priority: P2)

**Goal**: sin sesión, se muestra una landing pública con contenido PUBLIC y botón "Acceder";
la respuesta pública no expone PII y está limitada por tasa.

**Independent Test**: sin sesión ver la landing y llegar al login con "Acceder"; usuario
autenticado es redirigido a `/inicio`; `GET /public` no devuelve PII y responde 429 al abusar.

### Tests for User Story 3 ⚠️

- [ ] T026 [P] [US3] Test de contrato/integración de `GET /api/v1/landing/public` en
  `fornituras-api/src/test/.../modules/landing/` (sin auth → 200 solo PUBLIC activas y sin PII;
  429 al exceder el rate limit).

### Implementation for User Story 3

- [ ] T027 [US3] Añadir `GET /api/v1/landing/public` a `permitAll` en
  `fornituras-api/src/main/java/.../security/SecurityConfig.java`.
- [ ] T028 [US3] Aplicar rate limiting Bucket4j al endpoint público reutilizando el patrón de
  `by-codigo` (ver ADR 0010).
- [ ] T029 [US3] `LandingService.getPublic()` + endpoint `GET /landing/public` en el controller.
- [ ] T030 [US3] Página `sigefor/src/app/features/landing/pages/public-landing/` (renderiza
  PUBLIC, botón "Acceder" → `/login`, estado vacío).
- [ ] T031 [US3] Rutas en `sigefor/src/app/app.routes.ts`: landing pública bajo `guestGuard`,
  arranque de invitados hacia la landing, y redirección a `/inicio` si ya hay sesión (sin
  romper `authGuard`).

**Checkpoint**: US1 + US2 + US3 funcionan de forma independiente.

---

## Phase 6: User Story 4 — Tour guiado de primera vez (Priority: P3)

**Goal**: la primera visita a `/inicio` lanza un recorrido guiado (driver.js) que resalta
hero, accesos rápidos y menú; no se repite solo; se puede relanzar con "Ver tutorial".

**Independent Test**: primera visita lanza el tour; tras verlo, no se relanza solo; "Ver
tutorial" lo reinicia.

### Tests for User Story 4 ⚠️

- [ ] T032 [P] [US4] Spec de `sigefor/src/app/core/tour/tour.service.spec.ts` (arranca y limpia
  el driver; respeta el flag de primera visita).

### Implementation for User Story 4

- [ ] T033 [US4] `sigefor/src/app/core/tour/tour.service.ts` — wrapper de driver.js
  (crear/arrancar/destruir con limpieza) y `startHomeTour(steps)`.
- [ ] T034 [US4] Flag de "tour visto" en Capacitor Preferences: nueva clave en
  `sigefor/src/app/core/constants/storage-keys.ts` (p. ej. `sigefor.tour.home.done`) leída por
  el `tour.service`.
- [ ] T035 [US4] Integrar en `features/inicio/`: lanzar el tour en la primera visita y añadir
  botón "Ver tutorial" que lo relanza.
- [ ] T036 [US4] Importar el CSS de driver.js en `sigefor/src/global.scss` y tematizar el
  popover con la paleta GOBMX (guinda `#611232`) en `sigefor/src/theme/`.

**Checkpoint**: las 4 historias completas.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T037 [P] Spec `sigefor/src/app/features/landing/data/landing.service.spec.ts` (mapea
  `ApiResponse`).
- [ ] T038 [P] Tests unitarios backend del `LandingSectionMapper` y del validador de URL en
  `fornituras-api/src/test/.../modules/landing/`.
- [ ] T039 Ejecutar la validación de `quickstart.md` y el **checklist de seguridad** (sin PII en
  `/public`, XSS escapado, edición solo-ADMIN, rate limiting activo, escrituras auditadas).
- [ ] T040 [P] Actualizar documentación (`docs/` y nota de feature) según lo implementado.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Fase 1)**: sin dependencias. T001 (ADR) **bloquea** el resto por Constitución VI.
- **Foundational (Fase 2)**: depende de Setup. **Bloquea** todas las historias.
- **US1 (Fase 3)**: depende de Fase 2. Testeable con el seed sin US2.
- **US2 (Fase 4)**: depende de Fase 2. Independiente de US1 (edita datos que US1 muestra).
- **US3 (Fase 5)**: depende de Fase 2. Independiente de US1/US2.
- **US4 (Fase 6)**: depende de Fase 2 y de que exista el home de US1 (resalta sus zonas).
- **Polish (Fase 7)**: depende de las historias deseadas.

### Within Each User Story

- Los tests se escriben y fallan antes de implementar.
- Backend: modelo → repositorio → servicio → endpoint. Frontend: servicio de datos → página.

### Parallel Opportunities

- Fase 1: T002 y T003 en paralelo (tras/junto a T001).
- Fase 2: T005, T006, T008, T012 en paralelo; luego T007/T009; T013 tras T012.
- Tras Fase 2, US1/US2/US3 pueden avanzar en paralelo (distintos equipos). US4 tras US1.
- Los tests marcados [P] de cada historia corren en paralelo.

---

## Implementation Strategy

### MVP First (US1)

1. Fase 1 (Setup, con ADR) → 2. Fase 2 (Foundational) → 3. Fase 3 (US1).
4. **Validar** el home con datos sembrados. 5. Demo si procede.

### Incremental Delivery

Foundational → **US1 (MVP, home con seed)** → US2 (edición ADMIN) → US3 (landing pública) →
US4 (tour). Cada historia agrega valor sin romper las previas.

---

## Notes

- La rama de trabajo es `016-landing-configurable` (branch por spec; se conserva tras fusionar
  a `dev`).
- Reutilizar: `BaseEntity`, `ApiResponse`, `AuditWriter`, Bucket4j (`by-codigo`, ADR 0010),
  patrón de tests H2/MockMvc (ADR 0009), servicios Angular y páginas de catálogos.
- Seguridad no negociable: cara pública sin PII, contenido escapado (sin `innerHTML`),
  edición solo-ADMIN, escrituras auditadas.
