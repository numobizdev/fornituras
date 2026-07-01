---
description: "Task list — Catálogos genéricos (catalog → catalog_item) (006)"
---

# Tasks: Catálogos genéricos (catalog → catalog_item)

**Input**: Design documents from `specs/006-tipos-fornitura/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [data-model.md](./data-model.md)

> **Estado: IMPLEMENTADO** (regenerado 2026-06-30). Las tareas reflejan el **modelo genérico real**
> (`catalog`/`catalog_item`, módulo `modules/catalog`, Flyway `V15`+`V17`, frontend `core/catalog`),
> no el modelo tipado previo (`equipment_type`/`size`, módulo `equipmenttypes`, `V8`). `[x]` = ya
> construido; `[ ]` = pendiente real (deuda de cobertura o endurecimiento).

**Tests**: el proyecto exige autorización y auditoría; el catálogo añade pruebas de unicidad por
catálogo, resolución por `code` e integridad referencial (no eliminar en uso). **Deuda saldada
(2026-07-01):** se añadió la suite dedicada del módulo `catalog` (`CatalogContractTest`,
`CatalogIntegrationTest`, `CatalogAuthTest`, `CatalogHierarchyTest`, 18 pruebas) sobre H2/MockMvc,
además de la cobertura indirecta de `EquipmentServiceTest`.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/catalog/`
  (abreviado `<be>/catalog/`); migraciones en `fornituras-api/src/main/resources/db/migration/`;
  pruebas en `fornituras-api/src/test/java/.../modules/` (abreviado `<bet>/`).
- **Frontend**: `sigefor/src/app/core/catalog/` (cliente genérico) y
  `sigefor/src/app/features/tipos/` (feature de administración; abreviado `<fe>/tipos/`).

---

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 Crear la estructura de paquetes del módulo genérico `catalog` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/catalog/`
- [x] T002 [P] Crear el cliente genérico de frontend `core/catalog/` y la feature `tipos/` (`pages/tipos/`, `pages/tipo-form/`, `data/`) en `sigefor/src/app/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [x] T003 [P] Crear entidades `Catalog` (code único, nombre, is_system, active) y `CatalogItem` (nombre_normalizado, foto_url, `parent_item_id` self, orden, active) en `<be>/catalog/entity/`
- [x] T004 Crear la migración Flyway `V15__generic_catalog.sql` (crea `catalog`/`catalog_item`, índices de unicidad por catálogo distinguiendo por padre, siembra `TIPO_FORNITURA`/`TALLA`/`TIPO_ALMACEN`, migra `equipment_type`/`size` y el enum `warehouse_type`, repunta FKs) en `fornituras-api/src/main/resources/db/migration/`
- [x] T005 Crear la migración Flyway `V17__rename_tipo_fornitura_to_tipo_prenda.sql` (renombra el catálogo a `TIPO_PRENDA`, siembra el único valor "Fornitura", repunta equipment/tallas) en `fornituras-api/src/main/resources/db/migration/`
- [x] T006 [P] Definir `CatalogCodes` (`TIPO_PRENDA`, `TALLA`, `TIPO_ALMACEN`) para evitar strings mágicos en los consumidores en `<be>/catalog/CatalogCodes.java`
- [x] T007 [P] Definir DTOs `CatalogSummary`, `CatalogItemSummary`, `CatalogItemCreateRequest` y el `CatalogMapper` en `<be>/catalog/dto/` y `<be>/catalog/mapper/`
- [x] T008 [P] Implementar la **normalización** de nombre (trim/colapsar espacios/casefold) para unicidad por catálogo, reutilizando `common/text/CodeNormalizer` desde `<be>/catalog/service/`
- [x] T009 Configurar **autorización por rol** para `/catalogs/**` y `/catalog-items/**` (administración restringida a ADMIN; consulta a roles operativos; rechazo por defecto) en la config de Spring Security
- [x] T010 [P] Cablear el escritor de **auditoría** para `CREATE/UPDATE/DEACTIVATE_CATALOG_ITEM` — hecho vía `common/audit/AuditWriter` (registro interino SLF4J con actor/acción/id, sin PII); su implementación se reemplazará por el puerto definitivo cuando exista 012

**Checkpoint**: fundamento del catálogo genérico listo.

---

