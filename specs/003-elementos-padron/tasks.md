---
description: "Task list — Padrón de elementos policiales (003)"
---

# Tasks: Padrón de elementos policiales

**Input**: Design documents from `specs/003-elementos-padron/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/officers-api.md](./contracts/officers-api.md)

**Tests**: incluidos. El plan define pruebas (JUnit 5 + Testcontainers, contrato, autorización)
y el proyecto es de **alta sensibilidad** (PII): las pruebas de enmascaramiento/auditoría/cifrado
son parte del entregable, no opcionales.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/officers/`
  (abreviado `<be>/officers/`); migraciones en `fornituras-api/src/main/resources/db/migration/`;
  pruebas en `fornituras-api/src/test/java/com/numobiz/solutions/fornituras/modules/officers/`
  (abreviado `<bet>/officers/`).
- **Frontend**: `sigefor/src/app/features/elementos/` (abreviado `<fe>/elementos/`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: estructura del módulo y configuración de cifrado.

- [X] T001 Crear la estructura de paquetes del módulo `officers` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/officers/`
- [~] T002 [P] Configurar la conexión Always Encrypted (`columnEncryptionSetting=Enabled`) en `fornituras-api/src/main/resources/application-dev.yml` y documentar los nombres de variables (`SQLSERVER_AE_ENABLED`, `AE_CMK_PROVIDER`, `OFFICER_BLIND_INDEX_KEY`, `PHOTO_STORAGE_TARGET`) en `.env.example`
- [X] T003 [P] Preparar la carpeta de la feature frontend en `<fe>/elementos/` (la página `pages/elementos/` ya existe; crear `pages/elemento-form/` y `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: entidades, migración, cifrado y reglas transversales que todas las historias usan.

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T004 [P] Crear entidades de catálogo `Sexo`, `TipoSangre`, `Municipio` en `<be>/officers/entity/`
- [X] T005 Crear la entidad `Officer` (placa única; `nombre`/apellidos con Always Encrypted; `curp`/`rfc` + columnas blind index `curp_idx`/`rfc_idx`; FKs de catálogo; `foto_url`; `status`) en `<be>/officers/entity/Officer.java` (ver [data-model.md](./data-model.md))
- [X] T006 Crear la migración Flyway `V{n}__create_officers_and_catalogs.sql` (tablas `officers`, `sexo`, `tipo_sangre`, `municipio`; columnas Always Encrypted; `UNIQUE(placa)`; índices `*_idx`, `municipio_id`, `sexo_id`, `status`) en `fornituras-api/src/main/resources/db/migration/`
- [X] T007 [P] Sembrar catálogos (`sexo`, `tipo_sangre` O±/A±/B±/AB±, `municipio` del estado del cliente) en la migración o un seeder
- [X] T008 [P] Implementar utilidad de **normalización** (trim/upper/sin espacios) para `placa`, `curp`, `rfc` en `<be>/officers/service/`
- [X] T009 [P] Implementar el helper de **blind index** `HMAC(OFFICER_BLIND_INDEX_KEY, normalize(valor))` para CURP/RFC en `<be>/officers/service/` (clave desde entorno; nunca en repo — Principio III; ADR 0004)
- [X] T010 [P] Definir DTOs `OfficerCreateRequest`, `OfficerSummary`, `OfficerDetail` en `<be>/officers/dto/`
- [X] T011 Implementar `OfficerMapper` (entity↔dto) con **reglas de enmascaramiento por rol** en `<be>/officers/mapper/` (CURP/RFC/foto solo a rol autorizado)
- [X] T012 Configurar **autorización por rol** para `/officers/**` (extender la config de Spring Security existente; rechazo por defecto)
- [X] T013 [P] Proveer un escritor de **auditoría** (`VIEW_OFFICER`/`CREATE_OFFICER`/`UPDATE_OFFICER`, sin PII) reutilizando el mecanismo de la feature 012; si aún no existe, crear un escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo — las user stories pueden empezar.

---

## Phase 3: User Story 1 - Consultar el padrón (Priority: P1) 🎯 MVP

**Goal**: listar elementos con paginación, búsqueda por texto (nombre/CURP/RFC/placa), filtros
(municipio, sexo), ver ficha con enmascaramiento por rol y acceso auditado.

**Independent Test**: con datos cargados, buscar por placa devuelve el correcto; filtrar por
municipio devuelve solo esos, paginado; `CAPTURISTA` ve CURP/RFC enmascarados; `GET /officers/{id}`
genera registro de auditoría.

### Tests for User Story 1

- [~] T014 [P] [US1] Test de contrato `GET /officers` (paginación + `q` + `municipioId`/`sexoId`) en `<bet>/officers/OfficerListContractTest.java`
- [~] T015 [P] [US1] Test de integración (Testcontainers MSSQL): búsqueda por placa, filtro por municipio, paginación en `<bet>/officers/OfficerListIntegrationTest.java`
- [~] T016 [P] [US1] Test de autorización: `CAPTURISTA` recibe CURP/RFC enmascarados; `ADMIN` completo en `<bet>/officers/OfficerMaskingTest.java`
- [~] T017 [P] [US1] Test de auditoría: `GET /officers/{id}` escribe `VIEW_OFFICER` sin PII en `<bet>/officers/OfficerAuditTest.java`

### Implementation for User Story 1

- [X] T018 [US1] Implementar `OfficerRepository` con consulta paginada + filtros (municipio, sexo) y búsqueda por blind index/placa en `<be>/officers/repository/OfficerRepository.java`
- [X] T019 [US1] Implementar la **estrategia de búsqueda** en `OfficerService` (detectar CURP/RFC → blind index; placa → igualdad; nombre/apellidos → `LIKE` confidencial por enclave; fallback ADR 0004) en `<be>/officers/service/OfficerService.java`
- [X] T020 [US1] Implementar `GET /officers` (Pageable, `q`, `municipioId`, `sexoId`) devolviendo `OfficerSummary` en `<be>/officers/controller/OfficerController.java`
- [X] T021 [US1] Implementar `GET /officers/{id}` (ficha `OfficerDetail` enmascarada por rol) con **side effect de auditoría** `VIEW_OFFICER` en `<be>/officers/controller/OfficerController.java`
- [X] T022 [US1] Garantizar **cero PII en logs/URLs**: omitir/hashear `q` si pudo contener PII en `<be>/officers/service/`
- [X] T023 [P] [US1] Frontend: `officers.service.ts` (GET listado con params + GET ficha) en `<fe>/elementos/data/officers.service.ts`
- [X] T024 [US1] Frontend: página de listado — panel de filtros (texto, municipio, sexo), tabla paginada (foto miniatura, nombre, placa, municipio, tipo de sangre, acciones) en `<fe>/elementos/pages/elementos/`

**Checkpoint**: US1 funcional y testeable de forma independiente.

---

## Phase 4: User Story 2 - Registrar un nuevo elemento (Priority: P1)

**Goal**: alta de elemento con validación en el borde, unicidad de placa, blind index calculado,
foto y auditoría.

**Independent Test**: alta con placa nueva → aparece en el padrón; alta con placa repetida → 409;
formato CURP/RFC inválido → rechazo.

### Tests for User Story 2

- [~] T025 [P] [US2] Test de contrato `POST /officers` (validación, 409 por duplicado) en `<bet>/officers/OfficerCreateContractTest.java`
- [~] T026 [P] [US2] Test de integración: alta + unicidad de placa + blind index calculado + `CREATE_OFFICER` auditado en `<bet>/officers/OfficerCreateIntegrationTest.java`

### Implementation for User Story 2

- [X] T027 [US2] Añadir **Bean Validation** a `OfficerCreateRequest` (placa requerida; nombre/apellido paterno requeridos; CURP 18, RFC 12–13; catálogos válidos) en `<be>/officers/dto/`
- [X] T028 [US2] Implementar `POST /officers` en controller + service (normalizar, calcular blind index, persistir, auditar `CREATE_OFFICER`) en `<be>/officers/`
- [X] T029 [US2] Manejar duplicados (placa/curp/rfc normalizados) → **409** con mensaje claro en `<be>/officers/service/`
- [~] T030 [US2] Implementar `POST /officers/{id}/foto` y `GET /officers/{id}/foto` (storage cifrado, validación de tipo/tamaño, descarga auditada y por rol) en `<be>/officers/controller/` — **gated por ADR 0003** mientras la foto esté restringida
- [X] T031 [P] [US2] Frontend: `officers.service.ts` `create()` (+ subida de foto multipart) en `<fe>/elementos/data/officers.service.ts`
- [X] T032 [US2] Frontend: página `elemento-form` (nombre, apellidos, sexo, tipo de sangre, municipio, placa, CURP/RFC gated, sección de foto) en `<fe>/elementos/pages/elemento-form/`

**Checkpoint**: US1 y US2 funcionan de forma independiente; ya se puede poblar y consultar el padrón (MVP).

---

## Phase 5: User Story 3 - Reporte del padrón (Priority: P3)

**Goal**: generar reporte del padrón con los filtros aplicados, respetando el enmascaramiento de PII.

**Independent Test**: aplicar filtro por municipio y generar el reporte → contiene solo esos
elementos y respeta el enmascaramiento por rol.

### Tests for User Story 3

- [~] T033 [P] [US3] Test de contrato/integración del reporte (filtros + enmascaramiento + auditoría de generación) en `<bet>/officers/OfficerReportTest.java`

### Implementation for User Story 3

- [~] T034 [US3] Implementar endpoint de reporte/exportación del padrón (reutiliza filtros de US1, aplica enmascaramiento por rol, **audita** la generación) en `<be>/officers/controller/`
- [~] T035 [US3] Frontend: botón "Reporte" en el listado que dispara la generación/descarga en `<fe>/elementos/pages/elementos/`

**Checkpoint**: las tres historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: mejoras transversales y endurecimiento.

- [~] T036 Implementar `PUT /officers/{id}` (edición de atributos no identitarios; placa/CURP/RFC restringidos y auditados `UPDATE_OFFICER`) en `<be>/officers/controller/`
- [~] T037 [P] Validar/condicionar la estrategia de **secure enclaves** (ADR 0004) y documentar el fallback si el SQL Server del cliente no los soporta
- [~] T038 [P] Endurecimiento: rate limiting en endpoints de búsqueda, cabeceras de seguridad, manejo de errores que no filtre PII
- [X] T039 [P] Tests unitarios de normalización y blind index en `<bet>/officers/`
- [~] T040 Ejecutar la validación de [quickstart.md](./quickstart.md) (los 6 escenarios) y registrar resultados
- [~] T041 [P] Actualizar `docs/03-modelo-datos.md` si el esquema final difiere y confirmar ADR 0003/0004

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA** todas las user stories.
- **User Stories (Phase 3–5)**: dependen de Foundational. US1 y US2 (ambas P1) son el MVP; US3 (P3) después.
- **Polish (Phase 6)**: depende de las historias deseadas completas.

### User Story Dependencies

- **US1 (P1)**: tras Foundational. Independiente (la búsqueda usa datos sembrados o creados por US2).
- **US2 (P1)**: tras Foundational. Independiente; al integrarse con US1, poblar el padrón hace la demo end-to-end.
- **US3 (P3)**: tras Foundational; reutiliza filtros de US1 pero es testeable por separado.

### Within Each User Story

- Los tests se escriben y **fallan** antes de implementar (TDD).
- Modelos → servicios → endpoints → frontend.

### Parallel Opportunities

- Setup: T002, T003 en paralelo.
- Foundational: T004, T007, T008, T009, T010, T013 en paralelo (distintos archivos); T005→T006 secuenciales (entidad antes de migración); T011/T012 tras DTOs/entidad.
- US1: tests T014–T017 en paralelo; T023 (servicio frontend) en paralelo con backend.
- US2: tests T025–T026 en paralelo; T031 en paralelo con backend.

---

## Parallel Example: User Story 1

```bash
# Tests de US1 juntos (deben fallar primero):
Task: "Contract test GET /officers en <bet>/officers/OfficerListContractTest.java"
Task: "Integration test búsqueda/filtros en <bet>/officers/OfficerListIntegrationTest.java"
Task: "Authorization test enmascaramiento en <bet>/officers/OfficerMaskingTest.java"
Task: "Audit test VIEW_OFFICER en <bet>/officers/OfficerAuditTest.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2, ambas P1)

1. Completar Phase 1 (Setup) y Phase 2 (Foundational — crítico).
2. Completar US2 (alta) y US1 (consulta) — juntas dan el ciclo poblar→consultar.
3. **PARAR y VALIDAR**: probar alta + búsqueda + enmascaramiento + auditoría (quickstart).
4. Demo del MVP.

### Incremental Delivery

1. Setup + Foundational → fundamento listo.
2. US2 + US1 → MVP (padrón operativo, PII protegida).
3. US3 → reporte/exportación.
4. Polish → edición, endurecimiento, validación de enclaves.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Seguridad primero: ninguna tarea expone PII en logs/URLs/QR; el enmascaramiento y la auditoría
  se prueban explícitamente.
- CURP/RFC/foto están **gated por ADR 0003** (Propuesto); búsqueda sobre cifrado por **ADR 0004**.
- Commit por tarea o grupo lógico; validar tests en rojo antes de implementar.

### Decisión de cifrado y tareas diferidas (`[~]`)

- **Cifrado de PII a nivel de aplicación (ADR 0006, interino):** como Always Encrypted/enclaves no
  están disponibles, la PII (nombre/apellidos/CURP/RFC) se cifra con **AES-GCM** vía
  `EncryptedStringConverter` y la igualdad de CURP/RFC usa **blind index HMAC**; la `placa` va en
  claro, única y normalizada. El enmascaramiento por rol y la auditoría (`VIEW_OFFICER`,
  `CREATE_OFFICER`) están implementados.
- **T002 (Always Encrypted):** sustituida por la config de cifrado a nivel app (vars
  `PII_ENCRYPTION_KEY`/`OFFICER_BLIND_INDEX_KEY` en `.env.example`). La migración a AE queda para
  cuando exista la infraestructura (ADR 0006 §reversión).
- **T014–T017, T025–T026, T033 (tests de contrato/integración/masking/auditoría con
  Testcontainers):** diferidos por falta de infraestructura Testcontainers; cubierto a nivel
  unitario (`OfficerServiceTest`, `PiiCipherTest`, `BlindIndexerTest`, `PiiMaskerTest`, 13 tests).
- **T030 (foto):** el storage cifrado de la foto queda fuera de alcance hasta resolver ADR 0003.
- **T034/T035 (reporte US3), T036 (PUT edición):** no incluidos en este MVP (US1+US2). La ficha es
  de solo lectura en el frontend.
- **T037 (enclaves), T038 (rate limiting), T040 (quickstart), T041 (docs):** pendientes de
  infraestructura/entorno con BD.
- **Búsqueda por nombre parcial:** diferida (cifrado no determinista, ADR 0006). La búsqueda cubre
  placa (parcial) y CURP/RFC (exacta vía blind index).

### Desviación de roles

- Roles reales del sistema: **ADMIN** y **CAPTURISTA**. Lectura del padrón para autenticados (con
  enmascaramiento); solo **ADMIN** ve CURP/RFC en claro; alta para ADMIN/CAPTURISTA. El RBAC fino
  es de la feature 013.
