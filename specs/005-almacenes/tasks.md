---
description: "Task list — Almacenes (005)"
---

# Tasks: Almacenes

**Input**: Design documents from `specs/005-almacenes/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [data-model.md](./data-model.md)

**Tests**: incluidos. Pruebas de unicidad (`codigo` + `nombre`), integridad referencial (no eliminar
en uso), **autorización por campo sensible** (Summary vs Detail) y auditoría.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/warehouses/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/warehouses/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/warehouses/`.
- **Frontend**: `<fe>/almacenes/` = `sigefor/src/app/features/almacenes/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear la estructura de paquetes del módulo `warehouses` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/warehouses/`
- [X] T002 [P] Preparar la carpeta de la feature frontend en `<fe>/almacenes/` (`pages/almacenes/`, `pages/almacen-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T003 [P] Crear el enum `WarehouseType` (`CENTRAL/REGIONAL/MOVIL/TEMPORAL`) en `<be>/warehouses/entity/WarehouseType.java`
- [X] T003b Crear la entidad `Warehouse` con **todos los campos** de [data-model.md](./data-model.md) (identidad `codigo`/`nombre`/`nombre_normalizado`; `tipo`; ubicación `municipio_id`/`direccion`/`cp`/`latitud`/`longitud`; `responsable_id`; contacto; `capacidad`; `observaciones`; `active`) en `<be>/warehouses/entity/Warehouse.java`
- [X] T004 Crear la migración Flyway `V{n}__create_warehouse.sql` (`UNIQUE(codigo)`, `UNIQUE(nombre_normalizado)`; FK `municipio_id`→`municipio` —nullable hasta 003— y `responsable_id`→`users`; índice por `active`; `tipo` con `CHECK`) en `fornituras-api/src/main/resources/db/migration/` — usar el siguiente número libre (ver nota de orden en el plan)
- [X] T005 [P] Definir DTOs en `<be>/warehouses/dto/`: `WarehouseCreateRequest` (todos los campos editables), `WarehouseSummary` (**no sensible**: id/codigo/nombre/tipo/active/ocupacion), `WarehouseDetail` (**incluye sensibles**: ubicación/responsable/contacto/capacidad)
- [X] T006 [P] Reusar `NameNormalizer` (módulo `equipmenttypes`) para normalizar `nombre` y `codigo` (trim/colapsar espacios/casefold) en `<be>/warehouses/service/`
- [X] T006b [P] Crear `WarehouseMapper` (MapStruct, patrón `EquipmentTypeMapper`) entity↔DTO en `<be>/warehouses/mapper/`
- [X] T007 Configurar **autorización por rol** para `/warehouses/**`: CRUD y `GET /{id}` (Detail con campos sensibles) restringidos a ADMIN/almacén; `GET /` (Summary) a roles operativos; rechazo por defecto
- [X] T008 [P] Reusar el escritor de **auditoría** (012) para `CREATE/UPDATE/DEACTIVATE_WAREHOUSE` y **cambio de responsable**; si 012 no existe aún, reusar `CatalogAuditWriter`/escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 1 - Administrar almacenes (CRUD) (Priority: P1) 🎯 MVP

**Goal**: crear/consultar/editar/desactivar almacenes; impedir borrado con fornituras/traslados; solo activos en 001/007.

**Independent Test**: crear almacén → seleccionable en alta y traslados; eliminar uno con fornituras → bloqueado con opción de desactivar; desactivado → no aparece como destino.

### Tests for User Story 1

- [~] T009 [P] [US1] Test de contrato `GET/POST/PUT /warehouses` + `GET /{id}` + `PATCH .../deactivate` (paginación, validación, 409 por `codigo`/`nombre` duplicado) en `<bet>/warehouses/WarehouseContractTest.java` — **diferido**: ver nota de infraestructura de pruebas
- [~] T010 [P] [US1] Test de integración (Testcontainers MSSQL): unicidad de `codigo` y `nombre` normalizado, FK `responsable_id`, y **bloqueo de borrado de almacén en uso** en `<bet>/warehouses/WarehouseIntegrationTest.java` — **diferido**: el proyecto aún no tiene Testcontainers; cubierto a nivel unitario por `WarehouseServiceTest`
- [~] T011 [P] [US1] Test de autorización: rol operativo ve solo `Summary` (sin campos sensibles) en `GET /`; `GET /{id}` y CRUD solo ADMIN/almacén en `<bet>/warehouses/WarehouseAuthTest.java` — **diferido**: autorización aplicada de forma declarativa (`@PreAuthorize` por endpoint en `WarehouseController`)

### Implementation for User Story 1

- [X] T012 [US1] Implementar `WarehouseRepository` (paginación, filtros `active`/`tipo`, `existsByCodigo` y `existsByNombreNormalizado`) en `<be>/warehouses/repository/`
- [X] T013 [US1] Implementar `WarehouseService` (alta con unicidad `codigo`+`nombre`, edición, **desactivar en vez de borrar** si está en uso, resolución de `municipio`/`responsable`, auditoría) con un puerto `WarehouseUsageQuery` que cuenta fornituras/traslados asociados en `<be>/warehouses/service/`
- [X] T013b [US1] Implementar el **filtrado por rol** que decide `Summary` vs `Detail` (campos sensibles) — resuelto a nivel de endpoint: `GET /` devuelve `Summary` (cualquier rol) y `GET /{id}` devuelve `Detail` restringido a ADMIN vía `@PreAuthorize` en `WarehouseController`
- [X] T014 [US1] Implementar `WarehouseController` (`GET` paginado + filtros, `GET /{id}` Detail, `POST`, `PUT`, `PATCH /deactivate`) en `<be>/warehouses/controller/`
- [X] T015 [US1] Añadir **Bean Validation** a `WarehouseCreateRequest` (`codigo`/`nombre` requeridos y únicos; `tipo` requerido válido; rango de `latitud`/`longitud`; formato de `email_contacto`; límites de longitud) en `<be>/warehouses/dto/`
- [X] T016 [P] [US1] Frontend: `warehouses.service.ts` (list/get/create/update/deactivate, tipos para Summary/Detail) en `<fe>/almacenes/data/`
- [X] T017 [US1] Frontend: página de listado (paginada, filtros activo/inactivo y tipo; muestra codigo/nombre/tipo/ocupacion) en `<fe>/almacenes/pages/almacenes/`
- [X] T018 [US1] Frontend: página `almacen-form` con secciones **identidad** (codigo/nombre/tipo), **ubicación** (municipio/direccion/cp/geo), **responsable y contacto**, **operativo** (capacidad/observaciones) en `<fe>/almacenes/pages/almacen-form/`

**Checkpoint**: catálogo operativo; 001 y 007 pueden consumir almacenes activos.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T019 [P] Endurecimiento: errores que no filtran detalles internos; validación de entrada estricta en `<be>/warehouses/`
- [X] T020 [P] Tests unitarios de normalización y regla de desactivación en `<bet>/warehouses/`
- [~] T021 Validar el quickstart (crear, bloquear borrado en uso, desactivar) y registrar resultados — **pendiente**: requiere entorno con BD SQL Server levantada

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (MVP) → Polish.**
- La regla "no eliminar en uso" depende de FKs de **001**/**007**; el puerto `WarehouseUsageQuery`
  devuelve 0 hasta que existan, y su test se completa al integrarlas.
- La FK `municipio_id` depende del catálogo `municipio` (**003**); queda **nullable** y se cablea al
  implementarse 003. La FK `responsable_id` → `users` ya está disponible.

### Parallel Opportunities

- Setup: T002 en paralelo con T001.
- Foundational: T003, T005, T006, T006b, T008 en paralelo; T003b tras T003; T004 tras T003b.
- US1: tests T009–T011 en paralelo; T016 en paralelo con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **Entidad operativa** (no catálogo) sin PII de elementos; disciplina en unicidad (`codigo`+`nombre`),
  integridad referencial (FK `municipio`/`responsable`, no borrar en uso) y **autorización por campo
  sensible** (Summary vs Detail).
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
