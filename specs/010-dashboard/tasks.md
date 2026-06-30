---
description: "Task list — Tablero de control / Dashboard (010)"
---

# Tasks: Tablero de control (Dashboard)

**Input**: Design documents from `specs/010-dashboard/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de que los contadores **coinciden** con los listados filtrados
equivalentes, de autorización (sin PII) y de criterio de vigencia compartido con 001/008.

**Organization**: una user story (P1); feature de agregación sin entidades nuevas.

## Path Conventions

- **Backend**: `<be>/dashboard/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/dashboard/`;
  pruebas en `<bet>/dashboard/`.
- **Frontend**: `<fe>/inicio/` = `sigefor/src/app/features/inicio/` (ya andamiada).

---

## Phase 1: Setup

- [ ] T001 Crear la estructura de paquetes del módulo `dashboard` (`controller/`, `service/`, `dto/`) en `<be>/dashboard/`
- [ ] T002 [P] Preparar la página frontend `<fe>/inicio/` (ya existe `pages/inicio/`; crear `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: completar antes de la user story.

- [ ] T003 [P] Definir el DTO `DashboardSummary` (total, disponibles, asignadas, proximasAVencer, caducadas, enMantenimiento) en `<be>/dashboard/dto/`
- [ ] T004 [P] Reutilizar el criterio `VigenciaCriteria` (≤ 90 días / vencida) de **001**/**008** para que el tablero coincida con ellos en `<be>/dashboard/service/`
- [ ] T005 Configurar **autorización** del endpoint (`/dashboard/**` autenticado; filtrar indicadores por rol; sin PII)

**Checkpoint**: contrato y criterio listos.

---

## Phase 3: User Story 1 - Ver indicadores clave al entrar (Priority: P1) 🎯 MVP

**Goal**: al abrir `/inicio`, mostrar los contadores con color semántico, calculados con consultas
agregadas server-side, respetando rol y sin PII.

**Independent Test**: con datos cargados, los contadores coinciden con el inventario real; cada indicador
usa el color de `docs/05-ui-ux.md`; inventario vacío → ceros sin error.

### Tests for User Story 1

- [ ] T006 [P] [US1] Test de integración: cada contador de `GET /dashboard/summary` **coincide** con el `COUNT` del listado filtrado equivalente de 001/008 (SC-002) en `<bet>/dashboard/DashboardSummaryIntegrationTest.java`
- [ ] T007 [P] [US1] Test: inventario vacío → todos los contadores en cero sin error (Edge Case) en `<bet>/dashboard/DashboardEmptyTest.java`
- [ ] T008 [P] [US1] Test de autorización: requiere autenticación; respuesta sin PII; filtra indicadores por rol en `<bet>/dashboard/DashboardAuthTest.java`

### Implementation for User Story 1

- [ ] T009 [US1] Implementar `DashboardService` con **consultas agregadas** (`COUNT`/`GROUP BY` sobre `equipment` por estado y `fecha_vencimiento`; asignadas vía 004 si aplica) sin traer registros al cliente en `<be>/dashboard/service/`
- [ ] T010 [US1] Implementar `GET /dashboard/summary` en `DashboardController` (una sola respuesta) en `<be>/dashboard/controller/`
- [ ] T011 [P] [US1] Frontend: `dashboard.service.ts` (`getSummary`) en `<fe>/inicio/data/`
- [ ] T012 [US1] Frontend: página `inicio` — tarjetas de indicadores con **color semántico** institucional (guinda/verde/azul/naranja/rojo/amarillo, `docs/05-ui-ux.md`) en `<fe>/inicio/pages/inicio/`

**Checkpoint**: tablero operativo y consistente con los listados.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T013 [P] Verificar rendimiento (< 2 s con decenas de miles de fornituras, SC-001); apoyarse en índices de 001; evaluar caché breve solo si hace falta
- [ ] T014 Validar el quickstart (coincidencia con listados, vacío sin error, recarga refleja cambios) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (P1, MVP) → Polish.**
- Depende de **001** (estados + `fecha_vencimiento` + criterio de vigencia) y, para "asignadas", de
  **004**/**008**. Mientras 004/008 no existan, esos contadores se calculan desde `equipment.status` y se
  completan al integrarlos.

### Parallel Opportunities

- Foundational: T003, T004 en paralelo.
- US1: tests T006–T008 en paralelo; T011 (servicio frontend) con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **Agregados server-side**: nunca traer el inventario al cliente; cero PII en el tablero.
- Criterio de vigencia **compartido** con 001/008 (única fuente de verdad) para que los números coincidan.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
