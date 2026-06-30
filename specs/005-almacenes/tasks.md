---
description: "Task list — Almacenes (005)"
---

# Tasks: Almacenes

**Input**: Design documents from `specs/005-almacenes/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de unicidad, integridad referencial (no eliminar en uso), autorización
y auditoría.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/warehouses/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/warehouses/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/warehouses/`.
- **Frontend**: `<fe>/almacenes/` = `sigefor/src/app/features/almacenes/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Crear la estructura de paquetes del módulo `warehouses` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/warehouses/`
- [ ] T002 [P] Preparar la carpeta de la feature frontend en `<fe>/almacenes/` (`pages/almacenes/`, `pages/almacen-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [ ] T003 [P] Crear la entidad `Warehouse` (nombre/identificador único, ubicación/descripción, active) en `<be>/warehouses/entity/Warehouse.java`
- [ ] T004 Crear la migración Flyway `V{n}__create_warehouse.sql` (`UNIQUE(nombre)` normalizado; índice por `active`) en `fornituras-api/src/main/resources/db/migration/` — usar el siguiente número libre (ver nota de orden en el plan)
- [ ] T005 [P] Definir DTOs `WarehouseCreateRequest`, `WarehouseSummary`, `WarehouseDetail` en `<be>/warehouses/dto/`
- [ ] T006 [P] Implementar normalización del nombre (trim/colapsar espacios/casefold) para unicidad en `<be>/warehouses/service/`
- [ ] T007 Configurar **autorización por rol** para `/warehouses/**` (CRUD restringido; consulta a roles operativos; rechazo por defecto)
- [ ] T008 [P] Reusar el escritor de **auditoría** (012) para `CREATE/UPDATE/DEACTIVATE_WAREHOUSE`; si 012 no existe aún, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 1 - Administrar almacenes (CRUD) (Priority: P1) 🎯 MVP

**Goal**: crear/consultar/editar/desactivar almacenes; impedir borrado con fornituras/traslados; solo activos en 001/007.

**Independent Test**: crear almacén → seleccionable en alta y traslados; eliminar uno con fornituras → bloqueado con opción de desactivar; desactivado → no aparece como destino.

### Tests for User Story 1

- [ ] T009 [P] [US1] Test de contrato `GET/POST/PUT /warehouses` + `PATCH .../deactivate` (paginación, validación, 409 por duplicado) en `<bet>/warehouses/WarehouseContractTest.java`
- [ ] T010 [P] [US1] Test de integración (Testcontainers MSSQL): unicidad de nombre normalizado y **bloqueo de borrado de almacén en uso** en `<bet>/warehouses/WarehouseIntegrationTest.java`
- [ ] T011 [P] [US1] Test de autorización: rol operativo solo consulta; admin/almacén administra en `<bet>/warehouses/WarehouseAuthTest.java`

### Implementation for User Story 1

- [ ] T012 [US1] Implementar `WarehouseRepository` (paginación, filtro `active`, existsByNombre normalizado) en `<be>/warehouses/repository/`
- [ ] T013 [US1] Implementar `WarehouseService` (alta con unicidad, edición, **desactivar en vez de borrar** si está en uso, auditoría) con un puerto `WarehouseUsageQuery` que cuenta fornituras/traslados asociados en `<be>/warehouses/service/`
- [ ] T014 [US1] Implementar `WarehouseController` (`GET` paginado + `active`, `POST`, `PUT`, `PATCH /deactivate`) en `<be>/warehouses/controller/`
- [ ] T015 [US1] Añadir **Bean Validation** a `WarehouseCreateRequest` (nombre requerido/único; límites de ubicación/descripción) en `<be>/warehouses/dto/`
- [ ] T016 [P] [US1] Frontend: `warehouses.service.ts` (list/create/update/deactivate) en `<fe>/almacenes/data/`
- [ ] T017 [US1] Frontend: página de listado (paginada, filtro activo/inactivo) en `<fe>/almacenes/pages/almacenes/`
- [ ] T018 [US1] Frontend: página `almacen-form` (nombre, ubicación/descripción) en `<fe>/almacenes/pages/almacen-form/`

**Checkpoint**: catálogo operativo; 001 y 007 pueden consumir almacenes activos.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T019 [P] Endurecimiento: errores que no filtran detalles internos; validación de entrada estricta en `<be>/warehouses/`
- [ ] T020 [P] Tests unitarios de normalización y regla de desactivación en `<bet>/warehouses/`
- [ ] T021 Validar el quickstart (crear, bloquear borrado en uso, desactivar) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (MVP) → Polish.**
- La regla "no eliminar en uso" depende de FKs de **001**/**007**; el puerto `WarehouseUsageQuery`
  devuelve 0 hasta que existan, y su test se completa al integrarlas.

### Parallel Opportunities

- Setup: T002 en paralelo con T001.
- Foundational: T003, T005, T006, T008 en paralelo; T004 tras T003.
- US1: tests T009–T011 en paralelo; T016 en paralelo con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Catálogo de soporte sin PII; disciplina en unicidad, integridad referencial y autorización.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
