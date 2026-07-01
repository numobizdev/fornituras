---
description: "Task list — Almacenes (005)"
---

# Tasks: Almacenes

**Input**: Design documents from `specs/005-almacenes/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [data-model.md](./data-model.md)

> **Estado: IMPLEMENTADO** (regenerado 2026-06-30). Refleja el **modelo real** tras ADR 0007: el
> `tipo` es una **FK a `catalog_item`** (`tipo_item_id`, catálogo `TIPO_ALMACEN`) y `municipio`/
> `estado` son **texto libre** (sin FK). Se retiró el enum `WarehouseType` y el módulo
> `equipmenttypes`. `[X]` = construido; `[~]` = diferido; `[ ]` = pendiente real.

**Tests**: unicidad (`codigo` + `nombre`), integridad referencial (no eliminar en uso), resolución
del tipo contra `TIPO_ALMACEN`, **autorización por campo sensible** (Summary vs Detail) y auditoría.

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

- [X] T003 Crear la entidad `Warehouse` con **todos los campos** de [data-model.md](./data-model.md): identidad (`codigo`/`nombre`/`nombre_normalizado`); clasificación `tipo_item_id` (FK → `catalog_item`); ubicación `municipio`/`estado` (**texto libre**), `direccion`/`cp`/`latitud`/`longitud`; `responsable_id`; contacto; `capacidad`; `observaciones`; `active`, en `<be>/warehouses/entity/Warehouse.java`
- [X] T004 Crear la migración Flyway `V10__create_warehouse.sql` (`UNIQUE(codigo)`, `UNIQUE(nombre_normalizado)`; `responsable_id`→`users`; índice por `active`) en `fornituras-api/src/main/resources/db/migration/`
- [X] T005 Repuntar `warehouse` en `V15__generic_catalog.sql`: `tipo` (enum) → `tipo_item_id` (FK → `catalog_item` de `TIPO_ALMACEN`) y `municipio_id` (FK) → `municipio`/`estado` de **texto libre** (ADR 0007), en `fornituras-api/src/main/resources/db/migration/`
- [X] T006 [P] Definir DTOs en `<be>/warehouses/dto/`: `WarehouseCreateRequest` (todos los campos editables, con `tipoItemId`), `WarehouseSummary` (**no sensible**: id/codigo/nombre/tipoItemId/active/ocupacion), `WarehouseDetail` (**incluye sensibles**: ubicación/responsable/contacto/capacidad)
- [X] T007 [P] Reusar el normalizador de `common/text` para `nombre` y `codigo` (trim/colapsar espacios/casefold/sin acentos) desde `<be>/warehouses/service/`
- [X] T008 [P] Crear `WarehouseMapper` (entity↔DTO) en `<be>/warehouses/mapper/`
- [X] T009 Configurar **autorización por rol** para `/warehouses/**`: CRUD y `GET /{id}` (Detail con campos sensibles) restringidos a ADMIN/almacén; `GET /` (Summary) a roles operativos; rechazo por defecto (`@PreAuthorize` por endpoint)
- [ ] T010 [P] Cablear el escritor de **auditoría** (012) para `CREATE/UPDATE/DEACTIVATE_WAREHOUSE` y **cambio de responsable** — pendiente hasta que 012 exista

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 1 - Administrar almacenes (CRUD) (Priority: P1) 🎯 MVP

**Goal**: crear/consultar/editar/desactivar almacenes; impedir borrado con fornituras/traslados; solo activos en 001/007.

**Independent Test**: crear almacén (con tipo de `TIPO_ALMACEN`) → seleccionable en alta y traslados; eliminar uno con fornituras → bloqueado con opción de desactivar; desactivado → no aparece como destino.

### Tests for User Story 1

- [~] T011 [P] [US1] Test de contrato `GET/POST/PUT /warehouses` + `GET /{id}` + `PATCH .../deactivate` (paginación, validación, 409 por `codigo`/`nombre` duplicado) en `<bet>/warehouses/WarehouseContractTest.java` — **diferido**
- [~] T012 [P] [US1] Test de integración (carga de contexto + Flyway): unicidad de `codigo`/`nombre`, FK `responsable_id`, resolución de `tipo_item_id` contra `TIPO_ALMACEN` y **bloqueo de borrado en uso** en `<bet>/warehouses/WarehouseIntegrationTest.java` — **diferido**; cubierto a nivel unitario por `WarehouseServiceTest`
- [~] T013 [P] [US1] Test de autorización: rol operativo ve solo `Summary`; `GET /{id}` y CRUD solo ADMIN/almacén en `<bet>/warehouses/WarehouseAuthTest.java` — **diferido**; autorización declarativa (`@PreAuthorize`)
- [X] T014 [P] [US1] `WarehouseServiceTest` (unicidad, desactivación, conteo de uso vía puerto) en `<bet>/warehouses/WarehouseServiceTest.java`

### Implementation for User Story 1

- [X] T015 [US1] Implementar `WarehouseRepository` (paginación, filtros `active`/`tipoItemId`, `existsByCodigo` y `existsByNombreNormalizado`) en `<be>/warehouses/repository/`
- [X] T016 [US1] Implementar `WarehouseService` (alta con unicidad `codigo`+`nombre`, edición, **desactivar en vez de borrar** si está en uso, **resolución del `tipoItemId` contra `TIPO_ALMACEN`** vía `CatalogService`, auditoría) en `<be>/warehouses/service/`
- [X] T017 [US1] Implementar el puerto `WarehouseUsageQuery` + `DefaultWarehouseUsageQuery` (cuenta fornituras/traslados asociados) en `<be>/warehouses/service/`
- [X] T018 [US1] Implementar `WarehouseController` (`GET` paginado + filtros, `GET /{id}` Detail restringido, `POST`, `PUT`, `PATCH /deactivate`) en `<be>/warehouses/controller/`
- [X] T019 [US1] Añadir **Bean Validation** a `WarehouseCreateRequest` (`codigo`/`nombre` requeridos; `tipoItemId` requerido; rango de `latitud`/`longitud`; formato de `email_contacto`; límites de longitud) en `<be>/warehouses/dto/`
- [X] T020 [P] [US1] Frontend: `warehouses.service.ts` (list/get/create/update/deactivate; tipos Summary/Detail; tipo de almacén desde `core/catalog` con `CATALOG_CODES.TIPO_ALMACEN`) en `<fe>/almacenes/data/`
- [X] T021 [US1] Frontend: página de listado (paginada, filtros activo/inactivo y tipo; muestra codigo/nombre/tipo/ocupacion) en `<fe>/almacenes/pages/almacenes/`
- [X] T022 [US1] Frontend: página `almacen-form` con secciones **identidad** (codigo/nombre/tipo), **ubicación** (municipio/estado/direccion/cp/geo), **responsable y contacto**, **operativo** (capacidad/observaciones) en `<fe>/almacenes/pages/almacen-form/`

**Checkpoint**: almacenes operativos; 001 y 007 consumen almacenes activos.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T023 [P] Endurecimiento: errores que no filtran detalles internos; validación de entrada estricta en `<be>/warehouses/`
- [X] T024 [P] Tests unitarios de normalización y regla de desactivación en `<bet>/warehouses/`
- [~] T025 Validar el quickstart (crear, bloquear borrado en uso, desactivar) y registrar resultados — **pendiente**: requiere entorno con BD SQL Server levantada

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (MVP) → Polish.**
- La regla "no eliminar en uso" depende de FKs de **001**/**007**; el puerto `WarehouseUsageQuery`
  encapsula el conteo.
- El `tipo_item_id` depende del catálogo `TIPO_ALMACEN` (**006**, módulo `catalog`). La FK
  `responsable_id` → `users` ya está disponible. `municipio`/`estado` son texto libre (sin dependencia).

### Parallel Opportunities

- Setup: T002 en paralelo con T001.
- Foundational: T006, T007, T008 en paralelo; T004 antes de T005 (repunte tras creación).
- US1: tests T011–T013 en paralelo; T020 en paralelo con el backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **Entidad operativa** (no catálogo) sin PII de elementos; disciplina en unicidad (`codigo`+`nombre`),
  integridad referencial (tipo contra `TIPO_ALMACEN`, no borrar en uso) y **autorización por campo
  sensible** (Summary vs Detail).
- **Deuda visible:** T010 (auditoría), T011–T013 (tests de contrato/integración/auth dedicados),
  T025 (validación de quickstart con BD).
