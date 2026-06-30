# Feature Specification: Tablero de control (Dashboard)

**Feature Branch**: `010-dashboard`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Paleta de colores.MD` §Dashboard Ejecutivo + pantalla `Inicio` ya andamiada en
`sigefor/` (`/inicio`). Indicadores clave del estado del inventario.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver indicadores clave al entrar (Priority: P1)

Al abrir la aplicación, el usuario autenticado ve un tablero con los indicadores principales:
total de fornituras, disponibles, asignadas, próximas a vencer, caducadas y en mantenimiento,
cada uno con su **color semántico** institucional.

**Why this priority**: Es la pantalla de entrada (`/inicio`) y da la foto operativa inmediata.

**Independent Test**: Con datos cargados, los contadores coinciden con el inventario real y cada
indicador usa el color definido en [`docs/05-ui-ux.md`](../../docs/05-ui-ux.md).

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** abre `/inicio`, **Then** ve los contadores (total,
   disponibles, asignadas, próximas a vencer, caducadas, en mantenimiento) con su color
   semántico.
2. **Given** los datos cambian (alta, asignación, baja), **When** se recarga el tablero, **Then**
   los contadores reflejan el estado actual.
3. **Given** el rol del usuario, **When** ve el tablero, **Then** solo se muestran los
   indicadores que su rol puede consultar (sin filtrar PII).

### Edge Cases

- Inventario vacío: los contadores muestran cero sin error.
- Conteos costosos sobre decenas de miles de fornituras: deben resolverse con consultas
  agregadas eficientes (no traer todos los registros al cliente).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El tablero MUST mostrar al menos: total de fornituras, disponibles, asignadas,
  próximas a vencer, caducadas y en mantenimiento.
- **FR-002**: Cada indicador MUST usar el **color semántico** institucional definido en
  [`docs/05-ui-ux.md`](../../docs/05-ui-ux.md) (guinda/total, verde/disponible, azul/asignado,
  naranja/próximo a vencer, rojo/caducado, amarillo/mantenimiento).
- **FR-003**: Los contadores MUST calcularse con **consultas agregadas** del lado servidor, sin
  transferir el inventario completo al cliente.
- **FR-004**: El tablero MUST requerir autenticación y respetar el rol (mínimo privilegio); no
  expone PII.
- **FR-005**: Los indicadores de "próxima a vencer" y "caducada" MUST derivarse de
  `fecha_vencimiento` con el mismo criterio que **001**/**008** (≤ 90 días / vencida).

### Key Entities

- No introduce entidades nuevas; consume agregados de **Fornitura** (**001**), **Asignación**
  (**004**) e **Incidencia** (**008**).

## Success Criteria *(mandatory)*

- **SC-001**: El tablero carga en menos de 2 segundos con decenas de miles de fornituras.
- **SC-002**: Los contadores coinciden exactamente con los listados filtrados equivalentes.
- **SC-003**: Cero PII expuesta en el tablero.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001-inventario-equipos**, **004-asignacion-resguardos**, **008-incidencias**,
  **011-reportes**.
- UI/UX (paleta y colores de estado): [`docs/05-ui-ux.md`](../../docs/05-ui-ux.md).