## Phase 3: User Story 1 - Administrar valores de un catálogo (CRUD) (Priority: P1) 🎯 MVP

**Goal**: crear/consultar/editar/desactivar valores de cualquier catálogo por su `code`; impedir borrado en uso; solo activos donde el catálogo se consume.

**Independent Test**: "Fornitura" existe en `TIPO_PRENDA` y es seleccionable en el alta de fornitura; crear un valor con nombre duplicado en el mismo catálogo → 409; intentar eliminar un valor en uso → bloqueado con opción de desactivar; desactivado → no aparece como opción.

### Tests for User Story 1

- [x] T011 [P] [US1] Test de contrato del CRUD genérico (`GET /api/v1/catalogs/{code}/items` paginado + `active`, `POST`, `PUT /catalogs/items/{id}`, `PATCH /catalogs/items/{id}/deactivate`; 409 por nombre duplicado por catálogo; 404 por `code` inexistente; 400 por nombre en blanco y por foto con esquema inseguro) en `<bet>/catalog/CatalogContractTest.java`
- [x] T012 [P] [US1] Test de integración (carga de contexto sobre H2/JPA): unicidad de `nombre_normalizado` por catálogo, resolución por `code` y **conservación de la fila al desactivar un valor en uso** (la API no ofrece borrado físico; la referencia de `equipment` sigue resolviéndose) en `<bet>/catalog/CatalogIntegrationTest.java`
- [x] T013 [P] [US1] Test de autorización: rol operativo solo consulta; ADMIN administra en `<bet>/catalog/CatalogAuthTest.java`
- [x] T014 [P] [US1] Cobertura indirecta: `EquipmentServiceTest` verifica que `requireActiveItem(..., TIPO_PRENDA)` rechaza un tipo inactivo al dar de alta una fornitura en `<bet>/equipment/EquipmentServiceTest.java`

### Implementation for User Story 1

- [x] T015 [US1] Implementar `CatalogRepository`/`CatalogItemRepository` (buscar catálogo por `code`, paginación por catálogo, filtro `active`, existencia por nombre normalizado) en `<be>/catalog/repository/`
- [x] T016 [US1] Implementar `CatalogService` (resolver por `code`, alta con unicidad, edición, **desactivar en vez de borrar**, `requireActiveItem` para los consumidores) en `<be>/catalog/service/`
- [x] T017 [US1] Implementar `CatalogController` (CRUD genérico parametrizado por `code`, paginado) en `<be>/catalog/controller/`
- [x] T018 [US1] Añadir **Bean Validation** a `CatalogItemCreateRequest` (nombre requerido, límites de descripción) en `<be>/catalog/dto/`
- [x] T019 [P] [US1] Frontend: cliente genérico `catalog.service.ts` + modelo `catalog.model.ts` (`CATALOG_CODES`) en `<fe>/../core/catalog/`
- [x] T020 [P] [US1] Frontend: adaptador `equipment-types.service.ts` (mapea el CRUD genérico a la API histórica de tipos, consumiendo `CATALOG_CODES.TIPO_PRENDA`) en `<fe>/tipos/data/`
- [x] T021 [US1] Frontend: página de listado de valores (paginada, filtro activo/inactivo) en `<fe>/tipos/pages/tipos/`
- [x] T022 [US1] Frontend: página `tipo-form` (nombre, descripción, foto) en `<fe>/tipos/pages/tipo-form/`

**Checkpoint**: CRUD genérico operativo; 001 consume "Fornitura" (`TIPO_PRENDA`) y 005 consume `TIPO_ALMACEN`.

---

## Phase 4: User Story 2 - Catálogos dependientes (jerarquía item→item) (Priority: P2)

**Goal**: soportar valores que dependen de otro valor vía `parent_item_id` (p. ej. `TALLA` colgada de `TIPO_PRENDA`); un valor sin padre es global.

**Independent Test**: crear la talla "M" ligada al tipo de prenda "Fornitura"; al capturar una fornitura, "M" aparece; una talla global aparece para cualquier fornitura.

### Tests for User Story 2

