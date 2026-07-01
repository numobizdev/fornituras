# Feature Specification: Catálogos genéricos (catalog → catalog_item)

**Feature Branch**: `006-tipos-fornitura`

**Created**: 2026-06-30

**Updated**: 2026-06-30 — reestructuración a **catálogo genérico** (`catalog → catalog_item`).
Sustituye el modelo previo de tablas tipadas (`equipment_type`, `size`, enum `warehouse_type`).

**Input**: `Requerimientos.MD` §10 (Tipo de fornituras) + necesidad transversal de catálogos
administrables (tallas, tipo de almacén, y futuros: marca, material, color…).

> **Nota de diseño (2026-06-30):** todos los catálogos planos del sistema se modelan con **una
> sola estructura reutilizable** (`catalog` como cabecera + `catalog_item` como elementos), servida
> por **un único módulo/CRUD genérico**. Antes existían tablas tipadas por catálogo
> (`equipment_type`, `size`); ahora un catálogo es un registro en `catalog` (p. ej. `TIPO_FORNITURA`)
> y sus valores son filas en `catalog_item`. Esto sigue el principio LEGO: piezas conectables sin
> duplicar CRUD por cada catálogo. El **almacén** (005) y el **elemento** (003) NO son catálogos:
> son entidades/maestros; solo sus *clasificaciones* (tipo de almacén, sexo, tipo de sangre) lo son.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar valores de un catálogo (CRUD) (Priority: P1)

Un usuario autorizado registra y mantiene los valores de un catálogo (p. ej. los **tipos de
fornitura**: chaleco antibala, cinturón táctico, casco), cada uno con nombre, descripción y
—cuando aplica— foto representativa. La misma pantalla y API sirven para cualquier catálogo
(tipos, tallas, tipo de almacén…) seleccionando primero el catálogo.

**Why this priority**: El tipo de fornitura es un atributo de catálogo requerido por el alta de
fornituras (**001**); el catálogo debe existir antes. El mismo mecanismo habilita el resto de
catálogos del sistema.

**Independent Test**: Crear el valor "Chaleco antibala" en el catálogo `TIPO_FORNITURA`, verlo en la
lista y poder seleccionarlo al dar de alta una fornitura; un valor en uso no se elimina (se
desactiva).

**Acceptance Scenarios**:

1. **Given** un usuario autorizado sobre un catálogo, **When** registra un valor con nombre único
   (dentro de ese catálogo) y datos válidos, **Then** queda disponible para seleccionarse donde el
   catálogo se consume.
2. **Given** un valor usado por fornituras/almacenes, **When** se intenta eliminar, **Then** el
   sistema lo impide y ofrece desactivarlo.
3. **Given** un valor desactivado, **When** se abre un formulario que consume ese catálogo, **Then**
   no aparece como opción seleccionable.

### User Story 2 - Catálogos dependientes (jerarquía item→item) (Priority: P2)

Un usuario gestiona **tallas** que dependen del **tipo de fornitura** (un chaleco y un cinturón no
comparten tallas). Cada valor de `TALLA` puede opcionalmente colgar de un valor de `TIPO_FORNITURA`
(padre), o ser una talla global (sin padre).

**Independent Test**: Crear la talla "M" ligada al tipo "Chaleco antibala"; al capturar una
fornitura de ese tipo, "M" aparece como opción; para otro tipo sin esa talla, no.

**Acceptance Scenarios**:

1. **Given** el catálogo `TALLA` con jerarquía habilitada, **When** se crea una talla ligada a un
   tipo, **Then** solo se ofrece para fornituras de ese tipo.
2. **Given** una talla global (sin padre), **When** se captura cualquier fornitura, **Then** la
   talla está disponible.

### Edge Cases

- Nombre de valor duplicado **dentro del mismo catálogo**: rechazar (unicidad normalizada por
  catálogo). El mismo nombre puede existir en catálogos distintos.
- `code` de catálogo desconocido o inexistente: rechazar la operación.
- Borrar un **catálogo de sistema** (semilla, p. ej. `TIPO_ALMACEN`): impedido; solo se administran
  sus valores.
- Foto del valor: aplica solo a catálogos que la declaren (p. ej. `TIPO_FORNITURA`); no es PII.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST modelar los catálogos con una estructura genérica **cabecera**
  (`catalog`) + **elementos** (`catalog_item`), administrable por un único CRUD.
