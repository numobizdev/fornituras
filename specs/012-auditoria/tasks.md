---
description: "Task list — Bitácora de auditoría (012)"
---

# Tasks: Bitácora de auditoría (ISO 27001)

**Input**: Design documents from `specs/012-auditoria/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md)

**Tests**: incluidos. Pruebas de "exactamente 1 registro por operación sensible", **no-PII** en el log,
inmutabilidad (UPDATE/DELETE rechazado) y autorización de la consulta.

> **Habilitador transversal:** entregar pronto el puerto `AuditWriter` para que el resto de features lo
> consuman. Las features que se implementen antes que 012 usan un escritor mínimo y migran a este.

**Organization**: por user story; US2 (registro automático) entrega el puerto que todas usan.

## Path Conventions

- **Backend**: `<be>/audit/` = `fornituras-api/src/main/java/com/numobiz/solutions/fornituras/modules/audit/`;
  migraciones en `fornituras-api/src/main/resources/db/migration/`; pruebas en `<bet>/audit/`.
- **Frontend**: `<fe>/auditoria/` = `sigefor/src/app/features/auditoria/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Crear la estructura de paquetes del módulo `audit` (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `aspect/`) en `<be>/audit/`
- [X] T002 [P] Preparar la feature frontend `<fe>/auditoria/` (`pages/auditoria/`, `data/`)
- [X] T003 [P] Abrir el **ADR de inmutabilidad y retención** (append-only vs hashing encadenado; política de retención legal/ISO 27001) en `docs/04-decisiones/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase.

- [X] T004 [P] Crear la entidad `AuditLog` (usuario_id, accion, entidad, entidad_id, timestamp, ip, evidencia/diff JSON, prev_hash NULL) en `<be>/audit/entity/AuditLog.java`
- [X] T005 Crear la migración Flyway `V{n}__create_audit_log.sql` (append-only; índices por usuario/acción/(entidad,entidad_id)/timestamp; revocar UPDATE/DELETE al rol de la app) — usar el siguiente número Flyway libre
- [X] T006 [P] Definir DTOs `AuditEvent` (escritura interna) y `AuditLogSummary` (lectura) en `<be>/audit/dto/`
- [X] T007 [P] Implementar la **redacción de PII/secretos** central (enmascara valores sensibles, referencia entidades por id) en `<be>/audit/service/`

**Checkpoint**: esquema y utilidades listos.

---

## Phase 3: User Story 2 - Registro automático de eventos sensibles (Priority: P1) 🎯 MVP

**Goal**: registrar automáticamente cada evento sensible (incl. intentos denegados) con todos los
campos, **sin PII**, vía un puerto `AuditWriter` reutilizable.

**Independent Test**: ejecutar cada operación sensible → exactamente 1 registro con actor/acción/
entidad/id/timestamp/ip; una operación denegada → también registrada; ningún registro contiene PII.

### Tests for User Story 2

- [X] T008 [P] [US2] Test de integración: una operación sensible genera **exactamente 1** registro con los campos requeridos en `<bet>/audit/AuditCaptureTest.java`
- [X] T009 [P] [US2] Test de **no-PII** (SC-002): ningún campo del log contiene PII/secretos en claro (redacción aplicada) en `<bet>/audit/AuditNoPiiTest.java`
- [X] T010 [P] [US2] Test de eventos **denegados** (FR-006): un acceso rechazado por autorización se audita en `<bet>/audit/AuditDeniedTest.java`

### Implementation for User Story 2

- [X] T011 [US2] Implementar `AuditWriter` (puerto) + implementación que persiste `AuditEvent` redactado en `audit_log` en `<be>/audit/service/`
- [X] T012 [US2] Implementar `AuditAspect` (AOP sobre servicios sensibles vía `@Auditable`/pointcuts) y listeners de Spring Security (login/logout/denegado) evitando doble registro en `<be>/audit/aspect/`
- [X] T013 [US2] Implementar `AuditLogRepository` (append-only; sin métodos de update/delete) en `<be>/audit/repository/`
- [X] T014 [P] [US2] Test de **inmutabilidad** (SC-003): intento de UPDATE/DELETE sobre `audit_log` rechazado en `<bet>/audit/AuditImmutabilityTest.java`

**Checkpoint**: `AuditWriter` disponible para que todas las features lo consuman (MVP del Principio V).

---

## Phase 4: User Story 1 - Consultar la bitácora (Priority: P1)

**Goal**: consulta paginada con filtros (usuario, fecha, acción, entidad), restringida a auditor/admin.

**Independent Test**: tras una asignación, la bitácora muestra el evento con actor/acción/entidad/fecha/
IP, sin PII; los filtros acotan correctamente.

### Tests for User Story 1

- [X] T015 [P] [US1] Test de contrato `GET /audit` (paginación + filtros usuario/fecha/acción/entidad) en `<bet>/audit/AuditQueryContractTest.java`
- [X] T016 [P] [US1] Test de autorización: solo auditor/admin consultan; rol operativo → denegado (y auditado) en `<bet>/audit/AuditQueryAuthTest.java`

### Implementation for User Story 1

- [X] T017 [US1] Implementar `AuditQueryService` + consulta paginada con filtros e índices en `<be>/audit/service/`
- [X] T018 [US1] Implementar `GET /audit` en `AuditLogController` (restringido por rol) en `<be>/audit/controller/`
- [X] T019 [P] [US1] Frontend: `audit.service.ts` (consulta con filtros) en `<fe>/auditoria/data/`
- [X] T020 [US1] Frontend: página de consulta (tabla paginada + filtros usuario/fecha/acción/entidad) en `<fe>/auditoria/pages/auditoria/`

**Checkpoint**: bitácora consultable por roles autorizados.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T021 [P] *(condicional al ADR)* Implementar **hashing encadenado** (`prev_hash`) como refuerzo de detección de manipulación en `<be>/audit/service/`
- [X] T022 [P] Documentar y, si aplica, automatizar la **política de retención** (purga/archivado) conforme al ADR
- [X] T023 Migrar a `AuditWriter` cualquier escritor mínimo que features previas (001/004/etc.) hayan creado provisionalmente
- [X] T024 Validar el quickstart (1 registro por operación, no-PII, inmutabilidad, denegados) y registrar resultados

---

## Dependencies & Execution Order

- **Setup → Foundational (BLOQUEA) → US2 (puerto, MVP) → US1 (consulta) → Polish.**
- US2 antes que US1: primero generar eventos, luego consultarlos.
- El ADR (T003) condiciona T021/T022; no bloquea el MVP append-only.

### Parallel Opportunities

- Setup: T002, T003 en paralelo con T001.
- Foundational: T004, T006, T007 en paralelo; T005 tras T004.
- US2: tests T008–T010, T014 en paralelo; US1: T015/T016 en paralelo, T019 con backend.

---

## Notes

- [P] = archivos distintos, sin dependencias.
- La auditoría **no** puede ser superficie de fuga: redacta PII, referencia por id, append-only.
- Es habilitador transversal: prioriza el puerto `AuditWriter`.
- Commit por tarea o grupo lógico; TDD (tests en rojo antes de implementar).
