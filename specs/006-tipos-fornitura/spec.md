# Feature Specification: Catálogos genéricos (catalog → catalog_item)

**Feature Branch**: `006-tipos-fornitura`

**Created**: 2026-06-30

**Updated**: 2026-06-30 — reestructuración a **catálogo genérico** (`catalog → catalog_item`).
Sustituye el modelo previo de tablas tipadas (`equipment_type`, `size`, enum `warehouse_type`).

**Updated**: 2026-06-30 — **aclaración de dominio**: *fornitura* NO es una categoría con
subtipos, sino **un tipo de prenda concreto**. El catálogo administrable es de **tipos de prenda**
(`TIPO_PRENDA`) y hoy tiene un **único valor: "Fornitura"**. Se elimina la idea de "subtipos de
fornitura" (chaleco/cinturón/casco nunca fueron tipos).

**Input**: `Requerimientos.MD` §10 (Tipo de fornituras) + necesidad transversal de catálogos
administrables (tallas, tipo de almacén, y futuros: marca, material, color…).

> **Nota de diseño (2026-06-30):** todos los catálogos planos del sistema se modelan con **una
> sola estructura reutilizable** (`catalog` como cabecera + `catalog_item` como elementos), servida
> por **un único módulo/CRUD genérico**. Antes existían tablas tipadas por catálogo
> (`equipment_type`, `size`); ahora un catálogo es un registro en `catalog` (p. ej. `TIPO_PRENDA`)
> y sus valores son filas en `catalog_item`. Esto sigue el principio LEGO: piezas conectables sin
> duplicar CRUD por cada catálogo. El **almacén** (005) y el **elemento** (003) NO son catálogos:
> son entidades/maestros; solo sus *clasificaciones* (tipo de almacén, sexo, tipo de sangre) lo son.

> **Aclaración de dominio (2026-06-30) — "tipo de prenda", no "tipo de fornitura".** Una
> **fornitura** es un **tipo de prenda** específico de la corporación; no es una categoría que
> agrupe subtipos. Por eso el catálogo se llama **tipo de prenda** (`code = TIPO_PRENDA`) y su
> **único valor inicial es "Fornitura"**. El catálogo se conserva —aunque hoy tenga un solo valor—
> porque el cliente pide poder administrarlo (extensibilidad: mañana podrían darse de alta otras
> prendas). En consecuencia, el FK `equipment.equipment_type_id` referencia el **tipo de prenda**
> de cada fornitura (hoy, siempre "Fornitura").

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar valores de un catálogo (CRUD) (Priority: P1)

Un usuario autorizado registra y mantiene los valores de un catálogo (p. ej. los **tipos de
prenda**, hoy con el único valor "Fornitura"), cada uno con nombre, descripción y —cuando
aplica— foto representativa. La misma pantalla y API sirven para cualquier catálogo (tipo de
prenda, tallas, tipo de almacén…) seleccionando primero el catálogo.

**Why this priority**: El tipo de prenda es un atributo de catálogo requerido por el alta de
fornituras (**001**); el catálogo debe existir antes. El mismo mecanismo habilita el resto de
catálogos del sistema.

**Independent Test**: Verificar que el valor "Fornitura" existe en el catálogo `TIPO_PRENDA` y
puede seleccionarse al dar de alta una fornitura; crear un segundo tipo de prenda y verlo en la
lista; un valor en uso no se elimina (se desactiva).

**Acceptance Scenarios**:

1. **Given** un usuario autorizado sobre un catálogo, **When** registra un valor con nombre único
   (dentro de ese catálogo) y datos válidos, **Then** queda disponible para seleccionarse donde el
   catálogo se consume.
2. **Given** un valor usado por fornituras/almacenes, **When** se intenta eliminar, **Then** el
   sistema lo impide y ofrece desactivarlo.
3. **Given** un valor desactivado, **When** se abre un formulario que consume ese catálogo, **Then**
   no aparece como opción seleccionable.

### User Story 2 - Catálogos dependientes (jerarquía item→item) (Priority: P2)

Un usuario gestiona **tallas** que pueden depender del **tipo de prenda**. El sistema soporta la
jerarquía `TALLA → TIPO_PRENDA` (una talla cuelga opcionalmente de un tipo de prenda vía
`parent_item_id`, o es una talla **global** sin padre). Con un único tipo de prenda ("Fornitura")
la dependencia es hoy trivial —todas las tallas cuelgan de "Fornitura" o son globales—, pero el
mecanismo se conserva para cuando existan más tipos de prenda: cada prenda podría tener su propio
juego de tallas sin que se mezclen.

**Independent Test**: Crear la talla "M" ligada al tipo de prenda "Fornitura"; al capturar una
fornitura, "M" aparece como opción. Al dar de alta un segundo tipo de prenda con sus propias
tallas, esas tallas solo se ofrecen para ese tipo.

**Acceptance Scenarios**:

1. **Given** el catálogo `TALLA` con jerarquía habilitada, **When** se crea una talla ligada a un
   tipo de prenda, **Then** solo se ofrece para fornituras de ese tipo.
2. **Given** una talla global (sin padre), **When** se captura cualquier fornitura, **Then** la
   talla está disponible.

### Edge Cases

- Nombre de valor duplicado **dentro del mismo catálogo**: rechazar (unicidad normalizada por
  catálogo). El mismo nombre puede existir en catálogos distintos.
