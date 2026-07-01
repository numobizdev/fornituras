# Implementation Plan: Almacenes

**Branch**: `dev` (feature **005-almacenes**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-almacenes/spec.md`

> **Estado: IMPLEMENTADO.** Plan **regenerado** (2026-06-30) al modelo real: tras ADR 0007, la
> clasificación `tipo` es una **FK a `catalog_item`** (`tipo_item_id`, catálogo `TIPO_ALMACEN`) y
> `municipio`/`estado` son **texto libre** (ya no FK a `municipio`). Se retiró el enum `WarehouseType`
> y el módulo `equipmenttypes` (sustituido por `catalog`).

## Summary

Construir el módulo de **almacenes** como **entidad operativa / dato maestro** (no catálogo plano):
ubicaciones físicas de resguardo con **clave de negocio única** (`codigo`), **nombre** único, **tipo
de almacén** (FK al catálogo `TIPO_ALMACEN`), **ubicación** (municipio/estado texto libre + dirección
+ geolocalización opcional), **responsable** (`user`), **contacto institucional** y **cupo**. Es
prerequisito del alta de fornituras (**001**, almacén obligatorio) y del origen/destino de traslados
(**007**). CRUD con unicidad normalizada y **desactivación** (no borrado) cuando el almacén tiene
fornituras o traslados asociados.

Sin PII de elementos, pero **la ubicación/responsable de una armería es información sensible**: el
eje es **integridad referencial** + **autorización por rol sobre campos sensibles** + **auditoría**.
El detalle de la entidad vive en [`data-model.md`](./data-model.md).

Enfoque: módulo backend `warehouses` en Spring Boot (controller/service/repository/entity/dto +
mapper, con el puerto `WarehouseUsageQuery`), migración Flyway `V10__create_warehouse.sql` (y el
repunte a `tipo_item_id`/texto libre en `V15__generic_catalog.sql`), y feature `almacenes` en el
frontend `sigefor/`. El `tipo` se resuelve contra el catálogo `TIPO_ALMACEN` (módulo `catalog`).

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Frontend: servicios HTTP + componentes standalone Ionic.

**Storage**: SQL Server 2022. Tabla `warehouse` (`codigo` único, `nombre`/`nombre_normalizado`
único, `tipo_item_id` FK → `catalog_item` del catálogo `TIPO_ALMACEN`, `municipio`/`estado` **texto
libre**, dirección + geolocalización, `responsable_id` FK → `users`, contacto, `capacidad`,
`active`). Sin Always Encrypted (no hay PII de elementos); los campos sensibles
(dirección/geo/responsable) se protegen por RBAC, no por cifrado de columna.

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL) para integración y migración;
contrato y autorización. Frontend: pruebas de servicio con `HttpTestingController`.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: alta/edición de almacén < 1 min (SC-001); listado paginado < 2 s (SC-003).

**Constraints**: `codigo` y `nombre` únicos (normalizado); no eliminar almacén en uso (solo
desactivar); solo activos seleccionables en alta (001) y traslados (007); campos sensibles
(dirección/geo/responsable/contacto) solo visibles a ADMIN/almacén; operaciones autorizadas y
auditadas (Principios I, IV, V).

**Scale/Scope**: conjunto pequeño (decenas de filas); 2 pantallas (listado + formulario con
secciones identidad/ubicación/responsable/operativo).

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Sin PII de elementos; **campos sensibles** (dirección/geo/responsable/contacto) protegidos por RBAC y lectura auditada; contacto institucional, no personal; validación en el borde | ✅ |
| II. QR sin PII | No genera ni resuelve QR | ✅ (N/A) |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | CRUD restringido a rol (ADMIN/almacén); consulta de campos no sensibles a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Alta/edición/desactivación y **cambio de responsable** auditados (actor, almacén, cuándo) | ✅ |
| VI. ADR / stack congelado | Sin cambios de stack ni dependencias nuevas | ✅ |

**Resultado del gate**: PASA sin decisiones abiertas. Entidad operativa (no catálogo); disciplina en
integridad referencial, autorización por campo sensible y auditoría.

## Project Structure

### Documentation (this feature)

```text
specs/005-almacenes/
├── spec.md              # Qué y por qué
├── plan.md              # Este archivo
├── data-model.md        # Entidad operativa: campos, sensibilidad, contrato
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Reclasificada de catálogo a **entidad operativa**: el diseño de datos y el contrato se promueven a
> [`data-model.md`](./data-model.md) (ya no caben inline).

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/warehouses/
    │   ├── controller/     # WarehouseController (@PreAuthorize por endpoint)
    │   ├── service/        # WarehouseService + WarehouseUsageQuery/DefaultWarehouseUsageQuery (conteo de uso)
    │   ├── repository/     # WarehouseRepository
    │   ├── entity/         # Warehouse (tipo_item_id FK, municipio/estado texto libre)
    │   ├── dto/            # WarehouseCreateRequest, WarehouseSummary, WarehouseDetail
    │   └── mapper/         # WarehouseMapper (entity↔DTO)
    ├── main/resources/db/migration/   # V10__create_warehouse.sql (+ repunte en V15)
    └── test/java/.../modules/warehouses/   # WarehouseServiceTest

sigefor/
└── src/app/features/almacenes/
    ├── pages/almacenes/         # listado paginado (campos no sensibles + ocupación)
    ├── pages/almacen-form/      # alta/edición (secciones: identidad, ubicación, responsable, operativo)
    └── data/warehouses.service.ts
```

**Structure Decision**: módulo backend `warehouses/` (entidad + DTOs Summary/Detail + mapper +
normalizador de `common/text` reutilizado). El **tipo** se resuelve contra el catálogo `TIPO_ALMACEN`
(módulo `catalog`, `requireActiveItem`). La verificación "en uso" consulta `equipment` (001) y
`transfer` (007) a través del puerto `WarehouseUsageQuery`; `responsable_id` (`users`) ya existe;
`municipio`/`estado` son texto libre (sin FK).

> **Orden de migración (real)**: `V10__create_warehouse.sql` creó la tabla (con enum `tipo` y
> `municipio_id` iniciales); `V15__generic_catalog.sql` repuntó `tipo`→`tipo_item_id` (FK a
> `catalog_item`) y `municipio_id`→`municipio`/`estado` de texto libre (ADR 0007). Nunca reutilizar
> ni renumerar una migración ya aplicada.

## Phase 0 — Research

Sin incógnitas técnicas. Decisiones inline:
- **Entidad operativa, no catálogo**: el almacén tiene atributos de lugar (clave, tipo, ubicación,
  responsable, cupo); se modela como dato maestro con su propio `data-model.md`.
- **Clave vs nombre**: `codigo` es la clave de negocio **estable** (traslados/etiquetas); `nombre` es
  legible y puede cambiar. Ambos únicos (normalizados).
- **Borrado vs desactivación**: nunca borrado físico de un almacén en uso; columna `active`.
- **Conteo de uso**: el bloqueo de eliminación consulta fornituras (001) y traslados (007); por orden
  de implementación, la consulta se aísla tras un puerto que devuelve 0 hasta que esas tablas existan.
- **Reutilización**: catálogo `TIPO_ALMACEN` (módulo `catalog`) para el tipo; `user` como
  responsable; normalizador de `common/text`. `municipio`/`estado` como **texto libre** (ADR 0007),
  no catálogo geográfico.
- **Sensibilidad por campo**: dirección/geo/responsable/contacto solo a ADMIN/almacén → DTOs
  `Summary` (no sensible) vs `Detail` (sensible, filtrado por rol).

## Phase 1 — Design & Contracts

- **Data model**: ver [`data-model.md`](./data-model.md) (campos, restricciones, clasificación de
  sensibilidad, DTOs).
- **Contract**: `GET /warehouses` (paginado + filtro `active`/`tipo`, devuelve `Summary`),
  `GET /warehouses/{id}` (`Detail`, sensible, ADMIN/almacén), `POST /warehouses`,
  `PUT /warehouses/{id}`, `PATCH /warehouses/{id}/deactivate`. Todos con authn (JWT) + authz por rol;
  `POST/PUT` validan unicidad de `codigo` y `nombre` (409 si duplica).
- **Quickstart**: crear almacén con clave/tipo/ubicación/responsable → seleccionable en 001/007;
  rol operativo ve solo identidad/estado; intentar eliminar uno con fornituras → bloqueado;
  desactivar → no aparece como destino.

Re-check Constitution tras diseño: sin PII de elementos; campos sensibles bajo RBAC; integridad
referencial y auditoría cubiertas. **Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. Entidad operativa con sensibilidad por campo; la complejidad
> añadida (más columnas, FKs, RBAC por DTO) es inherente al dominio, no estructural — reutiliza
> patrones ya presentes en otros módulos (`catalog`, `officers`) y el catálogo `TIPO_ALMACEN`.