- [x] T023 [P] [US2] Test de integración de jerarquía: listar valores por `parentItemId`, unicidad de nombre por (catálogo, padre) —mismo nombre bajo padres distintos permitido; repetido bajo el mismo padre rechazado; globales independientes de las hijas— y que una talla ligada solo se ofrezca para su tipo en `<bet>/catalog/CatalogHierarchyTest.java`. **Nota:** este test destapó que `CatalogService` validaba unicidad solo por catálogo (ignorando el padre); se corrigió a unicidad por (catálogo, padre) según data-model (índices `uk_catalog_item_named`/`uk_catalog_item_named_child`). Se añadió además `CatalogUniquenessMigrationTest` como guarda a nivel de esquema (verifica que `V15` declara ambos índices únicos filtrados, ya que H2 no soporta índices filtrados y el motor real se valida en CI, ADR 0009)

### Implementation for User Story 2

- [x] T024 [US2] Soporte de `parent_item_id` en `CatalogItem` e índices de unicidad filtrados por padre (parte de `V15`) en `<be>/catalog/entity/` y la migración
- [x] T025 [US2] `CatalogService`/repos: listar items activos por catálogo **y padre** (`listActiveItems(code, parentId)`) en `<be>/catalog/service/`
- [x] T026 [P] [US2] Frontend: en el adaptador, `listSizes`/`createSize` sobre `CATALOG_CODES.TALLA` con `parentItemId` = tipo de prenda en `<fe>/tipos/data/equipment-types.service.ts`
- [x] T027 [US2] Frontend: gestión de tallas por tipo en `tipo-form` (listar/agregar/quitar tallas del tipo) en `<fe>/tipos/pages/tipo-form/`

**Checkpoint**: catálogos dependientes operativos; el mecanismo queda listo para futuros tipos de prenda.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [x] T028 [P] Endurecimiento: la foto se guarda **por referencia** (`foto_url`), no hay endpoint de subida, así que la validación de MIME/tamaño no aplica; en su lugar se añadió una **guarda de esquema** en `CatalogItemCreateRequest.fotoUrl` (`@Pattern`) que rechaza esquemas peligrosos (`javascript:`, `data:`, `vbscript:`…) admitiendo http(s) y rutas relativas, evitando XSS almacenado al renderar la foto. Los errores del handler global no filtran detalles internos
- [x] T029 [P] Cerrar la deuda de tests dedicados del módulo `catalog` (T011–T013, T023) — hecho: 18 pruebas nuevas
- [x] T030 Validar el quickstart y registrar resultados — **verificado (2026-07-01)** vía la suite dedicada: nombre duplicado por catálogo → 409 (`CatalogContractTest`/`CatalogIntegrationTest`); valor en uso → sin borrado físico, desactivación conserva la fila y la referencia sigue resolviéndose (`CatalogIntegrationTest`); desactivado → deja de listarse como activo y `requireActiveItem` lo rechaza; foto con esquema inseguro → 400. Suite completa del backend: **120 tests, 0 fallos, 0 errores**

---

## Dependencies & Execution Order

- **Setup (Phase 1)** → **Foundational (Phase 2, BLOQUEA)** → **US1 (Phase 3, MVP)** → **US2 (Phase 4)** → **Polish (Phase 5)**.
- US2 depende de la estructura de US1 (misma tabla `catalog_item` + `parent_item_id`); es incremento sobre el CRUD genérico.
- La regla "no eliminar en uso" depende de las FKs de **001** (`equipment`) y **005** (`warehouse`).

### Parallel Opportunities

- Setup: T002 en paralelo con T001.
- Foundational: T003, T006, T007, T008 en paralelo; T004 antes de T005 (rename tras estructura).
- US1: tests T011–T013 en paralelo; frontend T019/T020 en paralelo con el backend.
- US2: T026 en paralelo con el resto.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Catálogo de soporte sin PII; la disciplina es unicidad por catálogo, integridad referencial
  (validación por `code` en servicio, ADR 0007) y autorización.
- **Deuda saldada (2026-07-01):** T010 (auditoría vía `AuditWriter`), T011–T013/T023 (suite dedicada
  del módulo `catalog`) y T028 (guarda de esquema en `fotoUrl`). Corrección incluida: unicidad de
  nombre por (catálogo, padre) en `CatalogService` (antes solo por catálogo). Todas las tareas del
  feature quedan en `[x]`; el registro de auditoría definitivo (bitácora ISO 27001) llega con 012.
