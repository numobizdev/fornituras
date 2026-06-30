# Implementation Plan: Almacenes

**Branch**: `dev` (feature **005-almacenes**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-almacenes/spec.md`

## Summary

Construir el **catálogo de almacenes** (ubicaciones físicas de resguardo). Es prerequisito del alta
de fornituras (**001**, almacén obligatorio) y del origen/destino de traslados (**007**). CRUD con
nombre/identificador único, ubicación/descripción y **desactivación** (no borrado) cuando el almacén
tiene fornituras o traslados asociados. Sin PII: el eje es **integridad referencial** y
**autorización + auditoría**.

Enfoque: módulo backend `warehouses` en Spring Boot (controller/service/repository/entity/dto +
mapper), migración Flyway con `warehouse`, y feature `almacenes` en el frontend `sigefor/`.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Frontend: servicios HTTP + componentes standalone Ionic.

**Storage**: SQL Server 2022. Tabla `warehouse` (nombre/identificador único, ubicación/descripción,
`active`). Sin Always Encrypted (no hay PII).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración y migración;
contrato y autorización. Frontend: pruebas de servicio con `HttpTestingController`.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: alta/edición de almacén < 1 min (SC-001); listado paginado < 2 s (SC-003).

**Constraints**: nombre/identificador único (normalizado); no eliminar almacén en uso (solo
desactivar); solo activos seleccionables en alta (001) y traslados (007); operaciones autorizadas y
auditadas (Principios IV, V).

**Scale/Scope**: catálogo pequeño (decenas de filas); 2 pantallas (listado + formulario).

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Sin PII; validación de entrada en el borde | ✅ (N/A PII) |
| II. QR sin PII | No genera ni resuelve QR | ✅ (N/A) |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | CRUD restringido a rol (ADMIN/almacén); consulta a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Alta/edición/desactivación auditadas (actor, almacén, cuándo) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack ni dependencias nuevas | ✅ |

**Resultado del gate**: PASA sin decisiones abiertas. Catálogo de soporte; disciplina en integridad
referencial y autorización.

## Project Structure

### Documentation (this feature)

```text
specs/005-almacenes/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Catálogo simple: diseño de datos y contrato inline (sin research/data-model/contracts separados).

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/warehouses/
    │   ├── controller/     # WarehouseController
    │   ├── service/        # WarehouseService (desactivación, unicidad, conteo de uso)
    │   ├── repository/     # WarehouseRepository
    │   ├── entity/         # Warehouse
    │   ├── dto/            # WarehouseCreateRequest, WarehouseSummary, WarehouseDetail
    │   └── mapper/
    ├── main/resources/db/migration/   # V9__create_warehouse.sql (Flyway)
    └── test/java/.../modules/warehouses/

sigefor/
└── src/app/features/almacenes/
    ├── pages/almacenes/         # listado paginado
    ├── pages/almacen-form/      # alta/edición
    └── data/warehouses.service.ts
```

**Structure Decision**: módulo backend `warehouses/` siguiendo el patrón existente. La verificación
"en uso" consulta `equipment` (001) y `transfer` (007); mientras no existan, la regla se implementa y
su test de integración se completa al integrar esas features.

> **Orden de migración**: este plan asume `V9` después de `V8` (tipos, feature 006). Si 005 se
> implementa antes que 006, ajustar el número de versión Flyway al siguiente disponible; nunca
> reutilizar ni renumerar una migración ya aplicada.

## Phase 0 — Research

Sin incógnitas técnicas. Decisiones inline:
- **Borrado vs desactivación**: nunca borrado físico de un almacén en uso; columna `active`.
- **Conteo de uso**: el bloqueo de eliminación consulta fornituras (001) y traslados (007); por orden
  de implementación, la consulta se aísla tras un puerto/método que devuelve 0 hasta que esas tablas
  existan.

## Phase 1 — Design & Contracts

- **Data model** (inline): `warehouse(id, nombre UNIQUE normalizado, ubicacion/descripcion, active,
  created_at/by)`.
- **Contract** (inline): `GET /warehouses` (paginado + filtro `active`), `POST /warehouses`,
  `PUT /warehouses/{id}`, `PATCH /warehouses/{id}/deactivate`. Todos con authn (JWT) + authz por rol;
  `POST/PUT` validan unicidad (409 si duplica).
- **Quickstart** (inline): crear almacén → seleccionable en 001/007; intentar eliminar uno con
  fornituras → bloqueado; desactivar → no aparece como destino.

Re-check Constitution tras diseño: sin PII; integridad referencial y autorización cubiertas. **Gate
sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. Catálogo de soporte; no introduce complejidad estructural.
