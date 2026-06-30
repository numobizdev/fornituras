# Implementation Plan: Bajas definitivas de fornituras

**Branch**: `dev` (feature **009-bajas**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/009-bajas/spec.md`

## Summary

Implementar la **baja definitiva** de fornituras: buscar por código QR, registrar el **motivo**
(catálogo) y dar de baja. La fornitura pasa a estado "baja definitiva", deja de ser asignable/
trasladable y **conserva su historial**. Se bloquea la baja si tiene **asignación vigente** o está **en
traslado** (consistente con 001 FR-008 y 007). Incluye consulta paginada de bajas con filtros. Por
política, una baja **no se revierte** (un error se corrige con ajuste auditado).

Enfoque: módulo backend `decommissions` en Spring Boot, migración Flyway con `decommission` +
`decommission_reason`, y feature `bajas` en el frontend `sigefor/`. Reusa `equipment` (**001**, estado +
resolución por código) y el componente de captura (**014**).

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Captura de QR vía componente **014**. Frontend: servicios HTTP +
componentes Ionic.

**Storage**: SQL Server 2022. Tabla `decommission` (equipment_id, motivo_id, fecha, responsable,
observaciones) y catálogo `decommission_reason`. Actualiza `equipment.status` a "baja definitiva".

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración (bloqueo con asignación
vigente/en traslado; rechazo de operaciones sobre fornitura dada de baja); contrato; autorización;
auditoría. Frontend: pruebas de servicio + del flujo de baja por QR.

**Target Platform**: API REST en contenedor Linux; cliente Ionic.

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: baja en pocos pasos; listado de bajas paginado < 2 s.

**Constraints**: no dar de baja con asignación vigente/en traslado (FR-002, SC-003); una fornitura dada
de baja no se asigna/traslada y conserva historial (FR-003, SC-002); toda baja autorizada y auditada
(FR-005, SC-001).

**Scale/Scope**: 2 pantallas (dar de baja por QR + listado de bajas); volumen moderado.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | La baja no maneja PII (sobre fornituras) | ✅ (N/A PII) |
| II. QR sin PII / resolución server-side | Búsqueda por código resuelta en servidor (001) | ✅ |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | Dar de baja restringido a rol elevado; consulta a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Baja auditada (actor, fornitura por id, motivo, fecha) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack; política de no-reversión documentada (plan/ADR) | ✅ |

**Resultado del gate**: PASA. Dependencia de 001/004/007 = orden de implementación.

## Project Structure

### Documentation (this feature)

```text
specs/009-bajas/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Diseño de datos y contrato inline. La política de no-reversión (corrección por ajuste auditado) se
> documenta aquí; si se formaliza un flujo de ajuste, se eleva a ADR.

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/decommissions/
    │   ├── controller/     # DecommissionController (dar de baja, listar)
    │   ├── service/        # DecommissionService (validación de bloqueo, cambio de estado, auditoría)
    │   ├── repository/     # DecommissionRepository, DecommissionReasonRepository
    │   ├── entity/         # Decommission, DecommissionReason
    │   ├── dto/            # DecommissionRequest, DecommissionSummary
    │   └── mapper/
    ├── main/resources/db/migration/   # V{n}__create_decommission.sql (Flyway)
    └── test/java/.../modules/decommissions/

sigefor/
└── src/app/features/bajas/
    ├── pages/bajas/             # listado paginado de bajas + filtros
    ├── pages/baja-form/         # buscar por QR + motivo + confirmar
    └── data/decommissions.service.ts
```

**Structure Decision**: módulo backend `decommissions/`. La validación de bloqueo (asignación vigente/en
traslado) consulta **001** vía `EquipmentLifecycleQuery` (el mismo puerto que usa 001 para FR-008); el
cambio de estado se hace por el servicio de 001 para no duplicar reglas. La búsqueda por código reusa la
resolución server-side de 001 y el componente de captura **014**.

## Phase 0 — Research

Decisiones inline:
- **Bloqueo**: antes de dar de baja, consultar `EquipmentLifecycleQuery` (¿asignación vigente? ¿en
  traslado?) → si sí, rechazar con motivo (FR-002).
- **Motivo**: catálogo `decommission_reason` (caducidad, daño, extravío, obsolescencia…).
- **No-reversión**: una baja definitiva no se revierte; un error se corrige con un **ajuste auditado**
  (registro de corrección con justificación) — documentado; flujo formal → ADR si se requiere.
- **Baja masiva por lote**: fuera del MVP (mejora posible).

## Phase 1 — Design & Contracts

- **Data model** (inline): `decommission(id, equipment_id, motivo_id, fecha, responsable, observaciones)`;
  catálogo `decommission_reason(id, nombre, active)`. `equipment.status` → "baja definitiva".
- **Contract** (inline): `GET /decommissions` (paginado + filtros fecha/tipo/motivo),
  `POST /decommissions` (dar de baja: valida bloqueo, registra motivo/fecha, cambia estado, audita).
  Todos con authn + authz por rol.
- **Quickstart** (inline): dar de baja una fornitura disponible por QR con motivo → "baja definitiva" +
  aparece en lista; intentar baja con asignación vigente → bloqueado; operar una dada de baja → rechazado.

Re-check Constitution tras diseño: sin PII; bloqueo consistente con 001/004/007; historial preservado.
**Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones. El bloqueo reusa el puerto de 001; la no-reversión es política documentada, no
> complejidad estructural.
