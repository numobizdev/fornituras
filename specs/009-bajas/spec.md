# Feature Specification: Bajas definitivas de fornituras

**Feature Branch**: `009-bajas`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §6 (Bajas) — dar de baja una fornitura buscándola por QR y listar
las dadas de baja.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dar de baja una fornitura (Priority: P1)

Un usuario autorizado busca una fornitura por su código QR/número, registra el motivo y la da de
**baja definitiva**. La fornitura deja de ser asignable pero conserva su historial.

**Why this priority**: Cierra el ciclo de vida del equipo (adquisición → baja) y mantiene la
exactitud del inventario operativo.

**Independent Test**: Dar de baja una fornitura disponible por QR con un motivo; pasa a "baja
definitiva", ya no aparece como asignable y queda en la lista de bajas con su motivo.

**Acceptance Scenarios**:

1. **Given** una fornitura sin asignación vigente, **When** el usuario la busca por QR y confirma
   la baja con motivo, **Then** pasa a **"baja definitiva"**, deja de ser asignable y conserva
   su historial.
2. **Given** una fornitura **con asignación vigente**, **When** se intenta dar de baja, **Then**
   el sistema lo impide hasta resolver la devolución (consistente con **001** FR-008).
3. **Given** una fornitura ya dada de baja, **When** se intenta operar sobre ella
   (asignar/trasladar), **Then** el sistema lo rechaza.

---

### User Story 2 - Consultar fornituras dadas de baja (Priority: P2)

El usuario consulta, con paginación, la tabla de fornituras dadas de baja con información
relevante (QR, descripción, tipo, motivo, fecha de baja, quién la dio de baja).

**Acceptance Scenarios**:

1. **Given** fornituras dadas de baja, **When** el usuario abre la pantalla, **Then** las ve
   paginadas con su motivo y fecha.
2. **Given** filtros, **When** filtra por fecha o tipo, **Then** obtiene solo las que cumplen.

### Edge Cases

- Baja por caducidad vs por daño/extravío: el motivo es un catálogo.
- ¿Reversión de baja? Por política, una baja definitiva no se revierte; un error se corrige con
  ajuste auditado y justificación (definir en plan).
- Baja masiva por lote: posible mejora (no en MVP).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir buscar una fornitura por código QR/número y darla de baja
  registrando **motivo** (catálogo) y fecha.
- **FR-002**: El sistema MUST impedir la baja de una fornitura con **asignación vigente** o **en
  traslado** hasta resolverla.
- **FR-003**: Una fornitura en **"baja definitiva"** MUST NOT poder asignarse ni trasladarse, y
  MUST conservar su historial completo.
- **FR-004**: El sistema MUST listar, con paginación y filtros, las fornituras dadas de baja con
  información relevante (QR, descripción, tipo, motivo, fecha, responsable).
- **FR-005**: Toda baja MUST requerir autorización por rol y quedar auditada (actor, fornitura,
  motivo, fecha).

### Key Entities

- **Baja** (`decommission`): fornitura, motivo (catálogo), fecha, responsable, observaciones.
- **Motivo de baja**: catálogo (caducidad, daño, extravío, obsolescencia, etc.).
- **Fornitura** (**001**) — estado "baja definitiva".

## Success Criteria *(mandatory)*

- **SC-001**: El 100% de bajas quedan auditadas con motivo y responsable.
- **SC-002**: El 100% de intentos de operar una fornitura dada de baja son rechazados.
- **SC-003**: Ninguna fornitura con asignación vigente puede darse de baja.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos**, **004-asignacion-resguardos**, **007-traslados**,
  **012-auditoria**.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
