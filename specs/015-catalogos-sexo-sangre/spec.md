# Feature Specification: Migrar SEXO y TIPO_SANGRE a la estructura genérica de catálogos

**Feature Branch**: `015-catalogos-sexo-sangre`

**Created**: 2026-06-30

**Status**: Draft

**Input**: [ADR 0007](../../docs/04-decisiones/0007-catalogos-genericos.md) — pendiente registrado:
migrar los catálogos `SEXO` y `TIPO_SANGRE` (hoy tablas planas) a la estructura genérica
`catalog → catalog_item`.

> **Contexto.** El ADR 0007 unificó los catálogos planos del sistema en una estructura genérica
> (`catalog` + `catalog_item`) servida por un único CRUD (`modules/catalog`). `TIPO_PRENDA`, `TALLA`
> y `TIPO_ALMACEN` ya viven ahí. **`SEXO` y `TIPO_SANGRE` quedaron como tablas planas**
> (`sexo` con `nombre`; `tipo_sangre` con `etiqueta`), con sus propias entidades (`Sexo`,
> `TipoSangre`), repositorios y el `OfficerCatalogController`, usadas por el elemento (**003**).
> Esta feature completa esa migración, sin cambiar el comportamiento observable del padrón.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar SEXO y TIPO_SANGRE desde el CRUD genérico (Priority: P1)

Un usuario autorizado administra los valores de **sexo** y **tipo de sangre** con el **mismo CRUD
genérico** que el resto de catálogos (seleccionando el catálogo por su `code`), en lugar de un
mecanismo propio. Los valores existentes se conservan.

**Why this priority**: Elimina el código específico duplicado (entidades/repos/controller propios)
y alinea `SEXO`/`TIPO_SANGRE` con el principio LEGO del ADR 0007.

**Independent Test**: Listar los valores de `SEXO` y `TIPO_SANGRE` vía el CRUD genérico devuelve los
mismos valores que antes (MASCULINO/FEMENINO; O±/A±/B±/AB±); crear/desactivar un valor funciona igual
que en los demás catálogos.

**Acceptance Scenarios**:

1. **Given** los catálogos `SEXO` y `TIPO_SANGRE` migrados, **When** un usuario lista sus valores por
   el CRUD genérico, **Then** obtiene exactamente los valores que existían en las tablas planas.
2. **Given** un valor de sexo/tipo de sangre en uso por un elemento, **When** se intenta eliminar,
   **Then** el sistema lo impide y ofrece desactivarlo (misma regla que el resto de catálogos).

### User Story 2 - El padrón sigue funcionando igual (Priority: P1)

El alta y la consulta de elementos (**003**) siguen resolviendo sexo y tipo de sangre, ahora contra
`catalog_item`, sin cambio observable para el usuario.

**Why this priority**: La migración no debe romper el padrón, que es la feature de mayor sensibilidad.

**Independent Test**: Dar de alta un elemento seleccionando sexo y tipo de sangre; consultarlo y ver
los mismos valores; los elementos ya existentes conservan su sexo/tipo de sangre tras la migración.

**Acceptance Scenarios**:

1. **Given** elementos existentes con `sexo_id`/`tipo_sangre_id` a las tablas planas, **When** se
   aplica la migración, **Then** sus FKs quedan repuntadas al `catalog_item` equivalente sin pérdida.
2. **Given** el formulario de alta de elemento, **When** se capturan sexo y tipo de sangre, **Then**
   se ofrecen los valores activos de `SEXO`/`TIPO_SANGRE` desde el catálogo genérico.

### Edge Cases

- Valor de la tabla plana sin equivalente esperado: la migración lo crea como `catalog_item` (no se
  pierde ningún valor en uso).
- `code` de `catalog_item` para `SEXO`/`TIPO_SANGRE`: opcional; si el frontend dependía de una
  etiqueta estable, se conserva en `code`/`nombre`.
