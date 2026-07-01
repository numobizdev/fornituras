# Implementation Plan: Catálogos genéricos (catalog → catalog_item)

**Branch**: `dev` (feature **006-tipos-fornitura**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/006-tipos-fornitura/spec.md`

> **Estado: IMPLEMENTADO.** Este plan se **regeneró** (2026-06-30) contra el spec actualizado: los
> catálogos ya no son tablas tipadas (`equipment_type`/`size`) sino una **estructura genérica**
> `catalog` + `catalog_item` servida por un único CRUD (ADR 0007). Además, *fornitura* es un **tipo
> de prenda** concreto: el catálogo `TIPO_PRENDA` tiene un único valor de sistema, "Fornitura". El
> plan describe el diseño **tal como está construido**, no el modelo tipado previo.

## Summary

Modelar **todos los catálogos planos** del sistema con una sola estructura reutilizable —`catalog`
(cabecera) + `catalog_item` (valores), con jerarquía opcional `parent_item_id`— servida por un
**único módulo/CRUD genérico**. Sustituye las tablas tipadas por catálogo (LEGO: piezas conectables
sin duplicar CRUD). Catálogos semilla de sistema: **`TIPO_PRENDA`** (tipo de prenda; único valor
"Fornitura"), **`TALLA`** (opcionalmente colgada de `TIPO_PRENDA`) y **`TIPO_ALMACEN`**
(CENTRAL/REGIONAL/MOVIL/TEMPORAL). Prerequisito del alta de fornituras (**001**) y de almacenes
(**005**).

No hay PII: la foto (solo en catálogos que la declaran, p. ej. `TIPO_PRENDA`) es genérica del
equipo. El eje no es cifrado sino **integridad referencial en la capa de servicio** (validar que un
`catalog_item` pertenece al `catalog.code` esperado donde se consume), **desactivación** (no borrado)
de valores en uso, y **autorización + auditoría**.

Enfoque: módulo backend `modules/catalog` en Spring Boot (controller/service/repository/entity/dto),
migración Flyway `V15__generic_catalog.sql` (crea la estructura, siembra los catálogos y migra los
datos del modelo tipado) más `V17__rename_tipo_fornitura_to_tipo_prenda.sql` (renombra el catálogo a
`TIPO_PRENDA` y deja el único valor "Fornitura"), y en el frontend `sigefor/` el cliente genérico
`core/catalog` consumido por la feature `tipos` (adaptador a la API histórica), almacenes y fornituras.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Validation, Data JPA), Flyway
(`flyway-sqlserver`), `mssql-jdbc`. Almacenamiento de foto: por referencia (`foto_url`), no cifrado
(imagen genérica, no PII). Frontend: servicios HTTP + componentes standalone Ionic (`core/catalog`).

**Storage**: SQL Server 2022. Tablas genéricas **`catalog`** (`code` único, `nombre`, `descripcion`,
`is_system`, `active`) y **`catalog_item`** (`catalog_id` FK, `code?`, `nombre`, `nombre_normalizado`,
`descripcion?`, `foto_url?`, `parent_item_id?` FK self, `orden?`, `active`). Unicidad de
`nombre_normalizado` por catálogo (índices filtrados que distinguen por padre). Sin Always Encrypted
(no hay PII). Las FKs de `equipment` (tipo de prenda/talla) y `warehouse` (tipo de almacén) apuntan a
`catalog_item`.

**Testing**: JUnit 5 + Spring Boot Test; carga de contexto que ejecuta Flyway (V15/V17) contra la BD
de test; pruebas de servicio (unicidad, desactivación, resolución por `code`) y de autorización.
Frontend: pruebas de servicio con `HttpTestingController`.

**Target Platform**: API REST en contenedor Linux; cliente Ionic (web + móvil vía Capacitor).

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: alta/edición de valor en < 1 min (SC-001); listado paginado por catálogo < 2 s.

**Constraints**: `nombre` único (normalizado) **por catálogo**; no eliminar valores en uso (solo
desactivar); solo activos seleccionables donde el catálogo se consume; catálogos de sistema
(`is_system`) no borrables; toda operación autorizada y auditada (Principios IV, V).

**Scale/Scope**: catálogos pequeños (decenas–cientos de filas); un CRUD genérico + 2 pantallas de
administración (listado + formulario) reutilizadas por todos los catálogos.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | Sin PII; foto genérica del equipo. Validación de tipo/tamaño de imagen en el borde | ✅ (N/A PII) |
| II. QR sin PII | No genera ni resuelve QR | ✅ (N/A) |
| III. Cero secretos | Sin secretos nuevos | ✅ |
| IV. Mínimo privilegio | CRUD restringido a rol (ADMIN); consulta abierta a roles operativos; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | Alta/edición/desactivación auditadas (actor, catálogo/valor, cuándo) | ✅ |
| VI. ADR / stack congelado | Cambio de modelado registrado en **ADR 0007**; sin cambios de stack ni dependencias nuevas | ✅ |

**Resultado del gate**: PASA sin decisiones abiertas. Es infraestructura de catálogos de baja
sensibilidad; la disciplina está en integridad referencial (validación por `code` en servicio) y
autorización.

## Project Structure

### Documentation (this feature)

```text
specs/006-tipos-fornitura/
├── plan.md              # Este archivo (regenerado por /speckit-plan)
├── data-model.md        # Modelo de datos del catálogo genérico (Phase 1)
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Contrato REST inline en este plan (CRUD genérico pequeño). El modelo de datos se detalla en
> `data-model.md`; el modelo transversal del sistema vive en `docs/03-modelo-datos.md`.

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/catalog/
    │   ├── controller/     # CatalogController (CRUD genérico por catalog.code)
    │   ├── service/        # CatalogService (unicidad, desactivación, resolución por code)
    │   ├── repository/     # CatalogRepository, CatalogItemRepository
    │   ├── entity/         # Catalog, CatalogItem
    │   ├── dto/            # CatalogItemCreateRequest, *Summary, *Detail
    │   └── CatalogCodes.java   # TIPO_PRENDA, TALLA, TIPO_ALMACEN
    ├── main/resources/db/migration/
    │   ├── V15__generic_catalog.sql   # crea catalog/catalog_item, siembra y migra datos
    │   └── V17__rename_tipo_fornitura_to_tipo_prenda.sql   # rename + único valor "Fornitura"
    └── test/java/.../modules/  # equipment/catalog: resolución por code, desactivación

sigefor/
└── src/app/
    ├── core/catalog/                 # cliente genérico
    │   ├── catalog.model.ts          # CATALOG_CODES (TIPO_PRENDA/TALLA/TIPO_ALMACEN), interfaces
    │   └── catalog.service.ts        # CRUD genérico por code
    └── features/tipos/
        ├── pages/tipos/              # listado paginado de valores
        ├── pages/tipo-form/         # alta/edición de valor (con foto y tallas)
        └── data/equipment-types.service.ts   # adaptador core/catalog → API histórica de tipos/tallas
```

**Structure Decision**: módulo backend **único** `modules/catalog` (patrón de `auth`, `users`,
`qrcodes`) que sirve a todos los catálogos; la coherencia de "qué catálogo" se valida por `code` en
`CatalogService`. En el frontend, `core/catalog` es el cliente genérico; la feature `tipos` conserva
su interfaz previa mediante un **adaptador** (`equipment-types.service.ts`) para no reescribir las
páginas de tipos ni los formularios de fornitura. La verificación "en uso" consulta las entidades
consumidoras (`equipment`, `warehouse`) antes de permitir borrado.

## Phase 0 — Research

Sin incógnitas técnicas abiertas; decisiones registradas en **ADR 0007** y en el spec:

- **Genérico vs tipado**: se elige `catalog` + `catalog_item` con CRUD único sobre tablas tipadas por
  catálogo (que duplicaban pila de clases por cada catálogo). Trade-off aceptado: se pierde el tipado
  por FK por catálogo; se mitiga validando el `catalog.code` esperado al resolver cada referencia.
- **Jerarquía (talla por tipo de prenda)**: `parent_item_id` (FK self) en `catalog_item`; una talla
  sin padre es global. Con un único tipo de prenda la jerarquía es hoy trivial, pero el mecanismo
  queda disponible para futuros tipos.
- **Fornitura = tipo de prenda**: `TIPO_PRENDA` se siembra con el único valor "Fornitura"; el catálogo
  se conserva administrable por extensibilidad (ADR 0007, aclaración de dominio).
- **Borrado vs desactivación**: nunca borrado físico de un valor en uso; columna `active`. Catálogos
  de sistema (`is_system`) no borrables.
- **Foto**: solo en catálogos que la declaran; imagen genérica (no PII); almacenamiento por referencia.

## Phase 1 — Design & Contracts

- **Data model**: ver [`data-model.md`](./data-model.md) (`catalog` / `catalog_item`, índices de
  unicidad por catálogo, jerarquía `parent_item_id`, catálogos semilla).
- **Contract** (inline): CRUD genérico parametrizado por `code` de catálogo —
  `GET /catalogs/{code}/items` (paginado + filtro `active`, y `parentItemId` para dependientes),
  `POST /catalogs/{code}/items`, `GET /catalog-items/{id}`, `PUT /catalog-items/{id}`,
  `PATCH /catalog-items/{id}/deactivate`. Todos con authn (JWT) + authz por rol. `POST/PUT` validan
  unicidad de `nombre_normalizado` dentro del catálogo (409 si duplica) y que el `code` exista.
- **Quickstart** (inline): el alta de fornituras (001) ofrece "Fornitura" como tipo de prenda sin
  configuración previa; crear un valor con nombre duplicado en el mismo catálogo → 409; intentar
  eliminar un valor en uso → bloqueado con oferta de desactivar; desactivar → desaparece del alta.

Re-check Constitution tras diseño: catálogo sin PII; integridad por `code` en servicio y autorización
cubiertas. **Gate sigue en PASA.**

## Complexity Tracking

> Sin violaciones de la constitución. La estructura genérica **reduce** superficie (un módulo/CRUD en
> lugar de uno por catálogo); el trade-off de tipado por FK está documentado y mitigado en ADR 0007.
