---
description: "Task list — Catálogo de tipos de fornitura (006)"
---

# Tasks: Catálogo de tipos de fornitura

**Input**: Design documents from `specs/006-tipos-fornitura/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. El proyecto exige autorización y auditoría probadas; el catálogo añade pruebas
de unicidad e integridad referencial (no eliminar en uso).

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/equipmenttypes/`
  (abreviado `<be>/equipmenttypes/`); migraciones en `fornituras-api/src/main/resources/db/migration/`;
  pruebas en `<bet>/equipmenttypes/`.
- **Frontend**: `sigefor/src/app/features/tipos/` (abreviado `<fe>/tipos/`).

---

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Crear la estructura de paquetes del módulo `equipmenttypes` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/equipmenttypes/`
- [ ] T002 [P] Preparar la carpeta de la feature frontend en `<fe>/tipos/` (`pages/tipos/`, `pages/tipo-form/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [ ] T003 [P] Crear entidades `EquipmentType` (nombre único, descripción, foto_url, active) y `Size` (etiqueta, equipment_type_id NULL, active) en `<be>/equipmenttypes/entity/`
- [ ] T004 Crear la migración Flyway `V8__create_equipment_type_and_size.sql` (`equipment_type` con `UNIQUE(nombre)` normalizado; `size` con FK nullable a `equipment_type`; índices por `active`) en `fornituras-api/src/main/resources/db/migration/`
- [ ] T005 [P] Definir DTOs `EquipmentTypeCreateRequest`/`Summary`/`Detail` y `SizeCreateRequest`/`Summary` en `<be>/equipmenttypes/dto/`
- [ ] T006 [P] Implementar utilidad de **normalización** (trim/colapsar espacios/casefold) del nombre para comparar unicidad en `<be>/equipmenttypes/service/`
- [ ] T007 Configurar **autorización por rol** para `/equipment-types/**` y `/sizes/**` (CRUD restringido; consulta a roles operativos; rechazo por defecto) en la config de Spring Security
- [ ] T008 [P] Reusar el escritor de **auditoría** (feature 012) para `CREATE/UPDATE/DEACTIVATE_EQUIPMENT_TYPE`; si 012 aún no existe, escribir a `audit_log` con un escritor mínimo

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 1 - Administrar tipos de fornitura (CRUD) (Priority: P1) 🎯 MVP

**Goal**: crear/consultar/editar/desactivar tipos y tallas; impedir borrado en uso; solo activos en 001.

**Independent Test**: crear "Chaleco antibala" → seleccionable en alta de fornitura; intentar eliminar uno en uso → bloqueado con opción de desactivar; desactivado → no aparece como opción.

### Tests for User Story 1

- [ ] T009 [P] [US1] Test de contrato `GET/POST/PUT /equipment-types` + `PATCH .../deactivate` (paginación, validación, 409 por nombre duplicado) en `<bet>/equipmenttypes/EquipmentTypeContractTest.java`
- [ ] T010 [P] [US1] Test de integración (Testcontainers MSSQL): unicidad de nombre normalizado y **bloqueo de borrado de tipo en uso** (con fornitura asociada) en `<bet>/equipmenttypes/EquipmentTypeIntegrationTest.java`
- [ ] T011 [P] [US1] Test de autorización: rol operativo solo consulta; rol admin/almacén administra en `<bet>/equipmenttypes/EquipmentTypeAuthTest.java`

### Implementation for User Story 1

- [ ] T012 [US1] Implementar `EquipmentTypeRepository`/`SizeRepository` (paginación, filtro `active`, existsByNombre normalizado, conteo de uso) en `<be>/equipmenttypes/repository/`
- [ ] T013 [US1] Implementar `EquipmentTypeService` (alta con unicidad, edición, **desactivar en vez de borrar** si está en uso, auditoría) en `<be>/equipmenttypes/service/`
- [ ] T014 [P] [US1] Implementar `SizeService` (CRUD de tallas, asociación opcional por tipo) en `<be>/equipmenttypes/service/`
- [ ] T015 [US1] Implementar `EquipmentTypeController` y `SizeController` (`GET` paginado + `active`, `POST`, `PUT`, `PATCH /deactivate`) en `<be>/equipmenttypes/controller/`
- [ ] T016 [US1] Añadir **Bean Validation** a los `*CreateRequest` (nombre requerido/único, límites de descripción; foto: MIME y tamaño) en `<be>/equipmenttypes/dto/`
- [ ] T017 [P] [US1] Frontend: `equipment-types.service.ts` (list/create/update/deactivate + tallas) en `<fe>/tipos/data/`
- [ ] T018 [US1] Frontend: página de listado de tipos/tallas (paginada, filtro activo/inactivo) en `<fe>/tipos/pages/tipos/`
- [ ] T019 [US1] Frontend: página `tipo-form` (nombre, descripción, foto, tallas) en `<fe>/tipos/pages/tipo-form/`

**Checkpoint**: catálogo operativo; 001 puede consumir tipos/tallas activos.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T020 [P] Endurecimiento: validación estricta de imagen (MIME real, tamaño máx.), errores que no filtran detalles internos, en `<be>/equipmenttypes/`
- [ ] T021 [P] Tests unitarios de normalización de nombre y regla de desactivación en `<bet>/equipmenttypes/`
- [ ] T022 Validar el quickstart (sembrar tipos, bloquear borrado en uso, desactivar) y registrar resultados

---

## Dependencies & Execution Order

- **Setup (Phase 1)** → **Foundational (Phase 2, BLOQUEA)** → **US1 (Phase 3, MVP)** → **Polish (Phase 4)**.
- La regla "no eliminar en uso" depende de la FK de **001**; si 001 no existe aún, se implementa la
  regla y su test se completa al integrar 001 (entonces el conteo de uso es real).

### Parallel Opportunities

- Setup: T002 en paralelo con T001.
- Foundational: T003, T005, T006, T008 en paralelo; T004 tras T003 (entidad antes de migración).
- US1: tests T009–T011 en paralelo; T014 y T017 en paralelo con el resto del backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Catálogo de soporte sin PII; la disciplina es unicidad, integridad referencial y autorización.
- Commit por tarea o grupo lógico; tests en rojo antes de implementar (TDD).
