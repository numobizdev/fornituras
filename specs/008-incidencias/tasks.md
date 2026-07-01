---
description: "Task list — Incidencias y mantenimiento (008)"
---

# Tasks: Incidencias y mantenimiento

**Input**: Design documents from `specs/008-incidencias/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de cambio de estado de la fornitura al reportar/resolver, derivación de
alertas por umbral de fecha (mismo criterio que 001/010), autorización y auditoría.

**Organization**: por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/incidents/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/incidents/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/incidents/`.
- **Frontend**: `<fe>/incidencias/` = `sigefor/src/app/features/incidencias/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear la estructura de paquetes del módulo `incidents` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/incidents/`
- [ ] T002 [P] Preparar la feature frontend `<fe>/incidencias/` (`pages/incidencias/`, `pages/incidencia-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T003 [P] Crear la entidad `Incident` (equipment_id, tipo, descripción, estado, fecha_reporte, fecha_resolucion, reportado_por, actualizado_por) en `<be>/incidents/entity/Incident.java`
- [X] T004 Crear la migración Flyway `V{n}__create_incident.sql` (FK a `equipment`; índices por estado/equipment_id) — usar el siguiente número Flyway libre
- [X] T005 [P] Definir DTOs `IncidentCreateRequest`, `IncidentSummary`, `AlertItem` en `<be>/incidents/dto/`
- [X] T006 [P] Definir el puerto `EquipmentStateChanger` (poner "en mantenimiento"/"extraviada"/"disponible") implementado por **001**, y `VigenciaCriteria` (umbral ≤ 90 días reutilizado de 001) en `<be>/incidents/service/`
- [X] T007 Configurar **autorización por rol** para `/incidents/**` y `/alerts/**` (reportar/actualizar restringido; consulta a roles operativos; rechazo por defecto)
- [X] T008 [P] Reusar el escritor de **auditoría** (012) para `REPORT/UPDATE_INCIDENT`; si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 2 - Reportar una incidencia (Priority: P1) 🎯 MVP

**Goal**: reportar un problema sobre una fornitura; la incidencia queda "abierta" y, si aplica, la
fornitura pasa a "en mantenimiento"/"extraviada"; todo auditado.

**Independent Test**: reportar daño sobre fornitura disponible → incidencia "abierta" + fornitura "en
mantenimiento".

### Tests for User Story 2

- [X] T009 [P] [US2] Test de contrato `POST /incidents` (validación tipo/descripción; estado inicial "abierta") en `<bet>/incidents/IncidentCreateContractTest.java`
- [X] T010 [P] [US2] Test de integración: reporte de retiro → fornitura "en mantenimiento"/"extraviada"; auditado en `<bet>/incidents/IncidentCreateIntegrationTest.java`

### Implementation for User Story 2

- [X] T011 [US2] Implementar `IncidentRepository` (persistencia, por estado/equipment) en `<be>/incidents/repository/`
- [X] T012 [US2] Implementar `IncidentService.report()` (crear "abierta", cambiar estado de fornitura vía `EquipmentStateChanger` si aplica, auditar) en `<be>/incidents/service/`
- [X] T013 [US2] Implementar `POST /incidents` con **Bean Validation** del request en `<be>/incidents/controller/` y `<be>/incidents/dto/`
- [ ] T014 [P] [US2] Frontend: `incidents.service.ts` (`report`) en `<fe>/incidencias/data/`
- [ ] T015 [US2] Frontend: página `incidencia-form` (fornitura, tipo, descripción) en `<fe>/incidencias/pages/incidencia-form/`

**Checkpoint**: se pueden reportar incidencias (MVP).

---

## Phase 4: User Story 1 - Consultar incidencias (Priority: P1)

**Goal**: header con filtro por estado + tabla paginada (fornitura, problema, fecha, estado, "Actualizar").

**Independent Test**: con incidencias existentes, la tabla las muestra paginadas; el filtro por estado acota.

### Tests for User Story 1

- [X] T016 [P] [US1] Test de contrato `GET /incidents` (paginación + filtro estado) en `<bet>/incidents/IncidentListContractTest.java`

### Implementation for User Story 1

- [X] T017 [US1] Implementar `GET /incidents` (paginado + filtro estado) en `<be>/incidents/controller/`
- [ ] T018 [P] [US1] Frontend: `incidents.service.ts` (`list`) en `<fe>/incidencias/data/`
- [ ] T019 [US1] Frontend: página de listado (filtro de estado + tabla paginada con color semántico + acción "Actualizar") en `<fe>/incidencias/pages/incidencias/`

**Checkpoint**: seguimiento visible.

---

## Phase 5: User Story 3 - Actualizar/cerrar una incidencia (Priority: P2)

**Goal**: actualizar estado (en proceso → resuelta/cerrada); al resolver, la fornitura puede volver a
"disponible"; auditado.

**Independent Test**: actualizar "abierta"→"resuelta" → registrado y fornitura "disponible" si aplica.

### Tests for User Story 3

- [X] T020 [P] [US3] Test de integración `PATCH /incidents/{id}` (transición de estado + retorno de fornitura a "disponible" + auditoría) en `<bet>/incidents/IncidentUpdateTest.java`

### Implementation for User Story 3

- [X] T021 [US3] Implementar `IncidentService.update()` (transición de estado, liberar fornitura si procede, auditar) en `<be>/incidents/service/`
- [X] T022 [US3] Implementar `PATCH /incidents/{id}` en `<be>/incidents/controller/`
- [ ] T023 [US3] Frontend: acción "Actualizar" (modal de estado) en `<fe>/incidencias/pages/incidencias/`

**Checkpoint**: ciclo de incidencia completo.

---

## Phase 6: User Story 4 - Alertas inteligentes de vigencia y mantenimiento (Priority: P2)

**Goal**: derivar alertas preventivas (≤ 90 días, naranja) y críticas (caducada, rojo), más
mantenimiento, con el **mismo criterio** que 001/010.

**Independent Test**: fornitura con vencimiento ≤ 30 días → alerta preventiva; vencida → alerta crítica.

### Tests for User Story 4

- [X] T024 [P] [US4] Test: `GET /alerts/vigencia` clasifica próximas (≤ 90 días) y caducadas con el umbral compartido (SC-001/SC-002) en `<bet>/incidents/AlertVigenciaTest.java`

### Implementation for User Story 4

- [X] T025 [US4] Implementar `AlertService` (derivación por `fecha_vencimiento` usando `VigenciaCriteria`; consulta agregada eficiente) en `<be>/incidents/service/`
- [X] T026 [US4] Implementar `GET /alerts/vigencia` (próximas/caducadas) y alertas de mantenimiento en `<be>/incidents/controller/`
- [ ] T027 [US4] Frontend: vista/sección de alertas con color semántico (`docs/05-ui-ux.md`) en `<fe>/incidencias/pages/incidencias/`

**Checkpoint**: las cuatro historias funcionan; alertas derivadas operativas.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [~] T028 [P] *(diferido)* Evaluar **materializar alertas** (tabla + job) si el volumen lo exige → ADR
- [X] T029 [P] Tests unitarios del umbral de vigencia y de transición de estados en `<bet>/incidents/`
- [X] T030 Validar el quickstart (reporte→mantenimiento, resolución→disponible, alertas por fecha) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US2 (P1, MVP) → US1 (P1) → US3 (P2) → US4 (P2) → Polish.**
- Depende de **001** (estado de fornitura + `fecha_vencimiento` + criterio de vigencia compartido).
- El criterio de vigencia (T006 `VigenciaCriteria`) DEBE ser el mismo de 001/010 para no divergir.

### Parallel Opportunities

- Foundational: T003, T005, T006, T008 en paralelo; T004 tras T003.
- US2: T009/T010 en paralelo; T014 con backend. Demás historias: tests en paralelo, frontend con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Las alertas de vigencia son **derivadas** y comparten umbral con 001/010 (única fuente de verdad).
- Color semántico de estados según `docs/05-ui-ux.md`.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).

### Estado de implementación (2026-07-01)

- **Backend completo y probado.** Módulo `modules/incidents` (entidad `Incident` + enums
  `IncidentType`/`IncidentStatus`, migración `V19__create_incident.sql`, DTOs, `IncidentRepository`,
  `IncidentService` con reporte/actualización, `AlertService` de vigencia derivada, `IncidentController`
  y `AlertController` con `@PreAuthorize`). Alertas derivadas reutilizando `ExpiryCalculator`
  (umbral ≤ 90 días compartido con 001/010) vía nuevo método en `EquipmentRepository`. **17 pruebas
  nuevas** (contrato, integración, alertas y autorización) verdes; suite backend total: **156 tests,
  0 fallos, 0 errores**. Quickstart (T030) validado por esas pruebas.
- **Desviación de diseño (documentada):** T006 no introduce el puerto `EquipmentStateChanger`; siguiendo
  el precedente de 004/007, `IncidentService` delega el cambio de estado en `EquipmentService.changeStatus`
  (única fuente de las transiciones). Guarda de seguridad añadida: al reportar solo se retira la fornitura
  si está DISPONIBLE/ASIGNADA (no se interfiere con una custodia EN_TRASLADO); al resolver solo vuelve a
  DISPONIBLE si seguía retirada (EN_MANTENIMIENTO/EXTRAVIADA). Sin `mapper/` (mapeo inline en el servicio).
- **Pendiente — frontend (`sigefor/`):** T002, T014, T015, T018, T019, T023, T027 (feature
  `features/incidencias`: servicio HTTP, formulario de reporte, listado con color semántico y modal de
  actualización, vista de alertas). Se abordará con la skill de UI/UX del proyecto.
- **Diferido por diseño (`[~]`):** T028 (materializar alertas con tabla + job → ADR si el volumen lo exige;
  hoy las alertas son derivadas).
