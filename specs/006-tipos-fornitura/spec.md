# Feature Specification: Catálogo de tipos de fornitura

**Feature Branch**: `006-tipos-fornitura`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §10 (Tipo de fornituras) — CRUD de tipos con nombre, foto,
descripción.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar tipos de fornitura (CRUD) (Priority: P1)

Un usuario autorizado registra y mantiene el catálogo de tipos de fornitura (p. ej. chaleco
antibala, cinturón táctico, casco), cada uno con nombre, descripción y foto representativa.

**Why this priority**: El tipo es un atributo de catálogo requerido por el alta de fornituras
(**001**); el catálogo debe existir antes.

**Independent Test**: Crear un tipo "Chaleco antibala", verlo en la lista y poder seleccionarlo
al dar de alta una fornitura; un tipo en uso no se elimina (se desactiva).

**Acceptance Scenarios**:

1. **Given** un usuario autorizado, **When** registra un tipo con nombre único y datos válidos,
   **Then** queda disponible para seleccionarse en el alta de fornituras.
2. **Given** un tipo usado por fornituras, **When** se intenta eliminar, **Then** el sistema lo
   impide y ofrece desactivarlo.
3. **Given** un tipo desactivado, **When** se da de alta una fornitura, **Then** no aparece como
   opción seleccionable.

### Edge Cases

- Nombre de tipo duplicado: rechazar.
- Foto del tipo: límites de tamaño/formato; no es PII (imagen genérica del equipo).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir crear, consultar (paginado), editar y **desactivar**
  tipos de fornitura con nombre (único), descripción y foto.
- **FR-002**: El sistema MUST impedir eliminar un tipo en uso; en su lugar permite desactivarlo.
- **FR-003**: Solo tipos **activos** MUST ofrecerse en el alta de fornituras.
- **FR-004**: Las operaciones MUST requerir autorización por rol y quedar auditadas.

### Key Entities

- **Tipo de fornitura** (`equipment_type`): nombre (único), descripción, foto, estado.
- **Talla** (`size`): catálogo asociado; puede modelarse aparte o por tipo (ver Assumptions).

## Success Criteria *(mandatory)*

- **SC-001**: Un usuario crea o edita un tipo en menos de 1 minuto.
- **SC-002**: El 100% de intentos de eliminar un tipo en uso son bloqueados.

## Assumptions

- La **talla** es un catálogo relacionado (también requerido por **001**); puede gestionarse en
  esta misma feature o en su propio catálogo. Recomendación: catálogo de tallas independiente y,
  si aplica, asociado por tipo (un chaleco y un cinturón no comparten tallas).

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos**.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
