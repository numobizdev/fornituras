---
description: "Task list — Bajas definitivas de fornituras (009)"
---

# Tasks: Bajas definitivas de fornituras

**Input**: Design documents from `specs/009-bajas/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de bloqueo (asignación vigente/en traslado), rechazo de operaciones sobre
fornitura dada de baja, preservación de historial, autorización y auditoría.

**Organization**: por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/decommissions/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/decommissions/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/decommissions/`.
- **Frontend**: `<fe>/bajas/` = `sigefor/src/app/features/bajas/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear la estructura de paquetes del módulo `decommissions` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/decommissions/`
- [ ] T002 [P] Preparar la feature frontend `<fe>/bajas/` (`pages/bajas/`, `pages/baja-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T003 [P] Crear entidades `Decommission` (equipment_id, motivo_id, fecha, responsable, observaciones) y `DecommissionReason` (nombre, active) en `<be>/decommissions/entity/`
- [X] T004 Crear la migración Flyway `V{n}__create_decommission.sql` (`decommission` + `decommission_reason`; FK a `equipment`; índices por fecha/motivo) y **sembrar** motivos base (caducidad/daño/extravío/obsolescencia) — usar el siguiente número Flyway libre
- [X] T005 [P] Definir DTOs `DecommissionRequest`, `DecommissionSummary` en `<be>/decommissions/dto/`
- [X] T006 [P] Reusar el puerto `EquipmentLifecycleQuery` (¿asignación vigente? ¿en traslado?) y `EquipmentStateChanger` (poner "baja definitiva") de **001** en `<be>/decommissions/service/`
- [X] T007 Configurar **autorización por rol** para `/decommissions/**` (dar de baja restringido a rol elevado; consulta a roles operativos; rechazo por defecto)
- [X] T008 [P] Reusar el escritor de **auditoría** (012) para `DECOMMISSION_EQUIPMENT`; si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 1 - Dar de baja una fornitura (Priority: P1) 🎯 MVP

**Goal**: buscar por QR, registrar motivo y dar de baja; bloquear si tiene asignación vigente/en traslado;
conservar historial; auditar.

**Independent Test**: dar de baja una fornitura disponible por QR con motivo → "baja definitiva", deja de
ser asignable, queda en la lista de bajas; con asignación vigente → bloqueado.

### Tests for User Story 1

- [X] T009 [P] [US1] Test de contrato `POST /decommissions` (valida motivo; 409/bloqueo si hay asignación vigente/en traslado) en `<bet>/decommissions/DecommissionContractTest.java`
- [X] T010 [P] [US1] Test de integración: baja exitosa → estado "baja definitiva" + historial preservado + auditado; **bloqueo** con asignación vigente y con "en traslado" en `<bet>/decommissions/DecommissionIntegrationTest.java`
- [X] T011 [P] [US1] Test: operar (asignar/trasladar) una fornitura dada de baja → rechazado (SC-002) en `<bet>/decommissions/DecommissionGuardTest.java`

### Implementation for User Story 1

- [X] T012 [US1] Implementar `DecommissionRepository`/`DecommissionReasonRepository` en `<be>/decommissions/repository/`
- [X] T013 [US1] Implementar `DecommissionService.decommission()` (resolver fornitura por código, validar bloqueo vía `EquipmentLifecycleQuery`, registrar motivo/fecha/responsable, cambiar estado vía `EquipmentStateChanger`, auditar) en `<be>/decommissions/service/`
- [X] T014 [US1] Implementar `POST /decommissions` con **Bean Validation** del request en `<be>/decommissions/controller/` y `<be>/decommissions/dto/`
- [ ] T015 [P] [US1] Frontend: `decommissions.service.ts` (`decommission`, catálogo de motivos) en `<fe>/bajas/data/`
- [ ] T016 [US1] Frontend: página `baja-form` (búsqueda por QR con componente **014**, selección de motivo, confirmación) en `<fe>/bajas/pages/baja-form/`

**Checkpoint**: se pueden dar de baja fornituras de forma segura (MVP).

---

## Phase 4: User Story 2 - Consultar fornituras dadas de baja (Priority: P2)

**Goal**: tabla paginada de bajas con QR, descripción, tipo, motivo, fecha, responsable y filtros.

**Independent Test**: con bajas existentes, la lista las muestra paginadas con motivo y fecha; filtrar por
fecha/tipo acota.

### Tests for User Story 2

- [X] T017 [P] [US2] Test de contrato `GET /decommissions` (paginación + filtros fecha/tipo/motivo) en `<bet>/decommissions/DecommissionListContractTest.java`

### Implementation for User Story 2

- [X] T018 [US2] Implementar `GET /decommissions` (paginado + filtros) en `<be>/decommissions/controller/`
- [ ] T019 [P] [US2] Frontend: `decommissions.service.ts` (`list`) en `<fe>/bajas/data/`
- [ ] T020 [US2] Frontend: página de listado (tabla paginada + filtros) en `<fe>/bajas/pages/bajas/`

**Checkpoint**: ambas historias funcionan.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T021 [P] Documentar la **política de no-reversión** (corrección por ajuste auditado) y, si se formaliza un flujo de ajuste, abrir ADR
- [ ] T022 [P] Tests unitarios de la regla de bloqueo y del cambio de estado en `<bet>/decommissions/`
- [ ] T023 Validar el quickstart (baja por QR, bloqueo con asignación, rechazo de operaciones sobre baja) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (P1, MVP) → US2 (P2) → Polish.**
- Depende de **001** (estado/resolución por código + `EquipmentLifecycleQuery`), **004**/**007** (para
  que el bloqueo "asignación vigente/en traslado" sea real) y **014** (captura). El puerto permite
  desarrollar y testear antes de que 004/007 existan; la integración real cierra al integrarlos.

### Parallel Opportunities

- Foundational: T003, T005, T006, T008 en paralelo; T004 tras T003.
- US1: tests T009–T011 en paralelo; T015 con backend. US2: T017 con T019/T020.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Una baja **no se revierte**; los errores se corrigen con ajuste auditado.
- Sin PII; resolución de código server-side; auditoría por id.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
