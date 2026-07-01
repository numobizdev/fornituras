---
description: "Task list — Traslados entre almacenes (007)"
---

# Tasks: Traslados entre almacenes

**Input**: Design documents from `specs/007-traslados/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de transición de estados, validación de origen/disponibilidad, bloqueo de
asignación/baja de fornituras "en traslado", cancelación, autorización y auditoría.

**Organization**: por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/transfers/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/transfers/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/transfers/`.
- **Frontend**: `<fe>/traslados/` = `sigefor/src/app/features/traslados/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear la estructura de paquetes del módulo `transfers` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`) en `<be>/transfers/` — sin `mapper/` (mapeo inline en el servicio, igual que 004)
- [X] T002 [P] Preparar la feature frontend `<fe>/traslados/` (`pages/traslados/`, `pages/traslado-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T003 [P] Crear entidades `Transfer` (origen, destino, estado, fechas, creado_por, recibido_por) y `TransferItem` (transfer ↔ equipment) en `<be>/transfers/entity/` (+ enum `TransferStatus`)
- [X] T004 Crear la migración Flyway `V16__create_transfer.sql` (`transfer` + `transfer_item`; FKs a `warehouse`/`equipment`/`users`; `CHECK` estado y origen≠destino; índices por estado/origen/destino)
- [X] T005 [P] Definir DTOs `TransferCreateRequest`, `TransferSummary`, `TransferDetail` (+ `TransferItemDetail`) en `<be>/transfers/dto/`
- [~] T006 [P] Definir el puerto `EquipmentStateChanger` — **desviación:** siguiendo el precedente de 004 (`AssignmentService`), `TransferService` muta el estado/almacén de la fornitura vía `EquipmentRepository` directamente, sin puerto extra. La dirección inversa (¿en traslado?) sí se expone por puerto: `TransferLifecycleQuery` (@Primary) implementa `EquipmentLifecycleQuery.hasOngoingTransfer`
- [X] T007 Configurar **autorización por rol** para `/transfers/**` (`@PreAuthorize`: crear/recibir/cancelar solo ADMIN/CAPTURISTA; consulta a cualquier autenticado)
- [X] T008 [P] Reusar el escritor de **auditoría** (`AuditWriter`) para `CREATE/RECEIVE/CANCEL_TRANSFER`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 2 - Registrar un nuevo traslado (Priority: P1) 🎯 MVP

**Goal**: crear traslado origen→destino agregando fornituras disponibles del origen por QR; al confirmar,
quedan "en traslado" y el traslado "enviado".

**Independent Test**: crear traslado con 2 fornituras disponibles del origen → "en traslado" + "enviado"
con fecha de envío; agregar una no disponible/asignada o de otro almacén → bloqueada con motivo.

### Tests for User Story 2

- [~] T009 [P] [US2] Test de contrato `POST /transfers` — **diferido** (sin infra Testcontainers/MockMvc, como en 001/003/004). La lógica (validación origen/disponibilidad/ubicación, estados resultantes) está cubierta a nivel unitario en `TransferServiceTest`
- [~] T010 [P] [US2] Test de integración de creación — **diferido** (misma razón); cubierto por `TransferServiceTest.create_*`
- [X] T011 [P] [US2] Test: una fornitura "en traslado" **no** puede asignarse/darse de baja — cubierto por `TransferLifecycleQueryTest` (puerto) + guard añadido en `EquipmentService.changeStatus` (baja bloqueada si `hasOngoingTransfer`); asignación ya exige `DISPONIBLE`

### Implementation for User Story 2

- [X] T012 [US2] Implementar `TransferRepository` (specs de filtro/paginación + `existsOngoingByEquipmentId`) y `TransferItemRepository` en `<be>/transfers/repository/`
- [X] T013 [US2] Implementar `TransferService.create()` (validar disponibilidad + `warehouse_id==origen`, pasar fornituras a "en traslado", estado "enviado", transacción, auditar) en `<be>/transfers/service/`
- [X] T014 [US2] Implementar `POST /transfers` en `TransferController` en `<be>/transfers/controller/`
- [X] T015 [P] [US2] Frontend: `transfers.service.ts` (crear, recibir, cancelar, listar) en `<fe>/traslados/data/`
- [X] T016 [US2] Frontend: página `traslado-form` (origen/destino + captura por QR con componente **014**, resolución server-side por código, tabla previa + confirmar) en `<fe>/traslados/pages/traslado-form/` — **cierra la integración T014 de la spec 014**

**Checkpoint**: se pueden crear traslados de forma segura (MVP).

---

## Phase 4: User Story 3 - Recibir un traslado (Priority: P2)

**Goal**: confirmar recepción en destino; las fornituras quedan "disponible" bajo el almacén destino con
fecha de recepción.

**Independent Test**: recibir un traslado "enviado" → "recibido", fornituras "disponibles" en destino,
fecha registrada.

### Tests for User Story 3

- [~] T017 [P] [US3] Test de contrato/integración `POST /transfers/{id}/receive` — **diferido** (sin infra Testcontainers/MockMvc); la lógica está cubierta por `TransferServiceTest.receive_*`

### Implementation for User Story 3

- [X] T018 [US3] Implementar `TransferService.receive()` (transacción: estado, liberar fornituras a "disponible" en destino, actualizar `warehouse_id`, auditar) en `<be>/transfers/service/`
- [X] T019 [US3] Implementar `POST /transfers/{id}/receive` en `<be>/transfers/controller/`
- [X] T020 [US3] Frontend: acción "Recibir" en la lista de traslados en `<fe>/traslados/pages/traslados/`

**Checkpoint**: ciclo enviar→recibir cerrado.

---

## Phase 5: User Story 1 - Consultar traslados (Priority: P2)

**Goal**: listado paginado con filtros (origen, destino, estado) y botón "Nuevo traslado".

**Independent Test**: con traslados existentes, la lista los muestra paginados; filtrar por estado/
almacén acota correctamente.

### Tests for User Story 1

- [~] T021 [P] [US1] Test de contrato `GET /transfers` — **diferido** (sin infra Testcontainers/MockMvc); el filtrado se implementa con `JpaSpecificationExecutor`

### Implementation for User Story 1

- [X] T022 [US1] Implementar `GET /transfers` (paginado + filtros origen/destino/estado) y `GET /transfers/{id}` en `<be>/transfers/controller/`
- [X] T023 [US1] Frontend: página de listado (origen→destino, estado con color, fechas, acciones) + filtros por estado/origen en `<fe>/traslados/pages/traslados/`

**Checkpoint**: visibilidad completa de los movimientos.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T024 Implementar `POST /transfers/{id}/cancel` (revertir fornituras a "disponible" en origen, estado "cancelado", auditar) en `<be>/transfers/controller/`
- [~] T025 [P] *(diferido por diseño)* Evaluar **recepción parcial** → ADR si se adopta; hoy la recepción es total. Sin cambios
- [X] T026 [P] Tests unitarios de transición de estados y validación de origen en `<bet>/transfers/` (`TransferServiceTest` 7 + `TransferLifecycleQueryTest` 3)
- [~] T027 Validar el quickstart (crear, bloquear asignación de "en traslado", recibir, cancelar) — **pendiente:** requiere SQL Server levantado

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US2 (P1, MVP) → US3 (P2) → US1 (P2) → Polish.**
- US2 antes que US3 (no hay qué recibir sin crear); US1 (consulta) puede ir en paralelo a US3.
- Depende de **001** (estado/ubicación de fornitura, vía `EquipmentStateChanger`/`EquipmentLifecycleQuery`),
  **005** (almacenes) y **014** (captura).

### Parallel Opportunities

- Foundational: T003, T005, T006, T008 en paralelo; T004 tras T003.
- US2: tests T009–T011 en paralelo; T015 con backend.
- US3/US1: tests en paralelo; frontend con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Consistencia de estado primero: "en traslado" bloquea asignación/baja; transiciones transaccionales.
- Sin PII; resolución de código server-side; auditoría por id.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).

### Estado de implementación (2026-06-30)

- **Completado:** módulo backend `transfers` (entidades, `V16`, DTOs, repos, servicio con
  crear/recibir/cancelar/listar, controller con `@PreAuthorize`), puerto `TransferLifecycleQuery`
  (@Primary, compone asignación + traslado) y guard en `EquipmentService.changeStatus`; **10
  pruebas** backend nuevas verdes (78 en total, `BUILD SUCCESS`). Frontend `traslados` (listado con
  recibir/cancelar, alta con captura QR **014**), rutas y entrada de menú. Build de producción limpio.
- **Diferido (`[~]`):** tests de contrato/integración T009/T010/T017/T021 (sin infra
  Testcontainers/MockMvc, como en specs previas; lógica cubierta a nivel unitario); T006 (puerto
  `EquipmentStateChanger` — se usa `EquipmentRepository` directo, precedente de 004); T025
  (recepción parcial, decisión de diseño); T027 (quickstart, requiere SQL Server).
- **Cross-cutting:** cierra la integración **T014 de la spec 014** (captura QR en alta de traslado);
  `AssignmentLifecycleQuery` deja de ser `@Primary` (ahora lo compone `TransferLifecycleQuery`).
