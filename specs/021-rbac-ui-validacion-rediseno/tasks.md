# Tasks: Visibilidad coherente por rol, validación visible y rediseño de Login/Asignación

**Input**: Design documents from `/specs/021-rbac-ui-validacion-rediseno/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-permissions.md, quickstart.md

**Tests**: Incluidos (TDD estricto — estilo global del proyecto/usuario: test primero, Red-Green-Refactor).

**Organization**: Tareas agrupadas por user story para permitir implementación y prueba independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1..US6 según spec.md

## Phase 1: Setup

- [X] T001 Verificar rama `021-rbac-ui-validacion-rediseno` activa y líneas base en verde: `dotnet test fornituras-api-dotnet` y `npm test -- --watch=false --browsers=ChromeHeadless` en `sigefor/` (registrar fallos preexistentes si los hay)

## Phase 2: Foundational (bloquea US1, US2, US4, US5)

Matriz espejo + `hasAnyRole` + etiquetas de rol: piezas transversales que consumen todas las historias de visibilidad.

- [X] T002 [P] Test (rojo) de la matriz espejo en `sigefor/src/app/core/security/role-policy.spec.ts`: cada constante contiene exactamente los roles del contrato `contracts/ui-permissions.md` (WRITE_INVENTORY, WRITE_TRANSFERS, WRITE_OPERATIONS, AUTHORIZE_DECOMMISSION, WRITE_OFFICERS, MANAGE_CONFIG, MANAGE_LANDING, MANAGE_USERS, READ_AUDIT)
- [X] T003 [P] Test (rojo) de `hasAnyRole` en `sigefor/src/app/core/services/auth.service.spec.ts` (nuevo): usuario con rol incluido → true; rol no incluido → false; sin sesión → false; rol no reconocido → false (rechazo por defecto, FR-007)
- [X] T004 Crear `sigefor/src/app/core/security/role-policy.ts` con las constantes tipadas `ReadonlyArray<UserRole>` espejo de `RolePolicy.cs` (nota cruzada de sincronización en ambos archivos: comentario en role-policy.ts y en `fornituras-api-dotnet/src/Fornituras.Api/Security/RolePolicy.cs`)
- [X] T005 Añadir `hasAnyRole(roles: ReadonlyArray<UserRole>): boolean` a `sigefor/src/app/core/services/auth.service.ts` (verde para T003)
- [X] T006 [P] Crear `sigefor/src/app/core/constants/role-labels.ts` (ADMIN→"Administrador", SUPERVISOR→"Supervisor", ALMACEN→"Almacén", AUDITOR→"Auditor", CAPTURISTA→"Capturista"; rol desconocido → cadena vacía)

**Checkpoint**: `role-policy.spec` y `auth.service.spec` en verde.

## Phase 3: User Story 1 — El administrador recupera el control total (P1) 🎯 MVP

**Goal**: la cuenta admin sembrada siempre queda ADMIN+habilitada; el menú muestra el rol del usuario.

**Independent Test**: arrancar API (seeder corrige), login con admin sembrado → 13 módulos, FABs de Almacenes/Tipos visibles, rol "Administrador" en el menú.

- [X] T007 [P] [US1] Tests (rojo) del seeder *ensure* en `fornituras-api-dotnet/tests/Fornituras.Api.Tests/Data/DataSeederEnsureAdminTests.cs` (seguir el arnés existente del proyecto de tests): existe-con-rol-CAPTURISTA → queda ADMIN; existe-deshabilitado → queda habilitado; existe-correcto → sin cambios; no-existe → se crea ADMIN; `Seed.Admin.Enabled=false` o email vacío → no-op
- [X] T008 [US1] Implementar *ensure* en `SeedAdminAsync` de `fornituras-api-dotnet/src/Fornituras.Api/Data/DataSeeder.cs`: corregir `Role`/`Enabled` de la cuenta configurada si difieren, `LogWarning` de corrección sin credenciales/PII (verde para T007)
- [X] T009 [P] [US1] Test (rojo) en `sigefor/src/app/app.component.spec.ts`: con sesión activa, el menú muestra la etiqueta del rol del usuario (usar `role-labels`)
- [X] T010 [US1] Mostrar rol en el encabezado del menú: `sigefor/src/app/app.component.ts` (exponer etiqueta vía `computed` + `role-labels`) y `sigefor/src/app/app.component.html` (bajo nombre/email; estilo discreto en `sigefor/src/app/app.component.scss`)

**Checkpoint**: `dotnet test` verde con los casos nuevos; menú muestra rol. US1 entregable por sí sola.

## Phase 4: User Story 2 — Cada rol ve exactamente lo que puede hacer (P1)

**Goal**: menú y acciones derivan de la matriz espejo; flags reactivos (`computed`).

**Independent Test**: login con cada rol → visibilidad idéntica a la "Matriz de verificación por rol" de `contracts/ui-permissions.md`.

- [X] T011 [P] [US2] Test (rojo) del filtro de menú por rol en `sigefor/src/app/app.component.spec.ts`: ADMIN ve 13 ítems; AUDITOR ve Bitácora de Auditoría; CAPTURISTA no ve Usuarios/Landing/Auditoría
- [X] T012 [US2] `sigefor/src/app/core/constants/app-navigation.ts`: derivar `roles` de `role-policy.ts` (Auditoría→READ_AUDIT [+AUDITOR], Usuarios→MANAGE_USERS, Landing→MANAGE_LANDING) — verde para T011
- [X] T013 [P] [US2] Migrar Fornituras a matriz reactiva: `sigefor/src/app/features/fornituras/pages/fornituras/fornituras.page.ts` `canWrite = computed(hasAnyRole(WRITE_INVENTORY))` (+ ajustar template si el flag pasa a señal) con test de visibilidad por rol en spec nuevo del page o del flag
- [X] T014 [P] [US2] Migrar Elementos a `WRITE_OFFICERS` (computed) en `sigefor/src/app/features/elementos/pages/elementos/elementos.page.ts`
- [X] T015 [P] [US2] Migrar Asignación a `WRITE_OPERATIONS` (computed) en `sigefor/src/app/features/asignacion/pages/asignacion/asignacion.page.ts`
- [X] T016 [P] [US2] Migrar Incidencias a `WRITE_OPERATIONS` (computed) en `sigefor/src/app/features/incidencias/pages/incidencias/incidencias.page.ts`
- [X] T017 [P] [US2] Migrar Traslados a `WRITE_TRANSFERS` (computed) en `sigefor/src/app/features/traslados/pages/traslados/traslados.page.ts`
- [X] T018 [P] [US2] Migrar Bajas a `AUTHORIZE_DECOMMISSION` (computed) en `sigefor/src/app/features/bajas/pages/bajas/bajas.page.ts` (CAPTURISTA deja de ver un botón que el servidor rechaza)
- [X] T019 [P] [US2] Migrar Almacenes y Tipos a `MANAGE_CONFIG` (computed) en `sigefor/src/app/features/almacenes/pages/almacenes/almacenes.page.ts` y `sigefor/src/app/features/tipos/pages/tipos/tipos.page.ts` (sin cambio de roles; elimina el ad-hoc `isAdmin`)
- [X] T020 [US2] Barrido final: `grep hasRole(` en `sigefor/src` — fuera de `auth.service` y `admin.guard` no deben quedar combos ad-hoc; `admin.guard.ts` pasa a `hasAnyRole(MANAGE_USERS/MANAGE_LANDING)` según ruta

**Checkpoint**: matriz de verificación por rol cumplida en tests; US1+US2 = corrección completa del reporte original.

## Phase 5: User Story 3 — Cero credenciales en el repositorio (P1)

**Goal**: sin secretos versionados; entorno local sigue funcionando; rotación documentada.

**Independent Test**: `git ls-files` sin `appsettings.Development.json`; `.example` presente sin valores reales; API arranca.

- [X] T021 [US3] Retirar el archivo del control de versiones: `git rm --cached fornituras-api-dotnet/src/Fornituras.Api/appsettings.Development.json` + entrada en `.gitignore` (el archivo local permanece en disco)
- [X] T022 [P] [US3] Crear `fornituras-api-dotnet/src/Fornituras.Api/appsettings.Development.json.example` con la MISMA estructura y placeholders `__SET_ME__` (ConnectionStrings:Default, App:Seed:Admin:*, App:Cors) — sin ningún valor real
- [X] T023 [P] [US3] Documentar en `fornituras-api-dotnet/README.md` (crear sección si no existe): configuración local (copiar `.example` o `dotnet user-secrets`), nombres de variables, y **advertencia de rotación** de las credenciales expuestas en el historial (BD remota y admin sembrado)

**Checkpoint**: quickstart escenario 6 pasa.

## Phase 6: User Story 4 — Validación visible en formularios (P2)

**Goal**: componente `app-field-errors` aplicado a todos los formularios; incidencia con Reactive Forms.

**Independent Test**: guardar vacío en cada formulario → mensajes es-MX por campo; CURP/RFC con formato inválido → mensaje específico.

- [X] T024 [P] [US4] Test (rojo) del componente en `sigefor/src/app/core/forms/field-errors.component.spec.ts`: control válido → nada; required+touched → "Este campo es obligatorio."; email/maxlength/pattern → mensajes de `data-model.md`; `patternMessage` sobreescribe; `role="alert"` presente
- [X] T025 [US4] Implementar `sigefor/src/app/core/forms/field-errors.component.ts` (standalone, OnPush, inputs `control`/`label`/`patternMessage`) — verde para T024
- [X] T026 [US4] Estilos compartidos en `sigefor/src/global.scss`: clase `.field-error` (derivada de `.auth-page__field-error`) + mover `.form-actions` y `.state-message` a global
- [X] T027 [P] [US4] Aplicar `<app-field-errors>` en Fornituras: `sigefor/src/app/features/fornituras/pages/fornitura-form/fornitura-form.page.html` (+imports en .ts) y `fornitura-lote/fornitura-lote.page.html`
- [X] T028 [P] [US4] Aplicar en Elemento con `patternMessage` de CURP/RFC: `sigefor/src/app/features/elementos/pages/elemento-form/elemento-form.page.html`
- [X] T029 [P] [US4] Aplicar en Almacén: `sigefor/src/app/features/almacenes/pages/almacen-form/almacen-form.page.html`
- [X] T030 [P] [US4] Aplicar en Tipo y Usuario: `sigefor/src/app/features/tipos/pages/tipo-form/tipo-form.page.html` y `sigefor/src/app/features/usuarios/pages/usuario-form/usuario-form.page.html`
- [X] T031 [P] [US4] Aplicar en Traslado y Baja: `sigefor/src/app/features/traslados/pages/traslado-form/traslado-form.page.html` y `sigefor/src/app/features/bajas/pages/baja-form/baja-form.page.html`
- [X] T032 [US4] Migrar Incidencia a Reactive Forms con validators (código requerido+resuelto, tipo requerido, descripción requerida max 500) y `<app-field-errors>` en `sigefor/src/app/features/incidencias/pages/incidencia-form/incidencia-form.page.ts/.html`, conservando el flujo de resolución de código QR
- [X] T033 [US4] Limpiar duplicados `.form-actions`/`.state-message` de los SCSS por página (fornitura-form, fornitura-lote, elemento-form, almacen-form, tipo-form, usuario-form, traslado-form, baja-form, incidencia-form, bajas, incidencias, traslados, asignacion y demás que los redefinan)

**Checkpoint**: quickstart escenario 3 pasa; formularios alineados con obligatorios del backend (FR-009).

## Phase 7: User Story 5 — Pantalla de Asignación comprensible (P2)

**Goal**: asistente en tarjetas numeradas, searchbar fuera de lista, vigentes separada; conservar PR #5.

**Independent Test**: flujo QR→elemento→asignar sin solapes en 360–1280px; AUDITOR ve solo vigentes.
**Nota**: usar la skill **ui-ux-pro-max** para el trabajo visual de esta fase.

- [X] T034 [US5] Reestructurar `sigefor/src/app/features/asignacion/pages/asignacion/asignacion.page.html`: `ion-card` "1 · Identificar fornitura" (con `<app-qr-scan>`, spinner, `.scan-result-banner` y detalle/badge tal cual) + `ion-card` "2 · Seleccionar elemento" (searchbar fuera de `ion-list`, resultados, seleccionado, estado "sin resultados") + bloque de acciones; "Asignaciones vigentes" en su propia lista con encabezado
- [X] T035 [US5] Estilos de secciones en `sigefor/src/app/features/asignacion/pages/asignacion/asignacion.page.scss`: separación entre tarjetas, numeración de paso, estado deshabilitado/atenuado del Paso 2 sin fornitura disponible, sin `position/z-index` frágiles
- [X] T036 [US5] Ajustes de lógica mínimos en `asignacion.page.ts`: señal "sin resultados" para búsqueda vacía (edge case), reset del asistente tras asignar/limpiar ya existente se conserva; sin tocar `qr-scan` ni servicios

**Checkpoint**: quickstart escenario 5 pasa; tests existentes de qr-scan siguen verdes.

## Phase 8: User Story 6 — Login con identidad institucional (P3)

**Goal**: split-screen institucional mobile-first con escudo provisional; forgot/reset intactos.

**Independent Test**: quickstart escenario 4 (≥900px dos paneles; ~390px cabecera compacta; forgot/reset sin regresión).
**Nota**: usar la skill **ui-ux-pro-max** para el trabajo visual de esta fase.

- [X] T037 [P] [US6] Crear emblema provisional `sigefor/src/assets/img/escudo-sigefor.svg` (escudo estilizado dorado/blanco coherente con paleta gobmx, reemplazable por el oficial)
- [X] T038 [US6] Rediseñar `sigefor/src/app/features/auth/pages/login/login.page.html`: estructura split (`.login-split`) — panel institucional (escudo, "SIGEFOR", "Sistema Integral de Gestión de Fornituras", barra dorada) + panel formulario (campos/validación/toast actuales sin cambios de lógica en `login.page.ts`)
- [X] T039 [US6] Estilos del split en SCSS propio del login (`sigefor/src/app/features/auth/pages/login/login.page.scss` nuevo, manteniendo `auth-page.scss` compartido intacto): mobile-first (cabecera compacta guinda) + `@media (min-width: 900px)` dos paneles 50/50; fuente Patria y variables `--gobmx-*`
- [X] T040 [US6] Verificar sin regresión `sigefor/src/app/features/auth/pages/forgot-password/` y `reset-password/` (siguen usando `auth-page.scss`; smoke visual + tests existentes)

**Checkpoint**: quickstart escenario 4 pasa.

## Phase 9: Polish & Cross-Cutting

- [X] T041 [P] Suites completas: `dotnet test fornituras-api-dotnet` y `npm test -- --watch=false --browsers=ChromeHeadless` en `sigefor/` — todo verde (SC-007)
- [X] T042 Validación end-to-end con `quickstart.md` (escenarios 1–6) levantando API + frontend; capturar evidencia de: menú ADMIN con 13 módulos, rol visible, matriz por rol, errores de formulario, login responsive, asignación sin solapes
- [X] T043 Commits en español (presente imperativo) por fase lógica en la rama `021-rbac-ui-validacion-rediseno` y push de la rama (sin fusionar a dev)

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2)** → historias.
- **US1** depende de Foundational (role-labels para T009/T010; el seeder T007/T008 solo de Setup).
- **US2** depende de Foundational; independiente de US1 (puede probarse con usuarios creados manualmente).
- **US3** independiente (solo Setup).
- **US4** depende de Foundational solo por convención de estilos (T026); el componente (T024/T025) es independiente.
- **US5** conviene tras US2 (usa el flag `WRITE_OPERATIONS` reactivo de T015), pero es implementable en paralelo si T015 se hace primero.
- **US6** independiente (solo toca auth).
- **Polish** al final.

Orden de entrega incremental: US1 (MVP) → US2 → US3 → US4 → US5 → US6 → Polish.

## Parallel Examples

- Foundational: T002 ∥ T003 ∥ T006 (specs/archivos distintos), luego T004→T005.
- US1: T007 (backend) ∥ T009 (frontend).
- US2: T013–T019 en paralelo (una página por tarea) tras T012.
- US4: T027–T031 en paralelo tras T025/T026.
- US3: T022 ∥ T023 tras T021. US6: T037 ∥ (T038→T039).

## Implementation Strategy

**MVP = Phase 1+2+3 (US1)**: con solo el seeder *ensure* + rol visible, el administrador
recupera el sistema (el reporte original queda resuelto para su cuenta). Cada fase posterior
es un incremento independiente verificable con su escenario del quickstart. TDD en todas las
piezas con lógica (matriz, hasAnyRole, seeder, field-errors, filtros de menú); las fases
visuales (US5/US6) se validan con los escenarios manuales del quickstart + tests de humo
existentes.
