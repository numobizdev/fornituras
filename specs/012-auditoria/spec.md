# Feature Specification: Bitácora de auditoría (ISO 27001)

**Feature Branch**: `012-auditoria`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §9 (Auditoría) — log de transacciones con fecha y acción, tomando
como referencia ISO/IEC 27001. Operacionaliza el Principio V de la Constitución y
`Paleta de colores.MD` §Auditoría.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar la bitácora de auditoría (Priority: P1)

Un usuario autorizado (auditor/admin) consulta, con **paginación** y filtros (usuario, fecha,
acción, entidad), el registro de transacciones del sistema: quién hizo qué, cuándo, sobre qué
registro, desde qué IP, con evidencia del cambio.

**Why this priority**: Es la base de responsabilidad y cumplimiento; requisito transversal de
todas las features.

**Independent Test**: Tras realizar una asignación, la bitácora muestra el evento con actor,
acción, entidad, fecha/hora e IP, sin exponer PII en claro.

**Acceptance Scenarios**:

1. **Given** acciones realizadas en el sistema, **When** el auditor consulta la bitácora, **Then**
   ve los eventos paginados con usuario, fecha, hora, acción, registro afectado, IP y evidencia.
2. **Given** filtros (usuario/fecha/acción/entidad), **When** filtra, **Then** obtiene solo los
   eventos que cumplen.
3. **Given** un evento sobre un elemento (PII), **When** se consulta, **Then** el registro
   referencia la entidad por **id**, sin volcar PII en el log (Principio V).

---

### User Story 2 - Registro automático de eventos sensibles (Priority: P1)

El sistema registra automáticamente, sin intervención del usuario, los eventos sensibles:
login/logout, acceso a ficha de elemento, alta/edición/baja de fornituras, asignación/devolución,
traslados, incidencias, exportaciones y cambios de usuarios/roles.

**Why this priority**: La auditoría solo sirve si es completa y no evitable; es el corazón del
control ISO 27001.

**Independent Test**: Ejecutar cada operación sensible y verificar que genera exactamente un
registro de auditoría con los campos requeridos.

**Acceptance Scenarios**:

1. **Given** cualquier operación sensible, **When** se ejecuta, **Then** se crea un registro de
   auditoría con actor, acción, entidad, entidad_id, timestamp, IP y evidencia/diff.
2. **Given** un intento de operación denegada por autorización, **When** ocurre, **Then** también
   queda registrado (intento fallido).

### Edge Cases

- Integridad: la bitácora debe ser de **difícil alteración** (append-only / encadenamiento o
  almacenamiento protegido).
- Retención: política de conservación de logs conforme al marco legal y ISO 27001.
- Volumen: la consulta debe ser eficiente (índices, paginación) sobre millones de eventos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST registrar automáticamente los eventos sensibles (login/logout,
  acceso a PII, alta/edición/baja, asignación/devolución, traslado, incidencia, exportación,
  cambios de usuarios/roles) con: usuario, acción, entidad, entidad_id, fecha/hora, IP y
  evidencia del cambio.
- **FR-002**: La bitácora MUST NOT contener PII ni secretos en claro; referencia entidades por
  id y enmascara/redacta valores sensibles (Principio V).
- **FR-003**: La bitácora MUST ser **inmutable o de difícil alteración** (append-only; sin
  edición/borrado por usuarios de aplicación).
- **FR-004**: El sistema MUST permitir consultar la bitácora con paginación y filtros (usuario,
  fecha, acción, entidad), restringido a roles de auditoría/administración.
- **FR-005**: La política de **retención** de la bitácora MUST documentarse (ADR), alineada al
  marco legal y a ISO/IEC 27001.
- **FR-006**: Los intentos de acceso **denegados** MUST registrarse igual que los exitosos.

### Key Entities

- **Registro de auditoría** (`audit_log`): usuario, acción, entidad, entidad_id, timestamp, IP,
  evidencia/diff. Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).

## Success Criteria *(mandatory)*

- **SC-001**: El 100% de las operaciones sensibles generan exactamente un registro de auditoría.
- **SC-002**: Cero PII/secretos en claro en la bitácora.
- **SC-003**: Ningún usuario de aplicación puede editar o borrar registros de auditoría.
- **SC-004**: La consulta filtrada responde paginada en menos de 2 segundos sobre millones de
  eventos.

## Assumptions

- El mecanismo de inmutabilidad concreto (append-only en BD, tabla protegida, WORM, hashing
  encadenado) se decide por ADR.
- La referencia ISO/IEC 27001 guía controles de registro y trazabilidad (A.12, A.8, A.5).

## Dependencies

- Constitución (Principio V, y I para no filtrar PII); `docs/02-seguridad.md` §5.
- Transversal a todas las features.
- ADR de retención/inmutabilidad: `docs/04-decisiones/`.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