- **FR-002**: El sistema MUST permitir crear, consultar (paginado/filtrado por catálogo), editar y
  **desactivar** valores de un catálogo con nombre (único dentro del catálogo), descripción y —si el
  catálogo lo declara— foto.
- **FR-003**: El sistema MUST impedir eliminar un valor en uso; en su lugar permite desactivarlo.
- **FR-004**: Solo valores **activos** MUST ofrecerse donde el catálogo se consume (alta de
  fornituras, tipo de almacén, etc.).
- **FR-005**: El sistema MUST soportar catálogos **dependientes** vía enlace opcional item→item
  (`parent_item_id`), p. ej. `TALLA` colgando de `TIPO_FORNITURA`.
- **FR-006**: Los catálogos base (semilla) MUST marcarse como **de sistema** (`system = true`) para
  impedir su borrado; sus valores sí se administran.
- **FR-007**: Las operaciones MUST requerir autorización por rol y quedar auditadas.

### Key Entities

- **Catálogo** (`catalog`): cabecera. Atributos: `code` (único, estable, p. ej. `TIPO_FORNITURA`,
  `TALLA`, `TIPO_ALMACEN`), `nombre`, `descripcion` (opc.), `system` (bool, no borrable si `true`),
  `active`.
- **Valor de catálogo** (`catalog_item`): `catalog_id` (FK → `catalog`), `code` (opc., único dentro
  del catálogo), `nombre`, `nombre_normalizado` (trim + colapso de espacios + casefold; único dentro
  del catálogo), `descripcion` (opc.), `foto_url` (opc.), `parent_item_id` (FK self opc. → jerarquía),
  `orden` (opc.), `active`.

### Catálogos iniciales (semilla)

| `catalog.code`   | Uso                                              | Foto | Jerarquía            |
|------------------|--------------------------------------------------|------|----------------------|
| `TIPO_FORNITURA` | Tipos de fornitura (chaleco, cinturón, casco…)   | Sí   | —                    |
| `TALLA`          | Tallas                                            | No   | Padre → `TIPO_FORNITURA` |
| `TIPO_ALMACEN`   | Clasificación de almacén (CENTRAL/REGIONAL/…)     | No   | —                    |

> Catálogos candidatos a futuro (no en este alcance): `MARCA`, `MATERIAL`, `COLOR`, y —de la spec
> **003**— `SEXO`, `TIPO_SANGRE`, que pueden migrar a esta misma estructura. **`MUNICIPIO`/`ESTADO`
> dejan de ser catálogo**: se capturan como **texto libre** (ver specs 003 y 005).

## Success Criteria *(mandatory)*

- **SC-001**: Un usuario crea o edita un valor de catálogo en menos de 1 minuto.
- **SC-002**: El 100% de intentos de eliminar un valor en uso son bloqueados.
- **SC-003**: Agregar un catálogo nuevo (p. ej. `MARCA`) NO requiere tabla ni CRUD nuevos: basta un
  registro en `catalog` y consumir el CRUD genérico.

## Assumptions

- La **talla** es un catálogo dependiente del tipo de fornitura (ambos requeridos por **001**); se
  modela con `parent_item_id`. Una talla sin padre es global.
- El **tipo de almacén** deja de ser un `enum` en código y pasa al catálogo `TIPO_ALMACEN`
  (administrable). La migración conserva los valores actuales (CENTRAL/REGIONAL/MOVIL/TEMPORAL) como
  semilla. Ver spec **005**.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos** (consume `TIPO_FORNITURA`/`TALLA`), **005-almacenes**
  (consume `TIPO_ALMACEN`).
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).

> **Estado de implementación (2026-06-30):** el backend ya migró a la estructura genérica
> (`modules/catalog` + Flyway `V15__generic_catalog.sql`, que crea `catalog`/`catalog_item`, siembra
> `TIPO_FORNITURA`/`TALLA`/`TIPO_ALMACEN` y migra datos; se retiraron `modules/equipmenttypes` y el
> enum `warehouse_type`). El frontend consume el CRUD genérico (`core/catalog`).
> **Deuda de sincronización:** `plan.md`, `tasks.md` y `data-model.md` de esta feature describen aún
> el modelo tipado previo y deben regenerarse (`/speckit-plan`, `/speckit-tasks`) contra este spec.
