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

- [ ] T001 Crear la estructura de paquetes del módulo `transfers` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/transfers/`
- [ ] T002 [P] Preparar la feature frontend `<fe>/traslados/` (`pages/traslados/`, `pages/traslado-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [ ] T003 [P] Crear entidades `Transfer` (origen, destino, estado, fechas, creado_por, recibido_por) y `TransferItem` (transfer ↔ equipment) en `<be>/transfers/entity/`
- [ ] T004 Crear la migración Flyway `V{n}__create_transfer.sql` (`transfer` + `transfer_item`; FKs a `warehouse`/`equipment`; índices por estado/origen/destino) — usar el siguiente número Flyway libre
- [ ] T005 [P] Definir DTOs `TransferCreateRequest`, `TransferSummary`, `TransferDetail` en `<be>/transfers/dto/`
- [ ] T006 [P] Definir el puerto `EquipmentStateChanger` (cambiar estado disponible↔en_traslado, actualizar almacén) implementado por **001** en `<be>/transfers/service/`
- [ ] T007 Configurar **autorización por rol** para `/transfers/**` (crear/recibir/cancelar restringido; consulta a roles operativos; rechazo por defecto)
- [ ] T008 [P] Reusar el escritor de **auditoría** (012) para `CREATE/RECEIVE/CANCEL_TRANSFER`; si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 2 - Registrar un nuevo traslado (Priority: P1) 🎯 MVP

**Goal**: crear traslado origen→destino agregando fornituras disponibles del origen por QR; al confirmar,
quedan "en traslado" y el traslado "enviado".

**Independent Test**: crear traslado con 2 fornituras disponibles del origen → "en traslado" + "enviado"
con fecha de envío; agregar una no disponible/asignada o de otro almacén → bloqueada con motivo.

### Tests for User Story 2

- [ ] T009 [P] [US2] Test de contrato `POST /transfers` (valida origen/destino, disponibilidad y ubicación; estados resultantes) en `<bet>/transfers/TransferCreateContractTest.java`
- [ ] T010 [P] [US2] Test de integración: confirmar → fornituras "en traslado", traslado "enviado", fecha de envío; agregar inválida → rechazo con motivo en `<bet>/transfers/TransferCreateIntegrationTest.java`
- [ ] T011 [P] [US2] Test: una fornitura "en traslado" **no** puede asignarse/darse de baja (vía `EquipmentLifecycleQuery`) en `<bet>/transfers/TransferLockTest.java`

### Implementation for User Story 2

- [ ] T012 [US2] Implementar `TransferRepository`/`TransferItemRepository` (paginación, por estado/almacén) en `<be>/transfers/repository/`
- [ ] T013 [US2] Implementar `TransferService.create()` (validar disponibilidad + `warehouse_id==origen`, pasar fornituras a "en traslado" vía `EquipmentStateChanger`, estado "enviado", transacción, auditar) en `<be>/transfers/service/`
- [ ] T014 [US2] Implementar `POST /transfers` en `TransferController` en `<be>/transfers/controller/`
- [ ] T015 [P] [US2] Frontend: `transfers.service.ts` (crear, agregar por código, listar) en `<fe>/traslados/data/`
- [ ] T016 [US2] Frontend: página `traslado-form` (selección origen/destino + captura por QR con componente **014** acumulando en tabla previa + confirmar) en `<fe>/traslados/pages/traslado-form/`

**Checkpoint**: se pueden crear traslados de forma segura (MVP).

---

## Phase 4: User Story 3 - Recibir un traslado (Priority: P2)

**Goal**: confirmar recepción en destino; las fornituras quedan "disponible" bajo el almacén destino con
fecha de recepción.

**Independent Test**: recibir un traslado "enviado" → "recibido", fornituras "disponibles" en destino,
fecha registrada.

### Tests for User Story 3

- [ ] T017 [P] [US3] Test de contrato/integración `POST /transfers/{id}/receive` (estado→recibido, fornituras→disponible en destino, `warehouse_id` actualizado, fecha) en `<bet>/transfers/TransferReceiveTest.java`

### Implementation for User Story 3

- [ ] T018 [US3] Implementar `TransferService.receive()` (transacción: estado, liberar fornituras a "disponible" en destino, actualizar `warehouse_id`, auditar) en `<be>/transfers/service/`
- [ ] T019 [US3] Implementar `POST /transfers/{id}/receive` en `<be>/transfers/controller/`
- [ ] T020 [US3] Frontend: acción "Recibir" en la lista de traslados en `<fe>/traslados/pages/traslados/`

**Checkpoint**: ciclo enviar→recibir cerrado.

---

## Phase 5: User Story 1 - Consultar traslados (Priority: P2)

**Goal**: listado paginado con filtros (origen, destino, estado) y botón "Nuevo traslado".

**Independent Test**: con traslados existentes, la lista los muestra paginados; filtrar por estado/
almacén acota correctamente.

### Tests for User Story 1

- [ ] T021 [P] [US1] Test de contrato `GET /transfers` (paginación + filtros origen/destino/estado) en `<bet>/transfers/TransferListContractTest.java`

### Implementation for User Story 1

- [ ] T022 [US1] Implementar `GET /transfers` (paginado + filtros) en `<be>/transfers/controller/`
- [ ] T023 [US1] Frontend: página de listado (tabla paginada: origen, destino, estado, fechas, acciones) + filtros en `<fe>/traslados/pages/traslados/`

**Checkpoint**: visibilidad completa de los movimientos.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T024 Implementar `POST /transfers/{id}/cancel` (revertir fornituras a "disponible" en origen, estado "cancelado", auditar) en `<be>/transfers/controller/`
- [ ] T025 [P] *(diferido)* Evaluar **recepción parcial** → ADR si se adopta; documentar la decisión
- [ ] T026 [P] Tests unitarios de transición de estados y validación de origen en `<bet>/transfers/`
- [ ] T027 Validar el quickstart (crear, bloquear asignación de "en traslado", recibir, cancelar) y registrar resultados

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
