---
description: "Task list — Reportes y estadística (011)"
---

# Tasks: Reportes y estadística

**Input**: Design documents from `specs/011-reportes/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de que el Excel contiene exactamente las filas filtradas y respeta el
**enmascaramiento por rol**, de **auditoría de exportación**, de coincidencia de totales con 010 y de
volumen (10.000 filas sin agotar memoria).

**Organization**: por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/reports/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/reports/`;
  pruebas en `<bet>/reports/`.
- **Frontend**: `<fe>/reportes/` = `sigefor/src/app/features/reportes/`.

---

## Phase 1: Setup

- [X] T001 Crear la estructura de paquetes del módulo `reports` (`controller/`, `service/`, `repository/`, `dto/`) en `<be>/reports/`
- [X] T002 [P] Preparar la feature frontend `<fe>/reportes/` (`pages/reportes/`, `data/`)
- [X] T003 [P] **Decidir y registrar** la librería de export (Apache POI **SXSSF** streaming vs CSV en streaming) con justificación de licencia/mantenimiento (Principio VI) — nota de decisión/ADR

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T004 [P] Definir DTOs `ReportTotals` y `ActiveAssignmentRow` (campos PII enmascarables) en `<be>/reports/dto/`
- [X] T005 [P] Reutilizar el **enmascaramiento de PII** de **003** (`OfficerMapper`/reglas por rol) para construir las filas de asignaciones activas en `<be>/reports/service/`
- [X] T006 Configurar **autorización por rol** para `/reports/**` (PII y export solo a roles autorizados; rechazo por defecto)
- [X] T007 [P] Reusar el escritor de **auditoría** (012) para `EXPORT_REPORT` (quién, qué reporte/filtros, cuándo; sin PII); si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo.

---

## Phase 3: User Story 1 - Ver totales y asignaciones activas (Priority: P1) 🎯 MVP

**Goal**: totales por estado + tabla paginada de asignaciones activas con filtros, respetando el
enmascaramiento de PII por rol.

**Independent Test**: con datos cargados, los totales coinciden con el inventario/010; filtrar por
municipio → solo esas asignaciones, paginadas; rol sin permiso ve PII enmascarada.

### Tests for User Story 1

- [X] T008 [P] [US1] Test de contrato `GET /reports/totals` y `GET /reports/active-assignments` (paginación + filtros QR/nombre/RFC/placa/CURP/municipio) en `<bet>/reports/ReportQueryContractTest.java`
- [X] T009 [P] [US1] Test: totales **coinciden** con los listados/010 (SC-001); enmascaramiento por rol en las filas (FR-006) en `<bet>/reports/ReportConsistencyTest.java`

### Implementation for User Story 1

- [X] T010 [US1] Implementar `ReportService` (totales por estado con agregados; asignaciones activas con join `assignment`↔`equipment`↔`officers`, enmascaradas por rol) en `<be>/reports/service/`
- [X] T011 [US1] Implementar `GET /reports/totals` y `GET /reports/active-assignments` en `ReportController` en `<be>/reports/controller/`
- [X] T012 [P] [US1] Frontend: `reports.service.ts` (totales + asignaciones activas con filtros) en `<fe>/reportes/data/`
- [X] T013 [US1] Frontend: página de reportes (tarjetas de totales + tabla paginada + panel de filtros) en `<fe>/reportes/pages/reportes/`

**Checkpoint**: vista de control consolidada operativa.

---

## Phase 4: User Story 2 - Exportar a Excel (Priority: P1)

**Goal**: exportar el reporte vigente (con filtros) a Excel en streaming, respetando el enmascaramiento de
PII y **auditando** la exportación.

**Independent Test**: aplicar filtro y exportar → Excel con exactamente las filas del filtro; rol sin PII
→ campos sensibles enmascarados; cada export genera 1 evento de auditoría.

### Tests for User Story 2

- [X] T014 [P] [US2] Test: el Excel contiene **exactamente** las filas del filtro y respeta el enmascaramiento por rol (FR-003/FR-006) en `<bet>/reports/ExcelExportTest.java`
- [X] T015 [P] [US2] Test de **auditoría de exportación** (FR-005/SC-003): cada export escribe `EXPORT_REPORT` sin PII en `<bet>/reports/ExportAuditTest.java`
- [X] T016 [P] [US2] Test de **volumen** (SC-002): exportar 10.000 filas en streaming sin agotar memoria en `<bet>/reports/ExportVolumeTest.java`

### Implementation for User Story 2

- [X] T017 [US2] Implementar `ExcelExportService` en **streaming** (SXSSF/CSV según T003), reutilizando el enmascaramiento de 003 en `<be>/reports/service/`
- [X] T018 [US2] Implementar `GET /reports/active-assignments/export` (descarga; audita `EXPORT_REPORT`) en `<be>/reports/controller/`
- [X] T019 [US2] Frontend: botón "Exportar a Excel" que descarga el reporte con los filtros vigentes en `<fe>/reportes/pages/reportes/`

**Checkpoint**: export operativo, auditado y seguro respecto a PII.

---

## Phase 5: User Story 3 - Reportes operativos y de control (Priority: P2)

**Goal**: reportes predefinidos (inventario general/por unidad/región, asignados/disponibles/
mantenimiento, próximos a vencer, caducados, historial de movimientos, auditoría).

**Independent Test**: seleccionar un tipo de reporte → datos correctos paginados y exportables.

### Tests for User Story 3

- [X] T020 [P] [US3] Test de contrato `GET /reports/{tipo}` (datos correctos por tipo; paginado; exportable) en `<bet>/reports/PredefinedReportsTest.java`

### Implementation for User Story 3

- [X] T021 [US3] Implementar los reportes predefinidos (reutilizan agregados/consultas de 001/003/004/008/009) en `<be>/reports/service/`
- [X] T022 [US3] Implementar `GET /reports/{tipo}` y su export en `<be>/reports/controller/`
- [X] T023 [US3] Frontend: selector de reporte predefinido + tabla/descarga en `<fe>/reportes/pages/reportes/`

**Checkpoint**: las tres historias funcionan.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T024 [P] Advertir en la UI/metadatos que el Excel **hereda la sensibilidad** de los datos exportados
- [X] T025 [P] Endurecimiento: sin PII en logs ni en nombres de archivo; errores que no filtran detalles en `<be>/reports/`
- [X] T026 Validar el quickstart (totales = 010, filtros, export enmascarado + auditado, volumen) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (P1, MVP) → US2 (P1) → US3 (P2) → Polish.**
- Depende de **001** (estados/totales), **003** (PII + enmascaramiento), **004** (asignaciones activas),
  **008**/**009** (incidencias/bajas para reportes), **010** (consistencia de totales) y **012** (auditoría
  de export). Reutiliza el enmascaramiento de 003 (no lo reimplementa).

### Parallel Opportunities

- Setup: T002, T003 en paralelo con T001. Foundational: T004, T005, T007 en paralelo.
- US1: T008/T009 en paralelo, T012 con backend. US2: tests T014–T016 en paralelo, T019 con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- **PII primero**: enmascaramiento por rol en pantalla y Excel; export auditado sin PII en el log.
- El export es un **evento sensible**: streaming para volumen + auditoría siempre.
- La librería de Excel se **registra** (Principio VI). Commit por tarea o grupo lógico; TDD.
