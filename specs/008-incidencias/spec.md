# Feature Specification: Incidencias y mantenimiento

**Feature Branch**: `008-incidencias`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §4 (Incidencias) — tabla con filtro de estado; columnas
fornitura, problema, fecha reportada, estado, acción "Actualizar". Integra las alertas de
vencimiento/mantenimiento de `Paleta de colores.MD`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar incidencias (Priority: P1)

Al cargar, el usuario ve en el header un filtro por **estado** y una tabla paginada de
incidencias con columnas: fornitura, problema, fecha reportada, estado y acción "Actualizar".

**Why this priority**: Es la vista principal de seguimiento de problemas del equipo.

**Independent Test**: Con incidencias existentes, la tabla las muestra paginadas; el filtro por
estado acota correctamente.

**Acceptance Scenarios**:

1. **Given** incidencias registradas, **When** el usuario abre la pantalla, **Then** las ve
   paginadas con fornitura, problema, fecha y estado.
2. **Given** el filtro de estado, **When** selecciona "abierta", **Then** obtiene solo las
   incidencias abiertas.

---

### User Story 2 - Reportar una incidencia sobre una fornitura (Priority: P1)

Un usuario reporta un problema sobre una fornitura (daño, falla, extravío, necesidad de
mantenimiento), describiéndolo; la fornitura puede pasar a "en mantenimiento" o "extraviada"
según el caso.

**Why this priority**: Sin reportar no hay nada que dar seguimiento; afecta la disponibilidad
real del equipo.

**Independent Test**: Reportar una incidencia de daño sobre una fornitura disponible; la
incidencia queda "abierta" y, si aplica, la fornitura pasa a "en mantenimiento".

**Acceptance Scenarios**:

1. **Given** una fornitura, **When** el usuario reporta una incidencia con descripción del
   problema, **Then** se crea en estado "abierta" con fecha y queda auditada.
2. **Given** una incidencia que implica retiro de servicio, **When** se registra, **Then** la
   fornitura cambia a "en mantenimiento" o "extraviada" según corresponda.

---

### User Story 3 - Actualizar/cerrar una incidencia (Priority: P2)

Un usuario actualiza el estado de una incidencia (en proceso, resuelta, cerrada) y, al
resolverla, la fornitura puede volver a "disponible".

**Acceptance Scenarios**:

1. **Given** una incidencia abierta, **When** se actualiza a "resuelta", **Then** se registra el
   cambio y, si aplica, la fornitura vuelve a "disponible".
2. **Given** cualquier actualización, **When** se guarda, **Then** queda auditada (actor, fecha).

---

### User Story 4 - Alertas inteligentes de vencimiento y mantenimiento (Priority: P2)

El sistema genera alertas automáticas: **preventiva** cuando falten **≤ 90 días** para el
vencimiento (color naranja) y **crítica** cuando la fecha de vencimiento se superó (color rojo),
además de alertas para inspecciones periódicas, daños reportados y sustituciones programadas
(`Paleta de colores.MD` §Alertas Inteligentes).

**Independent Test**: Una fornitura con vencimiento dentro de 30 días aparece en la lista de
alertas preventivas; una vencida aparece en alertas críticas.

**Acceptance Scenarios**:

1. **Given** una fornitura con `fecha_vencimiento` ≤ 90 días, **When** se evalúan las alertas,
   **Then** aparece como **próxima a vencer** (preventiva, naranja).
2. **Given** una fornitura con `fecha_vencimiento` superada, **When** se evalúan las alertas,
   **Then** aparece como **caducada** (crítica, rojo).

### Edge Cases

- Incidencia sobre una fornitura ya asignada: ¿se notifica/retira al elemento? (definir en plan).
- Múltiples incidencias abiertas para la misma fornitura.
- Las alertas de vencimiento son **derivadas** (no requieren incidencia manual); conviven con
  incidencias reportadas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST listar incidencias con paginación y filtro por estado, mostrando
  fornitura, problema, fecha reportada, estado y acción de actualización.
- **FR-002**: El sistema MUST permitir reportar una incidencia sobre una fornitura con
  descripción del problema y tipo (daño, falla, extravío, mantenimiento).
- **FR-003**: El sistema MUST permitir actualizar el estado de una incidencia (abierta → en
  proceso → resuelta/cerrada) y reflejar el cambio de estado de la fornitura cuando aplique.
- **FR-004**: El sistema MUST derivar alertas de vigencia: **preventiva** (≤ 90 días, "próxima a
  vencer") y **crítica** ("caducada"), sin intervención manual.
- **FR-005**: El sistema MUST permitir generar alertas de mantenimiento (inspecciones
  periódicas, daños, sustituciones programadas).
- **FR-006**: Toda incidencia y actualización MUST requerir autorización por rol y quedar
  auditada.
- **FR-007**: Los estados e indicadores MUST usar el color semántico de
  [`docs/05-ui-ux.md`](../../docs/05-ui-ux.md) (mantenimiento=amarillo, próximo a vencer=naranja,
  caducado=rojo, extraviado/baja=gris).

### Key Entities

- **Incidencia** (`incident`): fornitura, tipo, descripción del problema, estado, fecha
  reportada, fecha de resolución, reportado_por, actualizado_por.
- **Alerta** (derivada o materializada): vigencia (próxima a vencer / caducada) y mantenimiento.
- **Fornitura** (**001**).

## Success Criteria *(mandatory)*

- **SC-001**: El 100% de fornituras con vencimiento ≤ 90 días aparecen como "próximas a vencer".
- **SC-002**: El 100% de fornituras vencidas aparecen como "caducadas".
- **SC-003**: Toda incidencia y su seguimiento quedan auditados.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos**, **011-reportes**, **012-auditoria**.
- UI/UX (color de estados): [`docs/05-ui-ux.md`](../../docs/05-ui-ux.md).
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
