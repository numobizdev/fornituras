# Implementation Plan: Migrar SEXO y TIPO_SANGRE a la estructura genérica de catálogos

**Branch**: `015-catalogos-sexo-sangre` | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/015-catalogos-sexo-sangre/spec.md`

> **Estado: IMPLEMENTADO.** Completa el único pendiente del ADR 0007: `SEXO` y `TIPO_SANGRE` dejan de
> ser tablas planas y pasan a la estructura genérica `catalog → catalog_item`, servida por el CRUD
> genérico existente (`modules/catalog`). Migración **data-preserving** (patrón `V15`/`V17`).

## Summary

Migrar los catálogos `SEXO` y `TIPO_SANGRE` (hoy tablas planas `sexo`/`tipo_sangre` con entidades,
repos y `OfficerCatalogController` propios) a `catalog`/`catalog_item`, repuntando las FKs
`officers.sexo_id`/`tipo_sangre_id` a `catalog_item`, y retirando el código específico. El padrón
(**003**) resuelve sexo/tipo de sangre contra `CatalogService` (`requireActiveItem`/`resolveNames`),
sin cambio observable. El frontend consume `SEXO`/`TIPO_SANGRE` desde `core/catalog`.

## Technical Context

**Language/Version**: Java 25 (`fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Frontend: `core/catalog` (cliente genérico).

**Storage**: SQL Server 2022. Migración `V18__migrate_sexo_tiposangre_to_catalog.sql`: siembra
`SEXO`/`TIPO_SANGRE` en `catalog`, copia sus valores a `catalog_item` (conservando nombre/estado/
orden), repunta `officers.sexo_id`/`tipo_sangre_id` a `catalog_item` (FKs `fk_officers_sexo_item`/
`fk_officers_tipo_sangre_item`) y retira las tablas `sexo`/`tipo_sangre`.

**Testing**: JUnit 5 + Spring Boot Test; la carga de contexto ejecuta Flyway (`V18`) contra la BD de
test; `OfficerServiceTest` cubre la validación de catálogos vía `CatalogService`. Frontend: build.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: sin cambio; resolución de nombres en lote (`resolveNames`) evita N+1 en el listado.

**Constraints**: migración data-preserving (cero pérdida de sexo/tipo de sangre de elementos
existentes); comportamiento del padrón sin cambio observable; unicidad por catálogo y desactivación
en uso heredadas del CRUD genérico.

**Scale/Scope**: 2 catálogos (2 + 8 valores), un módulo consumidor (`officers`) y su formulario.

## Constitution Check

| Principio | Cómo lo cumple | Estado |
|-----------|----------------|--------|
| I. Seguridad/privacidad primero | No cambia el tratamiento de PII; el tipo de sangre sigue sensible (FK no revela PII) | ✅ |
| II. QR sin PII | No aplica | ✅ (N/A) |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | Administración por el CRUD genérico (escritura solo ADMIN); lectura autenticada | ✅ |
| V. Auditoría sin fugas | Alta/edición/desactivación de valores auditadas por `CatalogService` | ✅ |
| VI. ADR / stack congelado | Ejecuta el ADR 0007 (sin nueva decisión de stack) | ✅ |

**Resultado del gate**: PASA. Refactor que **reduce** superficie (retira entidades/repos/controller
propios); riesgo acotado a la migración de datos, cubierta por el patrón probado de `V15`/`V17`.

## Project Structure

### Documentation (this feature)

```text
specs/015-catalogos-sexo-sangre/
├── spec.md
├── plan.md              # Este archivo
└── tasks.md             # Phase 2
```

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/.../modules/catalog/
    │   ├── CatalogCodes.java            # + SEXO, TIPO_SANGRE
    │   └── service/CatalogService.java  # + resolveNames(Collection<Long>) (batch)
    ├── main/java/.../modules/officers/
    │   ├── service/OfficerService.java  # resuelve sexo/tipo de sangre vía CatalogService
    │   └── (retirados) entity/Sexo, entity/TipoSangre, repository/Sexo|TipoSangreRepository,
    │       controller/OfficerCatalogController, dto/CatalogItem
    ├── main/resources/db/migration/V18__migrate_sexo_tiposangre_to_catalog.sql
    └── test/java/.../modules/officers/OfficerServiceTest.java  # mock de CatalogService

sigefor/
└── src/app/
    ├── core/catalog/catalog.model.ts          # CATALOG_CODES + SEXO, TIPO_SANGRE
    └── features/elementos/data/officers.service.ts  # listSexos/listTiposSangre vía core/catalog
```

**Structure Decision**: se reutiliza el módulo `catalog` (no se crea nada nuevo): la administración
de `SEXO`/`TIPO_SANGRE` usa su CRUD y `CatalogController`. `OfficerService` deja de depender de repos
propios y resuelve por `code` contra `CatalogService`. En el frontend, `officers.service` delega en el
cliente `core/catalog` y mapea `nombre → etiqueta` para no tocar las plantillas del padrón.

## Phase 0 — Research

Sin incógnitas: la decisión ya está en ADR 0007. Nota técnica verificada: `NameNormalizer` no elimina
`+`/`-`, por lo que `O+`/`O-`/`AB+`… normalizan distinto y no colisionan en el índice único por
catálogo. La migración repunta las FKs uniendo por `nombre` (único por catálogo).

## Phase 1 — Design & Contracts

- **Data model**: `SEXO`/`TIPO_SANGRE` como `catalog` (is_system) + `catalog_item`; `officers.sexo_id`/
  `tipo_sangre_id` → `catalog_item`. Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- **Contract**: se retiran `GET /sexos` y `GET /tipos-sangre`; el frontend usa el endpoint genérico
  `GET /catalogs/{code}/items/active` (`SEXO`/`TIPO_SANGRE`). La API del padrón (`/officers`) no cambia.
- **Quickstart**: dar de alta un elemento ofrece sexo/tipo de sangre desde el catálogo genérico; los
  elementos existentes conservan su valor tras `V18`.

## Complexity Tracking

> Sin violaciones. La feature **reduce** complejidad: elimina un mecanismo de catálogo paralelo.
