---
description: "Task list — Asignación de fornituras y resguardos (004)"
---

# Tasks: Asignación de fornituras y resguardos

**Input**: Design documents from `specs/004-asignacion-resguardos/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/assignments-api.md](./contracts/assignments-api.md)

**Tests**: incluidos. Por ser el **núcleo** del sistema y tocar PII (vía 003), las pruebas de
**concurrencia** (una sola asignación vigente por fornitura), enmascaramiento y auditoría son parte del
entregable, no opcionales.

> **Dependencia dura:** consume `equipment` (**001**, resolución `codigo → fornitura` + disponibilidad)
> y `officers` (**003**, búsqueda de elemento enmascarada). Hasta tener su mínimo viable, las tareas de
> integración de esas lecturas se prueban contra el puerto correspondiente (ver Foundational).

**Organization**: por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/assignments/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/assignments/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/assignments/`.
- **Frontend**: `<fe>/asignacion/` = `sigefor/src/app/features/asignacion/` (ya andamiada).

---

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Crear la estructura de paquetes del módulo `assignments` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/assignments/`
- [ ] T002 [P] Preparar la feature frontend `<fe>/asignacion/` (ya existe `pages/asignacion/`; crear `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [ ] T003 [P] Crear la entidad `Assignment` (equipment_id, officer_id, fecha_asignacion, fecha_devolucion NULL=vigente, asignado_por, recibido_por, firma) en `<be>/assignments/entity/Assignment.java`
- [ ] T004 Crear la migración Flyway `V{n}__create_assignment.sql` con **índice único filtrado** `(equipment_id) WHERE fecha_devolucion IS NULL` (garantiza una sola asignación vigente por fornitura) — usar el siguiente número Flyway libre
- [ ] T005 [P] Definir DTOs `AssignRequest`, `AssignmentSummary`, `ResguardoMeta` en `<be>/assignments/dto/`
- [ ] T006 [P] Definir el puerto `EquipmentLookup` (resolver `codigo → fornitura` + estado disponible) que implementa **001** (`GET /equipment/by-codigo`) en `<be>/assignments/service/`
- [ ] T007 [P] Definir el puerto `OfficerLookup` (buscar elemento por nombre/placa/CURP/RFC, enmascarado) que implementa **003** en `<be>/assignments/service/`
- [ ] T008 Configurar **autorización por rol** para `/assignments/**` (asignar/devolver/reasignar restringido; consulta de vigentes a roles operativos; rechazo por defecto)
- [ ] T009 [P] Reusar el escritor de **auditoría** (012) para `ASSIGN/RETURN/REASSIGN` (actor, fornitura, elemento por id, cuándo; sin PII); si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo — las user stories pueden empezar.

---

## Phase 3: User Story 1 - Asignar una fornitura a un elemento (Priority: P1) 🎯 MVP

**Goal**: flujo de 2 pasos (resolver código → fornitura disponible; buscar elemento; asignar) con
garantía de una sola asignación vigente por fornitura y auditoría.

**Independent Test**: con fornitura disponible y elemento existentes, asignar en < 1 min; dos
asignaciones simultáneas de la misma fornitura → solo una gana (la otra recibe conflicto).

### Tests for User Story 1

- [ ] T010 [P] [US1] Test de contrato `POST /assignments` (valida disponibilidad; 409 si ya está asignada) en `<bet>/assignments/AssignContractTest.java`
- [ ] T011 [P] [US1] Test de **concurrencia** (Testcontainers MSSQL): dos `POST /assignments` simultáneos de la misma fornitura → exactamente una asignación vigente en `<bet>/assignments/AssignConcurrencyTest.java`
- [ ] T012 [P] [US1] Test de autorización + auditoría: rol sin permiso → denegado; asignación exitosa → evento `ASSIGN` sin PII en `<bet>/assignments/AssignAuthAuditTest.java`

### Implementation for User Story 1

- [ ] T013 [US1] Implementar `AssignmentRepository` (asignación vigente por fornitura, historial, paginación) en `<be>/assignments/repository/`
- [ ] T014 [US1] Implementar `AssignmentService.assign()` (resolver código vía `EquipmentLookup`, verificar disponibilidad, transacción + manejo del índice único filtrado → 409 en conflicto, marcar fornitura "asignada", auditar) en `<be>/assignments/service/`
- [ ] T015 [US1] Implementar `POST /assignments` y `GET /assignments` (vigentes paginadas) en `<be>/assignments/controller/`
- [ ] T016 [P] [US1] Frontend: `assignments.service.ts` (resolver código, buscar elemento, asignar, listar vigentes) en `<fe>/asignacion/data/`
- [ ] T017 [US1] Frontend: wizard de 2 pasos en `pages/asignacion/` — paso 1 captura QR (componente **014**) y muestra la fornitura; paso 2 busca elemento (PII enmascarada) y confirma; al cargar, lista vigentes paginadas en `<fe>/asignacion/pages/asignacion/`

