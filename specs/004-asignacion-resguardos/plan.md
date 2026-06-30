# Implementation Plan: Asignación de fornituras y resguardos

**Branch**: `dev` (feature **004-asignacion-resguardos**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/004-asignacion-resguardos/spec.md`

## Summary

Implementar el **núcleo de SIGEFOR**: ligar una **fornitura** a un **elemento** mediante un flujo
de dos pasos (identificar fornitura por código QR → buscar elemento → asignar), con historial de
movimientos, devolución/reasignación, firma electrónica opcional y **generación de resguardo**.

La pantalla muestra al cargar las **asignaciones vigentes** (paginadas). El paso 1 resuelve el
**código `FOR-XXXXX`** (módulo `qrcodes`, ADR 0005) a una fornitura y verifica que esté
**disponible**; el paso 2 busca el elemento (nombre/placa/CURP/RFC) respetando el enmascaramiento
de PII. Todo cambio queda **auditado**.

> **Dependencia dura:** esta feature consume las entidades **Fornitura (001)** y **Elemento (003)**,
> que **aún no están implementadas** en el backend (módulos `equipment`/`officers` no existen; sí
> existen `auth`, `users`, `qrcodes`). 004 no se puede completar hasta tener al menos el mínimo de
> 001 (fornitura + resolución `codigo → fornitura`) y 003 (búsqueda de elemento). Ver §Phase 0.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Generación del **resguardo PDF** reutilizando el patrón ya
presente en `qrcodes` (`QrPdfService` usa una librería de PDF; reusar la misma). Frontend: servicios
HTTP + componente de captura de QR (spec **014**) + cámara Capacitor.

**Storage**: SQL Server 2022. Tabla nueva `assignment` (equipment_id, officer_id, fechas,
asignado_por, recibido_por, firma). Lee `equipment` (001) y `officers` (003). El resguardo se
genera al vuelo (no necesariamente se persiste el PDF; sí su metadato).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración (incluida
**concurrencia**: dos asignaciones simultáneas de la misma fornitura → solo una gana); pruebas de
contrato y de autorización/auditoría. Frontend: pruebas de servicio + del flujo de 2 pasos.

**Target Platform**: API REST en contenedor; cliente Ionic (web + móvil con cámara vía Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: asignar una fornitura disponible en < 1 minuto (SC-001); listado de
vigentes paginado < 2 s.

**Constraints**: una fornitura **nunca** con dos asignaciones vigentes (integridad/concurrencia);
resolución `codigo → fornitura` solo en servidor (Principio II); PII del elemento enmascarada por
rol y acceso auditado (Principios IV, V); sin PII en logs.

**Scale/Scope**: decenas de miles de fornituras/elementos; 1 pantalla (con wizard) + resguardo.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | PII del elemento solo vía 003 (cifrada/enmascarada); resguardo sin exponer datos de más | ✅ |
| II. QR sin PII / resolución server-side | `codigo → fornitura` se resuelve en servidor tras authn+authz; el código es opaco (ADR 0005) | ✅ |
| III. Cero secretos | Sin secretos nuevos; firma electrónica/clave (si aplica) desde gestor de secretos | ✅ |
| IV. Mínimo privilegio | Asignar/reasignar requiere rol (ADMIN/CAPTURISTA hoy); consulta sin acciones para roles limitados | ✅ |
| V. Auditoría sin fugas | ASSIGN/RETURN/REASSIGN auditados (actor, fornitura, elemento, fecha) sin PII | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack; reusar lib PDF existente; decisiones (firma electrónica) → ADR si aplica | ✅ |

**Resultado del gate**: PASA. Única "violación" aparente = depender de 001/003 no implementados;
no es violación de principio sino **orden de implementación** (ver Phase 0).

## Project Structure

### Documentation (this feature)

```text
specs/004-asignacion-resguardos/
├── plan.md              # Este archivo
├── research.md          # Phase 0: concurrencia, resguardo, firma, dependencias 001/003
├── data-model.md        # Phase 1: assignment + lecturas de equipment/officer
├── quickstart.md        # Phase 1: cómo correr y validar el flujo de 2 pasos
├── contracts/
│   └── assignments-api.md
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/assignments/
    │   ├── controller/     # AssignmentController (vigentes, asignar, devolver/reasignar, resguardo)
    │   ├── service/        # AssignmentService (transacción + bloqueo de concurrencia), ResguardoPdfService
    │   ├── repository/     # AssignmentRepository (vigente por fornitura, historial, paginación)
    │   ├── entity/         # Assignment
    │   ├── dto/            # AssignRequest, AssignmentSummary, ResguardoMeta
    │   └── mapper/
    ├── main/resources/db/migration/   # V{n}__create_assignment.sql (Flyway)
    └── test/java/.../modules/assignments/

sigefor/
└── src/app/features/asignacion/        # página ya andamiada
    ├── pages/asignacion/                # listado vigentes + wizard 2 pasos
    └── data/assignments.service.ts
```

**Structure Decision**: módulo backend `assignments/` siguiendo el patrón existente. Reusa el
**componente de captura de QR** (spec 014) para el paso 1 y el **servicio de elementos** (003)
para el paso 2. La resolución `codigo → fornitura` se expone desde el módulo `equipment` (001) y
se consume aquí (no se duplica). El resguardo PDF reutiliza la librería ya usada por `qrcodes`.

## Phase 0 — Research

Ver [research.md](./research.md). Puntos clave:
- **Dependencias 001/003:** definir el mínimo de 001 (entidad `equipment` con `codigo_qr` + endpoint
  `resolver codigo → fornitura` con estado) y de 003 (búsqueda de elemento) que 004 necesita.
- **Concurrencia:** garantizar una sola asignación vigente por fornitura (índice único filtrado o
  bloqueo transaccional) — decisión de research.
- **Resguardo:** generar PDF al vuelo reutilizando la lib de `qrcodes`; persistir solo metadatos.
- **Firma electrónica:** opcional; si el dispositivo no la soporta, el resguardo se emite sin firma
  y queda auditado (decisión de alcance → posible ADR).

## Phase 1 — Design & Contracts

- **Data model**: [data-model.md](./data-model.md) — `assignment` (con `fecha_devolucion` NULL =
  vigente) + **índice único filtrado** `(equipment_id) WHERE fecha_devolucion IS NULL`.
- **Contracts**: [contracts/assignments-api.md](./contracts/assignments-api.md) —
  `GET /assignments` (vigentes paginadas), `POST /assignments` (asignar, valida disponibilidad),
  `POST /assignments/{id}/return` (devolver), `POST /assignments/reassign` (reasignar),
  `GET /assignments/{id}/resguardo` (PDF). Consumen `GET /equipment/by-codigo/{codigo}` (de 001).
- **Quickstart**: [quickstart.md](./quickstart.md) — sembrar fornitura+elemento, ejecutar el flujo
  de 2 pasos, validar concurrencia, historial y auditoría.

Re-check Constitution tras diseño: el listado de vigentes y el resguardo no exponen PII a roles no
autorizados; la resolución del código es server-side; concurrencia controlada. **Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. La dependencia de 001/003 es **orden de implementación**, no
> complejidad estructural; se gestiona con el mínimo viable de esas features (ver research).
