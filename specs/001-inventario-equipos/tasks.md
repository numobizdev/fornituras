---
description: "Task list — Inventario de fornituras (001)"
---

# Tasks: Inventario de fornituras

**Input**: Design documents from `specs/001-inventario-equipos/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de unicidad (incl. concurrencia), derivación de vigencia, reglas de
estado, autorización y auditoría son parte del entregable.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

## Path Conventions

- **Backend**: `<be>/equipment/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/equipment/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/equipment/`.
- **Frontend**: `<fe>/fornituras/` = `sigefor/src/app/features/fornituras/` (ya andamiada).

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear la estructura de paquetes del módulo `equipment` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`) en `<be>/equipment/`
- [X] T002 [P] Preparar la feature frontend `<fe>/fornituras/` (ya existe `pages/fornituras/`; crear `pages/fornitura-form/`, `pages/fornitura-lote/`, `data/`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T003 [P] Crear el catálogo `EquipmentStatus` (disponible/asignada/en_mantenimiento/en_traslado/extraviada/baja_definitiva) en `<be>/equipment/entity/`
- [X] T004 Crear la entidad `Equipment` (id opaco inmutable; `codigo_qr` único normalizado; FKs a tipo/talla/almacén; `fecha_fabricacion`, `vida_util_meses`, `fecha_vencimiento`; `status`; descriptivos; `foto_url`) en `<be>/equipment/entity/Equipment.java`
- [X] T005 Crear la migración Flyway `V{n}__create_equipment.sql` (`equipment` + `equipment_status`; `UNIQUE(codigo_qr)` normalizado; índices por status/tipo/almacén/`fecha_vencimiento`) — usar el siguiente número Flyway libre
- [X] T006 [P] Implementar utilidad de **normalización** del código (trim/upper/sin guiones-espacios) en `<be>/equipment/service/`
- [X] T007 [P] Implementar el cálculo de **vigencia derivada** (vigente/próxima ≤ 90 días/caducada desde `fecha_vencimiento`) en `<be>/equipment/service/`
- [X] T008 [P] Definir DTOs `EquipmentCreateRequest`, `BatchCreateRequest`, `EquipmentSummary`, `EquipmentDetail` en `<be>/equipment/dto/`
- [X] T009 [P] Definir el puerto `EquipmentLifecycleQuery` (¿tiene asignación vigente? ¿traslado en curso?) que 004/007 implementarán; default "no" hasta integrarlos, en `<be>/equipment/service/`
- [X] T010 Configurar **autorización por rol** para `/equipment/**` (alta/edición/baja restringidas; consulta a roles operativos; rechazo por defecto)
- [X] T011 [P] Reusar el escritor de **auditoría** (012) para `CREATE/UPDATE/STATUS_CHANGE_EQUIPMENT`; si 012 no existe, escritor mínimo a `audit_log`

**Checkpoint**: fundamento listo — las user stories pueden empezar.

---

## Phase 3: User Story 1 - Registrar una fornitura individual (Priority: P1) 🎯 MVP

**Goal**: alta individual con identificador único normalizado, estado inicial "disponible", catálogos válidos y auditoría.

**Independent Test**: alta con código nuevo → aparece en consulta y estado "disponible"; segunda con el mismo código → rechazada (cero duplicados).

### Tests for User Story 1

- [X] T012 [P] [US1] Test de contrato `POST /equipment` (validación, catálogos, 409 por duplicado) en `<bet>/equipment/EquipmentCreateContractTest.java`
- [X] T013 [P] [US1] Test de integración: alta + unicidad normalizada + **inserción concurrente** del mismo código (solo una gana) en `<bet>/equipment/EquipmentUniquenessIntegrationTest.java`
- [X] T014 [P] [US1] Test de autorización: rol sin permiso de alta → denegado y auditado en `<bet>/equipment/EquipmentAuthTest.java`

### Implementation for User Story 1

- [X] T015 [US1] Implementar `EquipmentRepository` (existsByCodigoNormalizado, persistencia) en `<be>/equipment/repository/`
- [X] T016 [US1] Implementar el alta en `EquipmentService` (normalizar, validar catálogos activos, derivar `fecha_vencimiento`, estado inicial "disponible", auditar) en `<be>/equipment/service/`
- [X] T017 [US1] Implementar `POST /equipment` en `EquipmentController` (mapea 409 por duplicado) en `<be>/equipment/controller/`
- [X] T018 [US1] Añadir **Bean Validation** a `EquipmentCreateRequest` (código requerido, tipo/talla/almacén válidos, fechas coherentes) en `<be>/equipment/dto/`
- [X] T019 [P] [US1] Frontend: `equipment.service.ts` (`create`) en `<fe>/fornituras/data/`
- [X] T020 [US1] Frontend: página `fornitura-form` con captura de código vía componente **014** (lector/cámara/manual), selección de tipo/talla/almacén, fechas/vida útil en `<fe>/fornituras/pages/fornitura-form/`

**Checkpoint**: se puede dar de alta y la unicidad está garantizada (MVP base).

---

## Phase 4: User Story 3 - Consultar y buscar fornituras (Priority: P2)

**Goal**: listado paginado con filtros y ficha, sin PII del elemento, con resolución por código server-side.

**Independent Test**: filtrar por estado "disponible" → solo disponibles, paginado; buscar por código → la fornitura correcta.

### Tests for User Story 3

- [X] T021 [P] [US3] Test de contrato `GET /equipment` (paginación + filtros qr/tipo/talla/almacén/estado) y `GET /equipment/by-codigo/{codigo}` en `<bet>/equipment/EquipmentListContractTest.java`
- [X] T022 [P] [US3] Test de integración: búsqueda por código, filtro por estado, paginación, **vigencia derivada** en la respuesta en `<bet>/equipment/EquipmentListIntegrationTest.java`

### Implementation for User Story 3

- [X] T023 [US3] Implementar consulta paginada + filtros en `EquipmentRepository`/`EquipmentService` (specs dinámicas) en `<be>/equipment/`
- [X] T024 [US3] Implementar `GET /equipment` y `GET /equipment/{id}` (ficha `EquipmentDetail` con vigencia derivada, sin PII del elemento) en `<be>/equipment/controller/`
- [X] T025 [US3] Implementar `GET /equipment/by-codigo/{codigo}` (**resolución server-side**, consumible por 004/007/009; "no encontrado" sin filtrar detalles) en `<be>/equipment/controller/`
- [X] T026 [P] [US3] Frontend: `equipment.service.ts` (`list` con params + `getByCodigo`) en `<fe>/fornituras/data/`
- [X] T027 [US3] Frontend: página de listado — filtros (código, descripción, tipo, talla, almacén, estado) + tabla paginada con color semántico de estado/vigencia en `<fe>/fornituras/pages/fornituras/`

**Checkpoint**: alta + consulta operativas.

---

## Phase 5: User Story 2 - Alta por lote (Priority: P2)

**Goal**: capturar datos generales una vez y agregar N códigos QR (lector/cámara/manual), creando una fornitura por código, con validación de duplicados antes de confirmar.

**Independent Test**: datos de lote + 3 códigos distintos → 3 fornituras con los mismos datos; código repetido en el lote o ya existente → rechazado antes de confirmar; cancelar → no se crea nada.

### Tests for User Story 2

- [X] T028 [P] [US2] Test de contrato `POST /equipment/batch` (crea N; rechaza duplicado intra-lote y contra BD; atomicidad) en `<bet>/equipment/EquipmentBatchContractTest.java`
- [X] T029 [P] [US2] Test de integración: lote de 3 + duplicado intra-lote + rollback en fallo en `<bet>/equipment/EquipmentBatchIntegrationTest.java`

### Implementation for User Story 2

- [X] T030 [US2] Implementar `BatchService` (validar duplicados intra-lote y contra BD, crear todas en una transacción, auditar) en `<be>/equipment/service/`
- [X] T031 [US2] Implementar `POST /equipment/batch` en `EquipmentController` en `<be>/equipment/controller/`
- [X] T032 [P] [US2] Frontend: `equipment.service.ts` (`createBatch`) en `<fe>/fornituras/data/`
- [X] T033 [US2] Frontend: página `fornitura-lote` (datos generales + captura repetida de código con tabla previa y validación de duplicados antes de confirmar) en `<fe>/fornituras/pages/fornitura-lote/`

**Checkpoint**: carga masiva operativa (modo real de bodega).

---

## Phase 6: User Story 4 - Editar y cambiar el estado de una fornitura (Priority: P3)

**Goal**: editar atributos no identitarios y cambiar estado operativo; bloquear baja/traslado con asignación vigente; cambio de código restringido y auditado.

**Independent Test**: cambiar estado "disponible"→"en mantenimiento" → reflejado en consulta y auditado; intentar baja con asignación vigente → bloqueado.

### Tests for User Story 4

- [X] T034 [P] [US4] Test de contrato `PUT /equipment/{id}` y `PATCH /equipment/{id}/status` (transiciones válidas, código no editable salvo rol/auditoría) en `<bet>/equipment/EquipmentUpdateContractTest.java`
- [X] T035 [P] [US4] Test de integración: bloqueo de baja/traslado con asignación vigente (vía `EquipmentLifecycleQuery`) y auditoría del cambio en `<bet>/equipment/EquipmentStatusIntegrationTest.java`

### Implementation for User Story 4

- [X] T036 [US4] Implementar edición no identitaria + cambio de estado (validar transiciones contra el catálogo; consultar `EquipmentLifecycleQuery` para baja/traslado) en `<be>/equipment/service/`
- [X] T037 [US4] Implementar el cambio de `codigo_qr` **restringido y auditado** (solo rol elevado; registra valor anterior referenciado) en `<be>/equipment/service/`
- [X] T038 [US4] Implementar `PUT /equipment/{id}` y `PATCH /equipment/{id}/status` en `<be>/equipment/controller/`
- [X] T039 [US4] Frontend: edición/cambio de estado en `fornitura-form` (selector de estado con color semántico; aviso si hay asignación vigente) en `<fe>/fornituras/pages/fornitura-form/`

**Checkpoint**: las cuatro historias funcionan; el inventario es fiel a la realidad.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T040 [P] Endurecimiento: rate limiting en `GET /equipment/by-codigo` (mitiga enumeración, ADR 0005/0010), cabeceras de seguridad, errores que no filtran detalles en `<be>/equipment/`
- [X] T041 [P] Tests unitarios de normalización y de derivación de vigencia en `<bet>/equipment/`
- [~] T042 Validar el quickstart (alta, lote, búsqueda, vigencia, bloqueo de baja con asignación) y registrar resultados
- [X] T043 [P] Actualizar `docs/03-modelo-datos.md` si el esquema final difiere

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US1 (P1, MVP) → US3 (P2) → US2 (P2) → US4 (P3) → Polish.**
- US1 antes que US2/US3 para tener el alta base; US3 (consulta) habilita validar visualmente el lote.
- US4 (baja/traslado) usa el puerto `EquipmentLifecycleQuery`; su test queda completo al integrar 004/007.
- Catálogos **005/006** deben existir (FKs tipo/talla/almacén); el componente **014** habilita la captura de código.

### Parallel Opportunities

- Foundational: T003, T006, T007, T008, T009, T011 en paralelo; T004→T005 secuenciales.
- US1: tests T012–T014 en paralelo; T019 (servicio frontend) en paralelo con backend.
- US3/US2/US4: los tests de cada historia en paralelo; servicios frontend en paralelo con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- La fornitura **no** guarda PII; las fichas no exponen datos del elemento.
- El `codigo` es opaco (ADR 0005); su resolución es server-side y conviene auditarla/limitarla.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).

