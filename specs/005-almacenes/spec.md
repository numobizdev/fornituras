# Feature Specification: Almacenes

**Feature Branch**: `005-almacenes`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §11 (Almacenes) — ubicaciones de resguardo de fornituras.

> **Nota de diseño (2026-06-30):** el almacén **no** es un catálogo plano (tipo `sexo`/`motivo_baja`),
> sino una **entidad operativa / dato maestro**: un lugar físico real con clave de negocio,
> clasificación, ubicación, responsable y cupo. Su estructura completa vive en
> [`data-model.md`](./data-model.md).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar almacenes (CRUD) (Priority: P1)

Un usuario autorizado registra, consulta, edita y desactiva almacenes (ubicaciones físicas
donde se resguardan las fornituras). Cada almacén tiene una **clave de negocio única**, un
**tipo** (catálogo `TIPO_ALMACEN`), una **ubicación** (municipio/estado como **texto libre** +
dirección) y un **responsable**. Cada fornitura pertenece a un almacén.

**Why this priority**: El almacén es un atributo obligatorio del alta de fornituras (**001**) y
el origen/destino de los traslados (**007**); debe existir antes que ellos.

**Independent Test**: Crear un almacén, verlo en la lista, editarlo y desactivarlo; un almacén
con fornituras no puede eliminarse (solo desactivarse).

**Acceptance Scenarios**:

1. **Given** un usuario autorizado, **When** registra un almacén con nombre/ubicación válidos,
   **Then** queda disponible para asignarse a fornituras y traslados.
2. **Given** un almacén con fornituras asociadas, **When** se intenta eliminar, **Then** el
   sistema lo impide y ofrece **desactivarlo** en su lugar.
3. **Given** un almacén desactivado, **When** se da de alta una fornitura, **Then** no aparece
   como destino seleccionable.

### Edge Cases

- **Clave (`codigo`)** o **nombre** de almacén duplicado: rechazar (unicidad independiente de
  mayúsculas/espacios).
- Existencias por almacén: el conteo de fornituras por almacén alimenta reportes (**011**) y la
  **ocupación** frente a la `capacidad` declarada (no se almacena: se deriva por conteo).
- Capacidad excedida: si el almacén declara `capacidad`, intentar ubicar más fornituras de las que
  caben se **advierte** (no bloquea en MVP; ver Assumptions).
- Cambio de responsable: queda auditado (campo sensible).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir crear, consultar (paginado), editar y **desactivar**
  almacenes.
- **FR-002**: La **clave de negocio** (`codigo`) y el **nombre** del almacén MUST ser únicos
  (unicidad normalizada: trim + colapso de espacios + casefold).
- **FR-003**: El sistema MUST impedir eliminar un almacén con fornituras o traslados asociados;
  en su lugar permite desactivarlo.
- **FR-004**: Solo almacenes **activos** MUST ofrecerse como ubicación en alta de fornituras y
  como origen/destino de traslados.
- **FR-005**: Las operaciones de alta/edición/baja de almacén MUST requerir autorización por rol
  y quedar auditadas.
- **FR-006**: El almacén MUST registrar **clasificación** (`tipo`, resuelto contra el catálogo
  `TIPO_ALMACEN` de la spec **006**), **ubicación** (municipio y estado como **texto libre** +
  dirección) y **responsable** (usuario a cargo), además de datos operativos opcionales (cupo,
  contacto institucional, geolocalización).
- **FR-007**: Los campos **sensibles del almacén** (dirección, geolocalización, responsable,
  contacto) MUST exponerse solo a roles autorizados (ADMIN/almacén); los roles operativos básicos
  ven únicamente identidad y estado. Toda lectura de estos campos queda sujeta a autorización.
- **FR-008**: El contacto del almacén MUST ser **institucional** (no datos personales de un
  individuo); el sistema NO almacena PII de elementos ni del responsable más allá de su `user_id`.

### Key Entities

- **Almacén** (`warehouse`): **entidad operativa** (no catálogo). Atributos:
  - *Identidad*: `codigo` (clave de negocio única, estable), `nombre` (único, normalizado).
  - *Clasificación*: `tipo_item_id` (FK → `catalog_item` del catálogo `TIPO_ALMACEN`, spec **006**).
  - *Ubicación*: `municipio` y `estado` (**texto libre**, opcionales; ya no FK a catálogo),
    `direccion`, `cp`, `latitud`/`longitud` (sensibles, opcionales).
  - *Responsable y contacto*: `responsable_id` (FK → `user`), `telefono`, `email_contacto`
    (institucionales, opcionales).
  - *Operativo*: `capacidad` (cupo), `observaciones`.
  - *Estado*: `active` (+ `createdAt`/`updatedAt`).
  - Se relaciona con fornituras (**001**), traslados (**007**), el catálogo `TIPO_ALMACEN` (**006**)
    y `user`. Detalle en [`data-model.md`](./data-model.md).

## Success Criteria *(mandatory)*

- **SC-001**: Un usuario crea o edita un almacén en menos de 1 minuto.
- **SC-002**: El 100% de intentos de eliminar un almacén en uso son bloqueados.
- **SC-003**: El listado de almacenes carga paginado en menos de 2 segundos.

## Assumptions

- **Capacidad**: en MVP el cupo (`capacidad`) es informativo y se compara contra la ocupación
  derivada; exceder el cupo **advierte**, no bloquea. El bloqueo duro se evalúa como mejora.
- **Municipio/estado**: se capturan como **texto libre** (campos `municipio`/`estado`, opcionales).
  Decisión 2026-06-30: no se modelan como catálogo ni como FK geográfica, para simplificar la captura
  (mismo criterio en la spec **003** para el elemento).
- **Tipo de almacén**: deja de ser `enum` en código; es el catálogo `TIPO_ALMACEN` (spec **006**),
  administrable. Los valores actuales (CENTRAL/REGIONAL/MOVIL/TEMPORAL) se cargan como semilla.
- **Geolocalización** (`latitud`/`longitud`): opcional y desaconsejada salvo necesidad operativa;
  por ser ubicación de una armería, se trata como dato sensible.

## Dependencies

- Constitución (Principios I, IV, V).
- Features: **001-inventario-equipos**, **006-tipos-fornitura** (catálogo `TIPO_ALMACEN`),
  **007-traslados**, **011-reportes**, **012-auditoria**.
- Entidad `user` (responsable) — **implementada**.
- Modelo de datos: [`data-model.md`](./data-model.md) y [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