- `code` de catálogo desconocido o inexistente: rechazar la operación.
- Borrar un **catálogo de sistema** (semilla, p. ej. `TIPO_PRENDA`, `TIPO_ALMACEN`): impedido; solo
  se administran sus valores.
- Desactivar el **último valor** de un catálogo que un formulario obligatorio consume (p. ej. dejar
  `TIPO_PRENDA` sin ningún valor activo): impedido mientras el catálogo sea requerido por el alta
  de fornituras.
- Foto del valor: aplica solo a catálogos que la declaren (p. ej. `TIPO_PRENDA`); no es PII.

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
  (`parent_item_id`), p. ej. `TALLA` colgando de `TIPO_PRENDA`.
- **FR-006**: Los catálogos base (semilla) MUST marcarse como **de sistema** (`system = true`) para
  impedir su borrado; sus valores sí se administran.
- **FR-007**: El catálogo `TIPO_PRENDA` MUST sembrarse con el valor **"Fornitura"** como tipo de
  prenda inicial del sistema; el catálogo es administrable para dar de alta tipos de prenda
  adicionales en el futuro.
- **FR-008**: Las operaciones MUST requerir autorización por rol y quedar auditadas.

### Key Entities

- **Catálogo** (`catalog`): cabecera. Atributos: `code` (único, estable, p. ej. `TIPO_PRENDA`,
  `TALLA`, `TIPO_ALMACEN`), `nombre`, `descripcion` (opc.), `system` (bool, no borrable si `true`),
  `active`.
- **Valor de catálogo** (`catalog_item`): `catalog_id` (FK → `catalog`), `code` (opc., único dentro
  del catálogo), `nombre`, `nombre_normalizado` (trim + colapso de espacios + casefold; único dentro
  del catálogo), `descripcion` (opc.), `foto_url` (opc.), `parent_item_id` (FK self opc. → jerarquía),
  `orden` (opc.), `active`.

### Catálogos iniciales (semilla)

| `catalog.code`  | Uso                                                   | Foto | Jerarquía            | Valores semilla |
|-----------------|-------------------------------------------------------|------|----------------------|-----------------|
| `TIPO_PRENDA`   | Tipo de prenda controlada (hoy: **Fornitura**)        | Sí   | —                    | `Fornitura`     |
| `TALLA`         | Tallas                                                | No   | Padre → `TIPO_PRENDA`| — (se capturan) |
| `TIPO_ALMACEN`  | Clasificación de almacén (CENTRAL/REGIONAL/…)         | No   | —                    | CENTRAL, REGIONAL, MOVIL, TEMPORAL |

> Catálogos candidatos a futuro (no en este alcance): `MARCA`, `MATERIAL`, `COLOR`, y —de la spec
> **003**— `SEXO`, `TIPO_SANGRE`, que pueden migrar a esta misma estructura. **`MUNICIPIO`/`ESTADO`
> dejan de ser catálogo**: se capturan como **texto libre** (ver specs 003 y 005).

## Success Criteria *(mandatory)*

- **SC-001**: Un usuario crea o edita un valor de catálogo en menos de 1 minuto.
- **SC-002**: El 100% de intentos de eliminar un valor en uso son bloqueados.
- **SC-003**: Agregar un catálogo nuevo (p. ej. `MARCA`) NO requiere tabla ni CRUD nuevos: basta un
  registro en `catalog` y consumir el CRUD genérico.
- **SC-004**: Dar de alta una fornitura ofrece "Fornitura" como tipo de prenda sin configuración
  previa (el catálogo `TIPO_PRENDA` ya está sembrado).

## Assumptions

- Una **fornitura** es un **tipo de prenda** concreto (no una categoría). El catálogo `TIPO_PRENDA`
  arranca con un único valor, "Fornitura"; se conserva como catálogo administrable por
  extensibilidad, no porque hoy existan varios tipos.
- La **talla** es un catálogo que puede depender del tipo de prenda; se modela con `parent_item_id`.
  Una talla sin padre es global. Con un solo tipo de prenda la dependencia es trivial, pero el
  mecanismo queda disponible para futuros tipos.
- El **tipo de almacén** deja de ser un `enum` en código y pasa al catálogo `TIPO_ALMACEN`
  (administrable). La migración conserva los valores actuales (CENTRAL/REGIONAL/MOVIL/TEMPORAL) como
  semilla. Ver spec **005**.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos** (consume `TIPO_PRENDA`/`TALLA`), **005-almacenes**
  (consume `TIPO_ALMACEN`).
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- Decisión: [ADR 0007](../../docs/04-decisiones/0007-catalogos-genericos.md).

> **Estado de implementación (2026-06-30):** el backend ya migró a la estructura genérica
> (`modules/catalog` + Flyway `V15__generic_catalog.sql`, que crea `catalog`/`catalog_item`, siembra
> los catálogos y migra datos; se retiraron `modules/equipmenttypes` y el enum `warehouse_type`). El
> frontend consume el CRUD genérico (`core/catalog`).
> **Sincronización con el código:** resuelta. La migración `V17__rename_tipo_fornitura_to_tipo_prenda.sql`
> renombra el catálogo a **`TIPO_PRENDA`** y deja el único valor **"Fornitura"**; la constante
> `CatalogCodes.TIPO_PRENDA` y los usos en `sigefor` están actualizados; `plan.md`, `tasks.md` y
> `data-model.md` de esta feature fueron regenerados contra este spec.
</content>
</invoke>
