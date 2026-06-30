# Implementation Plan: Bitácora de auditoría (ISO 27001)

**Branch**: `dev` (feature **012-auditoria**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/012-auditoria/spec.md`

## Summary

Construir la **bitácora de auditoría** transversal que operacionaliza el **Principio V** de la
constitución: registrar automáticamente todo evento sensible (login/logout, acceso a PII, alta/edición/
baja, asignación/devolución, traslados, incidencias, exportaciones, cambios de usuarios/roles) con
actor, acción, entidad, `entidad_id`, timestamp, IP y evidencia/diff — **sin volcar PII ni secretos**
(referencia por id). La bitácora debe ser **de difícil alteración** (append-only) y consultable con
paginación/filtros, restringida a roles de auditoría/administración.

Es una pieza **habilitadora**: el resto de features llama a su escritor. Por eso se prioriza entregar
pronto el **puerto de escritura** (`AuditWriter`) que las demás features consumen, aunque la pantalla
de consulta y el endurecimiento de inmutabilidad lleguen después.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA, AOP para captura
transversal), Flyway (`flyway-sqlserver`), `mssql-jdbc`. Sin dependencias nuevas. Frontend: servicios
HTTP + componentes Ionic.

**Storage**: SQL Server 2022. Tabla `audit_log` **append-only** (sin UPDATE/DELETE para usuarios de
aplicación; permisos a nivel BD + ausencia de endpoints de edición). Opción de **hashing encadenado**
(cada fila referencia el hash de la anterior) para detección de manipulación — decisión por ADR.

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración (cada operación
sensible genera exactamente 1 registro); pruebas de **no-PII** en el log; pruebas de autorización de la
consulta; pruebas de inmutabilidad (intento de UPDATE/DELETE rechazado).

**Target Platform**: API REST en contenedor Linux; cliente Ionic.

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: consulta filtrada paginada < 2 s sobre millones de eventos (SC-004) → índices
por usuario/acción/entidad/timestamp.

**Constraints**: cero PII/secretos en claro (FR-002, Principio V); inmutabilidad (FR-003); registrar
también intentos **denegados** (FR-006); consulta solo para roles de auditoría/admin (FR-004).

**Scale/Scope**: tabla de alto volumen (millones de filas); 1 pantalla de consulta + el mecanismo de
captura automática (AOP/eventos) que las demás features usan.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | La auditoría no debe volverse superficie de fuga: redacta/enmascara, referencia por id | ✅ |
| II. QR sin PII | Eventos de QR/escaneo se auditan por id de código/lote, sin PII | ✅ (N/A directo) |
| III. Cero secretos | Nunca registra tokens/contraseñas/cadenas de conexión | ✅ |
| IV. Mínimo privilegio | Consulta restringida a auditor/admin; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | **Es** la implementación del Principio V; append-only, sin PII | ✅ |
| VI. ADR / stack congelado | Mecanismo de inmutabilidad/retención → **ADR** antes de fijar | ⚠️ ver research |

**Resultado del gate**: PASA con **un ADR pendiente** (inmutabilidad concreta + retención). No es
violación: es una decisión de diseño a registrar (append-only en BD vs hashing encadenado vs WORM).

## Project Structure

### Documentation (this feature)

```text
specs/012-auditoria/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> El diseño de datos y contrato se describe inline; la decisión de inmutabilidad/retención se eleva a
> **ADR** en `docs/04-decisiones/` (no se fija en el plan).

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/audit/
    │   ├── controller/     # AuditLogController (consulta paginada + filtros)
    │   ├── service/        # AuditWriter (puerto que el resto consume), AuditQueryService
    │   ├── repository/     # AuditLogRepository
    │   ├── entity/         # AuditLog
    │   ├── dto/            # AuditEvent (escritura), AuditLogSummary (lectura)
    │   └── aspect/         # AuditAspect (captura automática AOP), redacción de PII
    ├── main/resources/db/migration/   # V{n}__create_audit_log.sql (Flyway)
    └── test/java/.../modules/audit/

sigefor/
└── src/app/features/auditoria/
    ├── pages/auditoria/         # consulta filtrada (auditor/admin)
    └── data/audit.service.ts
```

**Structure Decision**: módulo backend `audit/` con un **puerto `AuditWriter`** que las demás features
inyectan (LEGO: las features dependen de la abstracción, no de la tabla). La captura de eventos
"automáticos" usa **AOP** (un `@Auditable` o pointcuts sobre servicios sensibles) más eventos de
seguridad (login/denegado). La **redacción de PII** ocurre en el aspecto antes de persistir.

## Phase 0 — Research

Decisiones / incógnitas:
- **Inmutabilidad** (FR-003) → opciones: (a) tabla append-only con permisos BD que niegan UPDATE/DELETE
  al usuario de la app; (b) **hashing encadenado** (cada fila guarda `hash(prev)` para detectar
  manipulación); (c) almacenamiento WORM externo. **Recomendado registrar como ADR**; MVP = (a) + sin
  endpoints de edición; (b) como refuerzo posterior.
- **Retención** (FR-005) → política por marco legal (LFPDPPP/LGPDPPSO) e ISO 27001 → **ADR**.
- **Captura** → AOP + `ApplicationEvent` de Spring Security para login/denegado; evitar doble registro.
- **No-PII** → estrategia de redacción/enmascarado central reutilizable; las features pasan ids, no PII.

## Phase 1 — Design & Contracts

- **Data model** (inline): `audit_log(id, usuario_id, accion, entidad, entidad_id, timestamp, ip,
  evidencia/diff JSON redactado, prev_hash NULL)`; índices `(usuario_id)`, `(accion)`, `(entidad,
  entidad_id)`, `(timestamp)`.
- **Contract** (inline): `GET /audit` (paginado + filtros usuario/fecha/acción/entidad; solo auditor/
  admin). **No** hay POST público: la escritura es interna vía `AuditWriter`.
- **Quickstart** (inline): ejecutar una operación sensible → aparece exactamente 1 registro sin PII;
  intentar UPDATE/DELETE → rechazado; operación denegada → registrada.

Re-check Constitution tras diseño: el log referencia por id, redacta PII y es append-only; la consulta
está restringida. **Gate sigue en PASA** (con el ADR de inmutabilidad/retención como acción).

## Complexity Tracking

> Sin violaciones. La única decisión abierta (mecanismo de inmutabilidad/retención) se resuelve por ADR
> y no añade complejidad estructural al MVP (tabla append-only + puerto de escritura).