**Checkpoint**: se puede asignar de forma segura (MVP del núcleo).

---

## Phase 4: User Story 2 - Devolver / reasignar una fornitura (Priority: P1)

**Goal**: cerrar una asignación vigente (devolución) y reasignar a otro elemento, conservando historial.

**Independent Test**: devolver una fornitura asignada → vuelve a "disponible" y queda en historial;
reasignar → cierra la vigente y abre una nueva atómicamente.

### Tests for User Story 2

- [ ] T018 [P] [US2] Test de contrato `POST /assignments/{id}/return` y `POST /assignments/reassign` (cierra vigente, abre nueva; historial preservado) en `<bet>/assignments/ReturnReassignContractTest.java`
- [ ] T019 [P] [US2] Test de integración: devolución libera la fornitura ("disponible"); reasignación es atómica y auditada (`RETURN`/`REASSIGN`) en `<bet>/assignments/ReturnReassignIntegrationTest.java`

### Implementation for User Story 2

- [ ] T020 [US2] Implementar `return()` y `reassign()` en `AssignmentService` (setear `fecha_devolucion`, liberar fornitura, abrir nueva en transacción, auditar) en `<be>/assignments/service/`
- [ ] T021 [US2] Implementar `POST /assignments/{id}/return` y `POST /assignments/reassign` en `<be>/assignments/controller/`
- [ ] T022 [US2] Frontend: acciones de devolver/reasignar en la lista de vigentes en `<fe>/asignacion/pages/asignacion/`

**Checkpoint**: ciclo completo asignar→devolver→reasignar con historial.

---

## Phase 5: User Story 3 - Generar resguardo (Priority: P2)

**Goal**: emitir el resguardo (PDF) de una asignación, con firma electrónica opcional; persistir solo
metadatos.

**Independent Test**: generar el resguardo de una asignación → PDF con los datos del resguardo; si el
dispositivo no soporta firma, se emite sin firma y queda auditado.

### Tests for User Story 3

- [ ] T023 [P] [US3] Test de `GET /assignments/{id}/resguardo` (genera PDF; metadatos persistidos; sin exponer PII de más) en `<bet>/assignments/ResguardoTest.java`

### Implementation for User Story 3

- [ ] T024 [US3] Implementar `ResguardoPdfService` reutilizando la librería de PDF de `qrcodes` (no añadir dependencia nueva) en `<be>/assignments/service/`
- [ ] T025 [US3] Implementar `GET /assignments/{id}/resguardo` (PDF al vuelo; persistir `ResguardoMeta`; firma electrónica opcional; auditar emisión) en `<be>/assignments/controller/`
- [ ] T026 [US3] Frontend: botón "Generar resguardo" (descarga PDF; captura de firma si está disponible) en `<fe>/asignacion/pages/asignacion/`

**Checkpoint**: las tres historias funcionan; el núcleo del sistema está operativo.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T027 [P] Endurecimiento: sin PII en logs/URLs (omitir/hashear términos de búsqueda de elemento); errores que no filtran detalles en `<be>/assignments/`
- [ ] T028 [P] Decidir el alcance de **firma electrónica** (obligatoria/opcional, formato) → ADR si aplica; documentar el fallback sin firma
- [ ] T029 Validar el quickstart (asignar, concurrencia, devolver/reasignar, resguardo, auditoría) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (P1, MVP) → US2 (P1) → US3 (P2) → Polish.**
- **Bloqueo externo:** US1 necesita el mínimo de **001** (`GET /equipment/by-codigo` + estado) y de
  **003** (búsqueda de elemento). Los puertos `EquipmentLookup`/`OfficerLookup` permiten desarrollar y
  testear con dobles; la integración real cierra al existir 001/003.
- El componente **014** habilita la captura de QR del paso 1.

### Parallel Opportunities

- Foundational: T003, T005, T006, T007, T009 en paralelo; T004 tras T003.
- US1: tests T010–T012 en paralelo; T016 (servicio frontend) en paralelo con backend.
- US2/US3: tests de cada historia en paralelo; frontend en paralelo con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **Integridad/concurrencia primero**: el índice único filtrado es la garantía dura de "una vigente por fornitura"; se prueba explícitamente.
- PII del elemento solo vía 003 (enmascarada); auditoría sin PII (referencia por id).
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