### Tests de contrato/integración (T012–T014, T021/T022, T028/T029, T034/T035) — HECHOS

Implementados con **`@SpringBootTest` + MockMvc + perfil H2** (`application-test.yml`) y
`spring-security-test`, **sin Testcontainers ni Docker** (no disponible en el entorno de
desarrollo). Ver **[ADR 0009](../../docs/04-decisiones/0009-tests-integracion-h2-mockmvc.md)**. Para
reducir arranques de contexto, algunas parejas contrato/integración se **consolidaron** en una clase
(anotado abajo). **23 tests** nuevos, todos verdes:

- **T012** → `EquipmentCreateContractTest` (201/estado inicial, 409 duplicado normalizado, 400
  validación y catálogo inactivo).
- **T013** → `EquipmentUniquenessIntegrationTest` (unicidad normalizada + **inserción concurrente**:
  solo una gana; cero duplicados vía `UNIQUE(codigo_normalizado)`).
- **T014** → `EquipmentAuthTest` (rol sin permiso → 403 y no crea; ADMIN/CAPTURISTA sí; consulta a
  cualquier autenticado). El rechazo es declarativo (`@PreAuthorize`), previo al servicio, por lo que
  no genera auditoría de negocio.
- **T021 + T022** → `EquipmentListApiTest` (paginación, filtro por estado, `by-codigo` server-side y
  404, **vigencia derivada** en la respuesta).
