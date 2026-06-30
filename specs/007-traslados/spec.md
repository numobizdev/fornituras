# Feature Specification: Traslados entre almacenes

**Feature Branch**: `007-traslados`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §5 (Traslados) — mover fornituras de un almacén a otro, con
captura por QR (lector/cámara/manual) y registro enviado/recibido.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar traslados (Priority: P2)

Al cargar, el usuario ve los traslados realizados con **paginación** y filtros, y un botón
"Nuevo traslado". La tabla muestra origen, destino, estado, fecha de envío, fecha de recepción
y acciones.

**Why this priority**: Da visibilidad del movimiento de equipo entre bodegas; consulta frecuente
de control.

**Independent Test**: Con traslados existentes, la lista los muestra paginados con sus estados;
los filtros acotan por origen/destino/estado.

**Acceptance Scenarios**:

1. **Given** traslados registrados, **When** el usuario abre la pantalla, **Then** los ve
   paginados con origen, destino, estado y fechas.
2. **Given** filtros aplicados, **When** filtra por estado o almacén, **Then** obtiene solo los
   traslados que cumplen.

---

### User Story 2 - Registrar un nuevo traslado (Priority: P1)

El usuario crea un traslado seleccionando **almacén origen** y **almacén destino**; luego agrega
las fornituras escaneando su QR (lector/cámara/manual) mediante un campo de búsqueda con botón
"Agregar", que las acumula en una tabla de fornituras a trasladar. Al confirmar, el traslado
queda **enviado**.

**Why this priority**: Es la acción central de la pantalla y cambia el estado de las fornituras.

**Independent Test**: Crear un traslado origen→destino con 2 fornituras; ambas pasan a "en
traslado" y el traslado queda en estado "enviado"; al recibirlo en destino, pasan a
"disponible" en el almacén destino.

**Acceptance Scenarios**:

1. **Given** origen y destino seleccionados y fornituras disponibles agregadas, **When** el
   usuario confirma, **Then** se crea el traslado en estado "enviado", las fornituras pasan a
   "en traslado" y se registra fecha de envío.
2. **Given** una fornitura **no disponible** o **asignada**, **When** se intenta agregar al
   traslado, **Then** el sistema lo bloquea indicando el motivo.
3. **Given** una fornitura cuyo almacén actual no es el origen seleccionado, **When** se intenta
   agregar, **Then** el sistema lo advierte/bloquea.

---

### User Story 3 - Recibir un traslado (Priority: P2)

En el almacén destino, un usuario confirma la recepción del traslado; las fornituras quedan en
"disponible" bajo el almacén destino y se registra la fecha de recepción.

**Why this priority**: Cierra el ciclo del movimiento; sin recepción el equipo queda "en
tránsito" indefinido.

**Acceptance Scenarios**:

1. **Given** un traslado "enviado", **When** el destino confirma la recepción, **Then** pasa a
   "recibido", las fornituras quedan "disponibles" en el almacén destino y se registra la fecha.
2. **Given** un traslado "enviado", **When** transcurre sin recepción, **Then** puede señalarse
   como pendiente (alerta de control).

### Edge Cases

- Recepción parcial: ¿se reciben solo algunas fornituras del traslado? (decisión de diseño —
  ver Assumptions).
- Cancelación de un traslado enviado: revertir fornituras a "disponible" en el origen, auditado.
- Concurrencia con una asignación: una fornitura "en traslado" no puede asignarse.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST listar traslados con paginación y filtros (origen, destino,
  estado).
- **FR-002**: El sistema MUST permitir crear un traslado con almacén **origen** y **destino**, y
  agregar fornituras por código QR (lector/cámara/manual) acumulándolas antes de confirmar.
- **FR-003**: El sistema MUST permitir agregar solo fornituras **disponibles** ubicadas en el
  almacén origen; bloquea el resto indicando el motivo.
- **FR-004**: Al confirmar, las fornituras MUST pasar a estado **"en traslado"** y el traslado
  a **"enviado"**, registrando fecha de envío.
- **FR-005**: Al recibir, las fornituras MUST pasar a **"disponible"** en el almacén destino y
  el traslado a **"recibido"**, registrando fecha de recepción.
- **FR-006**: Una fornitura **"en traslado"** MUST NOT poder asignarse ni darse de baja hasta su
  recepción o cancelación.
- **FR-007**: Toda creación, recepción o cancelación de traslado MUST quedar auditada y
  requerir autorización por rol.

### Key Entities

- **Traslado** (`transfer`): origen, destino, estado (enviado/recibido/cancelado), fecha de
  envío, fecha de recepción, creado_por, recibido_por.
- **Renglón de traslado** (`transfer_item`): traslado ↔ fornitura.
- **Fornitura** (**001**), **Almacén** (**005**).

## Success Criteria *(mandatory)*

- **SC-001**: Un usuario crea un traslado de 10 fornituras en menos de 3 minutos.
- **SC-002**: El 100% de fornituras "en traslado" quedan bloqueadas para asignación/baja.
- **SC-003**: El 100% de traslados conservan trazabilidad de envío y recepción.

## Assumptions

- Recepción parcial: por defecto el traslado se recibe completo; la recepción parcial se evalúa
  como mejora posterior (decisión de plan/ADR).
- El código QR llega capturado por **014-escaneo-qr**.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos**, **005-almacenes**, **014-escaneo-qr**, **012-auditoria**.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
