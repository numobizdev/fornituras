# Feature Specification: Asignación de fornituras y resguardos

**Feature Branch**: `004-asignacion-resguardos`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §1 (Captura y asignación) — flujo de 2 pasos: identificar
fornitura por QR (lector/cámara/manual) → buscar elemento → asignar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver asignaciones vigentes (Priority: P1)

Al cargar la pantalla, el usuario ve **quién tiene asignada cada fornitura** (asignaciones
vigentes), con paginación y un botón para iniciar una nueva asignación.

**Why this priority**: Es lo que la pantalla muestra al entrar; da contexto operativo inmediato
(qué está entregado y a quién).

**Independent Test**: Con asignaciones existentes, la pantalla las lista paginadas mostrando
fornitura y elemento; el botón "Asignar" abre el flujo de captura.

**Acceptance Scenarios**:

1. **Given** asignaciones vigentes, **When** el usuario abre la pantalla, **Then** ve la lista
   paginada de fornituras asignadas con su elemento resguardatario.
2. **Given** un usuario sin permiso de asignar, **When** abre la pantalla, **Then** puede
   consultar pero no ve el botón de nueva asignación.

---

### User Story 2 - Asignar una fornitura a un elemento (Priority: P1)

El usuario ejecuta el flujo en dos pasos:
**Paso 1 — Identificar la fornitura:** captura el código QR (el sistema **detecta
automáticamente** si proviene de lector HID, cámara o tecleo manual) y pulsa "Buscar"; el
sistema muestra la descripción de la fornitura y si está **disponible**.
**Paso 2 — Identificar el elemento:** busca por nombre, placa, CURP o RFC; al encontrarlo,
muestra nombre, descripción y **foto** del elemento.
Con ambos pasos completos, dispone de **"Asignar fornitura"** y **"Limpiar"**.

**Why this priority**: Es el propósito central de SIGEFOR: ligar equipo↔elemento con
trazabilidad.

**Independent Test**: Escanear una fornitura disponible, buscar un elemento, asignar; la
fornitura pasa a estado "asignada" y aparece en las asignaciones vigentes con ese elemento.

**Acceptance Scenarios**:

1. **Given** una fornitura **disponible** identificada por QR y un elemento encontrado, **When**
   el usuario pulsa "Asignar fornitura", **Then** se crea la asignación, la fornitura pasa a
   "asignada" y se genera el resguardo.
2. **Given** una fornitura **no disponible** (asignada, en mantenimiento, baja, en traslado),
   **When** se identifica en el paso 1, **Then** el sistema lo indica y **no** permite asignar.
3. **Given** un QR con firma inválida o desconocido, **When** se intenta identificar, **Then**
   el sistema lo rechaza sin revelar información (delega verificación en **002**/**014**).
4. **Given** un flujo a medio capturar, **When** el usuario pulsa "Limpiar", **Then** se
   reinician ambos pasos sin crear nada.

---

### User Story 3 - Reasignar y registrar entrega/recepción (Priority: P2)

El usuario reasigna una fornitura de un elemento a otro, registrando la devolución del anterior
y la nueva entrega; el sistema conserva el **historial de movimientos** y puede capturar firma
electrónica y generar el resguardo.

**Why this priority**: Refleja la rotación real del equipo; depende de poder asignar (US2).

**Independent Test**: Reasignar una fornitura asignada al elemento A hacia el elemento B;
la asignación de A se cierra (fecha de devolución) y se abre una vigente para B; el historial
conserva ambas.

**Acceptance Scenarios**:

1. **Given** una fornitura asignada al elemento A, **When** se reasigna al elemento B, **Then**
   la asignación de A se cierra y la de B queda vigente, conservando el historial.
2. **Given** una asignación, **When** se registra la recepción/devolución, **Then** la
   fornitura vuelve a "disponible" (o al estado que corresponda) y queda auditado.

### Edge Cases

- Detección automática del origen del QR (lector HID emula teclado; cámara; tecleo manual): la
  UX la resuelve **014-escaneo-qr**; aquí se asume un código ya obtenido.
- Concurrencia: dos usuarios intentan asignar la misma fornitura disponible a la vez → solo una
  asignación gana; la otra recibe "ya no está disponible".
- Elemento dado de baja del cuerpo con fornitura asignada: requiere resolver la devolución antes
  de su baja.
- Firma electrónica: si no está disponible en el dispositivo, el resguardo se genera sin firma
  pero el evento queda auditado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La pantalla MUST mostrar al cargar las **asignaciones vigentes** (fornitura →
  elemento) con paginación.
- **FR-002**: El flujo de asignación MUST ser de dos pasos: (1) identificar fornitura por código
  QR y mostrar su descripción y disponibilidad; (2) buscar elemento por nombre, placa, CURP o
  RFC y mostrar nombre, descripción y foto.
- **FR-003**: El sistema MUST permitir asignar solo fornituras en estado **disponible** y MUST
  bloquear el resto indicando el motivo.
- **FR-004**: Al asignar, el sistema MUST crear la asignación, cambiar la fornitura a "asignada"
  y generar el **resguardo** correspondiente.
- **FR-005**: El sistema MUST permitir **reasignar** cerrando la asignación previa (fecha de
  devolución) y abriendo la nueva, conservando el **historial** completo de movimientos.
- **FR-006**: El sistema MUST registrar **entrega y recepción** y, cuando esté disponible,
  **firma electrónica**; MUST poder **generar el resguardo** automáticamente.
- **FR-007**: La resolución `QR → fornitura` MUST verificar firma y autorización en el servidor
  antes de mostrar cualquier dato (Principio II; ver **002**/**014**).
- **FR-008**: La búsqueda de elemento MUST respetar la autorización de PII (mostrar foto/datos
  solo a roles autorizados) y auditar el acceso (Principio V).
- **FR-009**: Toda asignación, reasignación y devolución MUST quedar auditada (actor, fornitura,
  elemento, fecha) sin escribir PII en el log.
- **FR-010**: El botón **"Limpiar"** MUST reiniciar el flujo sin persistir nada.

### Key Entities *(include if feature involves data)*

- **Asignación** (`assignment`): relación fornitura↔elemento en el tiempo. Atributos: fornitura,
  elemento, fecha de asignación, fecha de devolución (null si vigente), asignado_por (usuario),
  recibido_por, firma. Conserva historial. Ver [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
- **Resguardo**: documento generado a partir de una asignación (entrega/recepción).
- **Fornitura** (**001**) y **Elemento** (**003**).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario asigna una fornitura disponible a un elemento en menos de 1 minuto.
- **SC-002**: El 100% de los intentos de asignar una fornitura no disponible son bloqueados.
- **SC-003**: El 100% de las asignaciones/reasignaciones quedan en el historial y auditadas.
- **SC-004**: Cero colisiones: una fornitura nunca queda con dos asignaciones vigentes.

## Assumptions

- El código QR llega ya capturado por **014-escaneo-qr**; su firma se verifica vía **002**.
- La firma electrónica y el formato del resguardo (PDF/plantilla) se detallan en plan/ADR.
- Roles que pueden asignar: ADMIN, SUPERVISOR, ALMACEN (ver **013-usuarios**).

## Dependencies

- Constitución (Principios II, IV, V); `docs/02-seguridad.md`.
- Features: **001-inventario-equipos**, **002-qr-equipos**, **003-elementos-padron**,
  **014-escaneo-qr**, **012-auditoria**.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