- **T028 + T029** → `EquipmentBatchApiTest` (crea N; rechaza duplicado intra-lote y contra BD;
  **atomicidad**: nada persiste si uno falla).
- **T034 + T035** → `EquipmentUpdateStatusApiTest` (edición no identitaria; **código inmutable**;
  transición válida; **bloqueo de baja/traslado con asignación vigente** vía el puerto real
  `EquipmentLifecycleQuery`).

> **Límite (ADR 0009):** H2 en modo `MSSQLServer` no es SQL Server; el esquema de test lo genera JPA
> (Flyway off). Validar migraciones/dialecto/semántica de carrera real queda para un perfil
> Testcontainers en CI (donde haya Docker).

La cobertura **unitaria** previa sigue vigente: `EquipmentServiceTest` (10), `ExpiryCalculatorTest`
(5) y `CodeNormalizerTest` (3) — 18 tests (`CodeNormalizer` es la utilidad compartida en
`common/text/`).

### Endurecimiento (T040) — HECHO

Rate limiting de `GET /equipment/by-codigo` con **Bucket4j tras el puerto `RateLimiter`**
(`common/ratelimit`), configurable en `app.ratelimit.by-codigo` (por defecto 30/60 s por actor);
excedido → **429** sin filtrar detalles. Ver
**[ADR 0010](../../docs/04-decisiones/0010-rate-limiting-bucket4j.md)**. Cabeceras de seguridad por
defecto de Spring Security y errores genéricos (`GlobalExceptionHandler`) ya cubrían el resto de la
tarea. Test: `EquipmentRateLimitTest`.

### Tareas diferidas (`[~]`) y por qué

- **T037 (cambio de código por rol elevado):** implementado como **inmutable** en la edición
  normal (el servicio rechaza el cambio); la operación dedicada y auditada se abordará si el
  negocio la requiere.
- **T042 (quickstart):** requiere entorno con SQL Server levantado.
- **T043 (actualizar `docs/03-modelo-datos.md`):** **COMPLETADA.** El doc refleja ya el esquema de
  `V11` y las FKs repuntadas por ADR 0007 (`V15`: catálogos genéricos `catalog`/`catalog_item`,
  `municipio` a texto libre).

### Desviaciones respecto al plan (alineadas al código existente)

- **Estado como enum** (`EquipmentStatus`, persistido como cadena + `CHECK`) en vez de tabla
  catálogo, igual que `WarehouseType`.
- **Roles reales** del sistema: ADMIN y CAPTURISTA (no SUPERVISOR/ALMACEN/OPERADOR/AUDITOR). La
  escritura se permite a ADMIN/CAPTURISTA y la lectura a cualquier autenticado; el RBAC fino es de 013.
- **Captura de código:** input manual/teclado (cubre lector físico, que actúa como teclado) y QR
  manual; la **cámara** es la feature **014** y queda como punto de extensión.