- Filtro del padrón por sexo (`sexoId`): debe seguir funcionando apuntando al nuevo `catalog_item`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST modelar `SEXO` y `TIPO_SANGRE` como catálogos de sistema
  (`is_system = true`) en la estructura genérica `catalog → catalog_item`.
- **FR-002**: La migración MUST **preservar los datos**: copiar los valores de `sexo`/`tipo_sangre` a
  `catalog_item` y **repuntar** `officers.sexo_id`/`tipo_sangre_id` a los nuevos `catalog_item`.
- **FR-003**: Tras repuntar las FKs, la migración MUST retirar las tablas `sexo`/`tipo_sangre` y el
  código específico (`entity/Sexo`, `entity/TipoSangre`, sus repositorios y el mecanismo propio de
  exposición) sin dejar referencias colgadas.
- **FR-004**: La administración de `SEXO`/`TIPO_SANGRE` MUST usar el CRUD genérico de catálogos
  (`modules/catalog`), con las mismas reglas (unicidad por catálogo, desactivación en uso, autorización).
- **FR-005**: El alta/consulta de elementos (**003**) MUST resolver sexo y tipo de sangre contra
  `catalog_item` validando el `catalog.code` esperado (`SEXO`/`TIPO_SANGRE`), sin cambio observable.
- **FR-006**: El frontend MUST consumir `SEXO`/`TIPO_SANGRE` desde `core/catalog` (con
  `CATALOG_CODES.SEXO`/`TIPO_SANGRE`), retirando el servicio específico si existía.

### Key Entities

- **Catálogo** (`catalog`) y **Valor de catálogo** (`catalog_item`): ver [spec 006](../006-tipos-fornitura/spec.md)
  y [ADR 0007](../../docs/04-decisiones/0007-catalogos-genericos.md). Se añaden dos catálogos de
  sistema: `SEXO` y `TIPO_SANGRE`.
- **Elemento** (`officer`): sus FKs `sexo_id`/`tipo_sangre_id` pasan a apuntar a `catalog_item`
  (antes a las tablas planas). El tipo de sangre sigue siendo un valor sensible; el FK no revela PII.

### Catálogos a migrar (semilla conservada)

| `catalog.code` | Origen (tabla plana)        | Valores conservados                      |
|----------------|-----------------------------|------------------------------------------|
| `SEXO`         | `sexo` (`nombre`)           | MASCULINO, FEMENINO                       |
| `TIPO_SANGRE`  | `tipo_sangre` (`etiqueta`)  | O+, O−, A+, A−, B+, B−, AB+, AB−          |

## Success Criteria *(mandatory)*

- **SC-001**: El 100% de los elementos existentes conservan su sexo y tipo de sangre tras la migración
  (repunte de FKs sin pérdida).
- **SC-002**: `SEXO`/`TIPO_SANGRE` se administran por el mismo CRUD genérico que los demás catálogos;
  no queda código específico de estos dos catálogos.
- **SC-003**: El alta y la consulta del padrón (**003**) no cambian su comportamiento observable.

## Assumptions

- La migración es **data-preserving** y se implementa como una migración Flyway nueva (número libre
  siguiente), siguiendo el patrón de `V15`/`V17` (copiar → repuntar FKs → retirar lo viejo).
- No se cambia la política de PII: el tipo de sangre sigue siendo sensible; solo cambia dónde se
  almacena el catálogo, no su tratamiento.
- El `OfficerCatalogController` (o su equivalente) se retira o se reduce a un passthrough del CRUD
  genérico; el frontend del padrón consume `core/catalog`.

## Dependencies

- [ADR 0007](../../docs/04-decisiones/0007-catalogos-genericos.md) (decisión ya tomada; esta feature la completa).
- Features: **006-tipos-fornitura** (CRUD genérico `modules/catalog`), **003-elementos-padron**
  (consumidor de `SEXO`/`TIPO_SANGRE`).
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
