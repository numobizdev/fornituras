---
description: "Task list — Migrar SEXO y TIPO_SANGRE al catálogo genérico (015)"
---

# Tasks: Migrar SEXO y TIPO_SANGRE a la estructura genérica de catálogos

**Input**: Design documents from `specs/015-catalogos-sexo-sangre/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

> **Estado: IMPLEMENTADO** (2026-06-30). `[X]` = construido; `[ ]` = pendiente real. Refactor
> data-preserving que completa el ADR 0007.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/catalog/` y `<be>/officers/` bajo
  `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/`; migraciones en
  `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/`.
- **Frontend**: `sigefor/src/app/core/catalog/` y `sigefor/src/app/features/elementos/`.

---

## Phase 1: Setup

- [X] T001 Confirmar que el CRUD genérico `modules/catalog` (006) está disponible como base (sin cambios estructurales nuevos).

---

## Phase 2: Foundational (Blocking Prerequisites)

- [X] T002 Añadir `SEXO` y `TIPO_SANGRE` a `CatalogCodes` en `<be>/catalog/CatalogCodes.java`
- [X] T003 Añadir `resolveNames(Collection<Long>)` (resolución en lote id→nombre) a `CatalogService` en `<be>/catalog/service/CatalogService.java`
- [X] T004 Crear la migración `V18__migrate_sexo_tiposangre_to_catalog.sql`: sembrar `SEXO`/`TIPO_SANGRE` en `catalog`, copiar valores a `catalog_item` (nombre/estado/orden), **repuntar** `officers.sexo_id`/`tipo_sangre_id` a `catalog_item` (join por nombre) y **retirar** `sexo`/`tipo_sangre`, en `fornituras-api/src/main/resources/db/migration/`
- [X] T005 Añadir `SEXO`/`TIPO_SANGRE` a `CATALOG_CODES` en `sigefor/src/app/core/catalog/catalog.model.ts`

---

## Phase 3: User Story 1 - Administrar SEXO/TIPO_SANGRE por el CRUD genérico (P1) 🎯

**Goal**: administrar sexo y tipo de sangre con el mismo CRUD genérico; conservar los valores existentes.

**Independent Test**: listar `SEXO`/`TIPO_SANGRE` por el CRUD genérico devuelve los valores previos; crear/desactivar funciona igual que los demás catálogos.

### Implementation for User Story 1

- [X] T006 [US1] Retirar el mecanismo específico: `entity/Sexo`, `entity/TipoSangre`, `repository/SexoRepository`, `repository/TipoSangreRepository`, `controller/OfficerCatalogController` y `dto/CatalogItem` en `<be>/officers/`
- [X] T007 [P] [US1] Frontend: `officers.service.ts` `listSexos()`/`listTiposSangre()` delegan en `core/catalog` (`listActiveItems(SEXO|TIPO_SANGRE)`), mapeando `nombre → etiqueta`, en `sigefor/src/app/features/elementos/data/`

**Checkpoint**: `SEXO`/`TIPO_SANGRE` administrables por el CRUD genérico; endpoints `/sexos`, `/tipos-sangre` retirados.

---

## Phase 4: User Story 2 - El padrón sigue funcionando igual (P1)

**Goal**: alta/consulta de elementos resuelven sexo/tipo de sangre contra `catalog_item`, sin cambio observable; los elementos existentes conservan su valor.

**Independent Test**: dar de alta un elemento (sexo/tipo de sangre desde el catálogo genérico) y consultarlo muestra los mismos valores; los elementos previos conservan su sexo/tipo de sangre tras `V18`.

### Implementation for User Story 2

- [X] T008 [US2] `OfficerService`: inyectar `CatalogService`; validar `sexoId`/`tipoSangreId` con `requireActiveItem(..., SEXO|TIPO_SANGRE)`; resolver nombres con `resolveName`/`resolveNames`, en `<be>/officers/service/OfficerService.java`
- [X] T009 [US2] Actualizar `OfficerServiceTest` para mockear `CatalogService` (en vez de los repos planos) en `<bet>/officers/OfficerServiceTest.java`

**Checkpoint**: padrón operativo contra el catálogo genérico; FKs repuntadas sin pérdida.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T010 Sincronizar documentación: `docs/03-modelo-datos.md` (SEXO/TIPO_SANGRE genéricos), ADR 0007 (pendiente cerrado) y `specs/README.md` (orden).
- [X] T011 Verificar: `mvnw clean test` (78 tests; Flyway `V18` en verde) y build de `sigefor`.

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 → US2 → Polish.**
- US2 depende del repunte de FKs de la migración (T004) y del cambio de `OfficerService` (T008).
- Depende de **006** (CRUD genérico) y consume **003** (padrón).

### Parallel Opportunities

- Foundational: T002, T003, T005 en paralelo; T004 tras T002.
- US1: T007 (frontend) en paralelo con T006 (backend).

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Refactor sin cambio de comportamiento observable; migración data-preserving (patrón `V15`/`V17`).
- Deuda del ADR 0007 **cerrada**: ya no quedan catálogos de negocio fuera de la estructura genérica
  (queda `motivo_baja` de la spec 009, que se valorará al implementar bajas).
