# Implementation Plan: Incidencias y mantenimiento

**Branch**: `dev` (feature **008-incidencias**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/008-incidencias/spec.md`

## Summary

Implementar el **seguimiento de incidencias** sobre fornituras (daño, falla, extravío, mantenimiento):
reportar, listar con filtro por estado, actualizar/cerrar, y **derivar alertas** de vigencia (preventiva
≤ 90 días = naranja; crítica = caducada = rojo) y de mantenimiento. El reporte de una incidencia puede
cambiar el estado de la fornitura ("en mantenimiento"/"extraviada") y su resolución devolverla a
"disponible". Las **alertas de vigencia son derivadas** (no requieren incidencia manual) y conviven con
las reportadas, usando el **color semántico** de `docs/05-ui-ux.md`.

Enfoque: módulo backend `incidents` en Spring Boot, migración Flyway con `incident`, y feature
`incidencias` en el frontend `sigefor/`. Reusa el módulo `equipment` (**001**, estado y
`fecha_vencimiento`) y el criterio de vigencia ya definido allí.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Frontend: servicios HTTP + componentes Ionic con color semántico.

**Storage**: SQL Server 2022. Tabla `incident` (equipment_id, tipo, descripción, estado, fechas,
reportado_por, actualizado_por). Las alertas de vigencia se **derivan** de `equipment.fecha_vencimiento`
(consulta agregada/vista), no se materializan salvo decisión posterior.

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración (cambio de estado de la
fornitura al reportar/resolver; derivación de alertas por umbral de fecha); contrato; autorización;
auditoría. Frontend: pruebas de servicio + render de color por estado.

**Target Platform**: API REST en contenedor Linux; cliente Ionic.

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: listado paginado < 2 s; cálculo de alertas con consultas agregadas eficientes.

**Constraints**: el criterio de vigencia (≤ 90 días / vencida) MUST coincidir con **001**/**010**
(FR-004); cambios de estado de fornitura coherentes (FR-003); operaciones autorizadas y auditadas
(FR-006); color semántico según `docs/05-ui-ux.md` (FR-007).

**Scale/Scope**: 1 pantalla (listado + reporte + actualización) + vista de alertas; volumen moderado.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Las incidencias no manejan PII (sobre fornituras) | ✅ (N/A PII) |
| II. QR sin PII | No genera ni resuelve QR (puede referenciar fornitura por id) | ✅ (N/A) |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | Reportar/actualizar por rol; consulta a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Reporte/actualización auditados (actor, incidencia, fornitura por id) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack; materializar alertas (si se decide) → ADR | ✅ |

**Resultado del gate**: PASA. La derivación de alertas reutiliza el criterio de 001; sin decisiones
abiertas (materializar alertas es optimización futura → ADR si se adopta).

## Project Structure

### Documentation (this feature)

```text
specs/008-incidencias/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Diseño de datos y contrato inline. La materialización de alertas se eleva a ADR si se adopta.

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/incidents/
    │   ├── controller/     # IncidentController (listar, reportar, actualizar), AlertController (vigencia)
    │   ├── service/        # IncidentService (reporte + cambio de estado fornitura), AlertService (derivación)
    │   ├── repository/     # IncidentRepository
    │   ├── entity/         # Incident
    │   ├── dto/            # IncidentCreateRequest, IncidentSummary, AlertItem
    │   └── mapper/
    ├── main/resources/db/migration/   # V{n}__create_incident.sql (Flyway)
    └── test/java/.../modules/incidents/

sigefor/
└── src/app/features/incidencias/
    ├── pages/incidencias/       # header con filtro de estado + tabla paginada + "Actualizar"
    ├── pages/incidencia-form/   # reportar incidencia
    └── data/incidents.service.ts
```

**Structure Decision**: módulo backend `incidents/`. El cambio de estado de la fornitura se hace vía el
servicio/puerto de **001** (no se duplica el catálogo de estados). La **derivación de alertas** vive en
`AlertService`, que consulta `equipment.fecha_vencimiento` con el **mismo criterio** que 001/010 (una
sola fuente de verdad del umbral de 90 días).

## Phase 0 — Research

Decisiones inline:
- **Alertas derivadas vs materializadas**: MVP = derivadas (consulta agregada por `fecha_vencimiento`);
  materializar (tabla de alertas + job) solo si el volumen lo exige → ADR.
- **Acoplamiento de estado**: reportar incidencia de retiro → fornitura "en mantenimiento"/"extraviada";
  resolver → "disponible" (si procede). Vía puerto a 001; transaccional.
- **Criterio único de vigencia**: reutilizar el cálculo de 001 (≤ 90 días / vencida) para no divergir de
  010 (FR-004).
- **Incidencia sobre fornitura asignada**: definir si se notifica/retira al elemento (Edge Case) — MVP
  registra la incidencia y marca la fornitura; la devolución la gestiona 004.

## Phase 1 — Design & Contracts

- **Data model** (inline): `incident(id, equipment_id, tipo[daño/falla/extravío/mantenimiento],
  descripcion, estado[abierta/en_proceso/resuelta/cerrada], fecha_reporte, fecha_resolucion,
  reportado_por, actualizado_por)`; índices por estado/equipment_id.
- **Contract** (inline): `GET /incidents` (paginado + filtro estado), `POST /incidents` (reportar),
  `PATCH /incidents/{id}` (actualizar estado), `GET /alerts/vigencia` (derivadas: próximas/caducadas).
  Todos con authn + authz por rol.
- **Quickstart** (inline): reportar incidencia de daño → "abierta" + fornitura "en mantenimiento";
  resolver → fornitura "disponible"; fornitura con vencimiento ≤ 30 días aparece en alertas preventivas.

Re-check Constitution tras diseño: sin PII; estados coherentes; criterio de vigencia único. **Gate sigue
en PASA.**

## Complexity Tracking

> Sin violaciones. Alertas derivadas en el MVP (sin job ni tabla extra); materialización diferida a ADR.
