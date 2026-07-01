# Data Model — Catálogos genéricos (feature 006)

> Modelo **implementado** (ADR 0007). Estructura genérica `catalog` + `catalog_item` que sustituye
> las tablas tipadas `equipment_type`/`size` y el enum `warehouse_type`. Fuente transversal:
> [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md). Migraciones:
> `V15__generic_catalog.sql` (estructura, semilla y migración de datos) y
> `V17__rename_tipo_fornitura_to_tipo_prenda.sql` (renombra `TIPO_FORNITURA` → `TIPO_PRENDA` y deja
> el único valor "Fornitura").

## Entidades

### `catalog` — cabecera de un catálogo administrable

| Campo        | Tipo (BD)              | Notas                                                            |
|--------------|------------------------|-----------------------------------------------------------------|
| id           | BIGINT IDENTITY (PK)   | Identificador interno.                                           |
| code         | NVARCHAR(40) **único** | Clave estable de negocio (p. ej. `TIPO_PRENDA`, `TALLA`, `TIPO_ALMACEN`). |
| nombre       | NVARCHAR(120)          | Legible (p. ej. "Tipo de prenda").                              |
| descripcion  | NVARCHAR(500) (null)   |                                                                 |
| is_system    | BIT (default 0)        | `1` = catálogo de sistema (semilla): **no borrable**; solo se administran sus valores. |
| active       | BIT (default 1)        |                                                                 |
| created_at / updated_at | DATETIME2   | Timestamps.                                                      |

### `catalog_item` — valor de un catálogo

| Campo              | Tipo (BD)              | Notas                                                        |
|--------------------|------------------------|--------------------------------------------------------------|
| id                 | BIGINT IDENTITY (PK)   | Identificador interno; destino de las FKs consumidoras.      |
| catalog_id         | BIGINT FK → `catalog`  | Catálogo al que pertenece.                                   |
| code               | NVARCHAR(40) (null)    | Opcional; estable cuando el consumidor lo necesita (p. ej. `TIPO_ALMACEN` = CENTRAL). |
| nombre             | NVARCHAR(120)          | Valor mostrado.                                              |
| nombre_normalizado | NVARCHAR(120)          | trim + colapso de espacios + casefold; base de la unicidad. |
| descripcion        | NVARCHAR(500) (null)   |                                                             |
| foto_url           | NVARCHAR(500) (null)   | Solo catálogos que la declaran (p. ej. `TIPO_PRENDA`). No PII. |
| parent_item_id     | BIGINT FK self (null)  | Jerarquía item→item (p. ej. `TALLA` colgada de `TIPO_PRENDA`). Null = global. |
| orden              | INT (null)             | Orden de presentación.                                       |
| active             | BIT (default 1)        | Solo activos seleccionables donde el catálogo se consume.   |
| created_at / updated_at | DATETIME2         | Timestamps.                                                  |

## Índices y unicidad

- `uk_catalog_code` — `catalog.code` único.
- `idx_catalog_item_catalog` (`catalog_id`), `idx_catalog_item_active` (`active`),
  `idx_catalog_item_parent` (`parent_item_id`).
- **Unicidad de nombre por catálogo**, distinguiendo por padre para permitir el mismo valor (p. ej.
  talla "M") bajo tipos distintos:
  - `uk_catalog_item_named` — único (`catalog_id`, `nombre_normalizado`) **WHERE `parent_item_id IS NULL`** (valores globales).
  - `uk_catalog_item_named_child` — único (`catalog_id`, `parent_item_id`, `nombre_normalizado`) **WHERE `parent_item_id IS NOT NULL`**.

## Relaciones

- `catalog` 1—N `catalog_item`.
- `catalog_item` 1—N `catalog_item` (auto-referencia `parent_item_id`; jerarquía talla→tipo de prenda).
- `equipment` N—1 `catalog_item` (tipo de prenda `TIPO_PRENDA` vía `equipment_type_id`; talla `TALLA`
  vía `size_id`, nullable).
- `warehouse` N—1 `catalog_item` (tipo de almacén `TIPO_ALMACEN` vía `tipo_item_id`).

> La coherencia "el `catalog_item` referenciado pertenece al catálogo correcto" **no** la impone el
> esquema (se perdió el tipado por FK por catálogo); se valida en `CatalogService` comparando el
> `catalog.code` esperado al resolver cada referencia (ADR 0007, consecuencia aceptada).

## Reglas de validación

- `nombre` obligatorio; único (normalizado) dentro del catálogo (409 al duplicar).
- `code` de catálogo debe existir; operación rechazada si es desconocido.
- No eliminar un valor **en uso** (referenciado por `equipment`/`warehouse`): se **desactiva** (`active = 0`).
- Catálogo con `is_system = 1`: no borrable; solo se administran sus valores.
- Solo valores `active = 1` se ofrecen donde el catálogo se consume.

## Catálogos semilla (sistema, `is_system = 1`)

| `catalog.code` | nombre           | Foto | Jerarquía              | Valores semilla                         |
|----------------|------------------|------|------------------------|-----------------------------------------|
| `TIPO_PRENDA`  | Tipo de prenda   | Sí   | —                      | **Fornitura** (único)                   |
| `TALLA`        | Tallas           | No   | Padre → `TIPO_PRENDA`  | — (se capturan; global o por tipo)      |
| `TIPO_ALMACEN` | Tipos de almacén | No   | —                      | CENTRAL, REGIONAL, MOVIL, TEMPORAL      |

## Estado / transiciones

Un `catalog_item` es `active` (seleccionable) o inactivo (`active = 0`, conservado por integridad
histórica y referencial). No hay borrado físico de valores en uso. Los catálogos de sistema no se
crean ni borran desde la API; se siembran por migración.
