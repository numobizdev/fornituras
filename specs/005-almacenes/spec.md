# Feature Specification: Almacenes

**Feature Branch**: `005-almacenes`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §11 (Almacenes) — ubicaciones de resguardo de fornituras.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar almacenes (CRUD) (Priority: P1)

Un usuario autorizado registra, consulta, edita y desactiva almacenes (ubicaciones físicas
donde se resguardan las fornituras). Cada fornitura pertenece a un almacén.

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

- Nombre/identificador de almacén duplicado: rechazar.
- Existencias por almacén: el conteo de fornituras por almacén alimenta reportes (**011**).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir crear, consultar (paginado), editar y **desactivar**
  almacenes.
- **FR-002**: El nombre/identificador de almacén MUST ser único.
- **FR-003**: El sistema MUST impedir eliminar un almacén con fornituras o traslados asociados;
  en su lugar permite desactivarlo.
- **FR-004**: Solo almacenes **activos** MUST ofrecerse como ubicación en alta de fornituras y
  como origen/destino de traslados.
- **FR-005**: Las operaciones de alta/edición/baja de almacén MUST requerir autorización por rol
  y quedar auditadas.

### Key Entities

- **Almacén** (`warehouse`): nombre/identificador (único), ubicación/descripción, estado
  (activo/inactivo). Se relaciona con fornituras (**001**) y traslados (**007**).

## Success Criteria *(mandatory)*

- **SC-001**: Un usuario crea o edita un almacén en menos de 1 minuto.
- **SC-002**: El 100% de intentos de eliminar un almacén en uso son bloqueados.
- **SC-003**: El listado de almacenes carga paginado en menos de 2 segundos.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos**, **007-traslados**, **011-reportes**.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
