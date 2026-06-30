# Feature Specification: Reportes y estadística

**Feature Branch**: `011-reportes`

**Created**: 2026-06-30

**Status**: Draft

**Input**: `Requerimientos.MD` §7 (Reportes) — totales por estado, tabla de asignaciones activas
con filtros, exportación a Excel. Complementa reportes operativos/de control de
`Paleta de colores.MD` §Reportes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver totales y asignaciones activas (Priority: P1)

El usuario ve un total de fornituras desglosado por estado (disponibles, asignadas, con
incidencia, baja) y conteo de elementos, y debajo una tabla **paginada** de asignaciones activas
con filtros (QR de fornitura, nombre, RFC, placa, CURP y municipio).

**Why this priority**: Es la vista de control consolidada del responsable del inventario.

**Independent Test**: Con datos cargados, los totales coinciden con el inventario; filtrar las
asignaciones activas por municipio devuelve solo las de ese municipio, paginadas.

**Acceptance Scenarios**:

1. **Given** datos cargados, **When** el usuario abre Reportes, **Then** ve los totales por
   estado y la tabla de asignaciones activas paginada.
2. **Given** filtros (QR, nombre, RFC, placa, CURP, municipio), **When** filtra, **Then** la
   tabla muestra solo las asignaciones que cumplen, respetando el enmascaramiento de PII por rol.

---

### User Story 2 - Exportar a Excel (Priority: P1)

El usuario exporta el reporte vigente (con sus filtros) a un archivo Excel para uso fuera del
sistema.

**Why this priority**: Requisito explícito; los reportes se comparten con mandos y control
interno.

**Independent Test**: Aplicar un filtro y exportar; el Excel contiene exactamente las filas del
filtro y respeta el enmascaramiento de PII según el rol del exportador.

**Acceptance Scenarios**:

1. **Given** un reporte filtrado, **When** el usuario exporta a Excel, **Then** el archivo
   contiene las mismas filas visibles y respeta la autorización de PII.
2. **Given** un rol sin permiso de ver PII completa, **When** exporta, **Then** el archivo sale
   con los campos sensibles enmascarados.

---

### User Story 3 - Reportes operativos y de control (Priority: P2)

El usuario genera reportes predefinidos: inventario general / por unidad / por región, fornituras
asignadas / disponibles / en mantenimiento, próximos a vencer, caducados, historial de
movimientos y auditoría (`Paleta de colores.MD` §Reportes).

**Acceptance Scenarios**:

1. **Given** un tipo de reporte seleccionado, **When** se genera, **Then** entrega los datos
   correctos paginados y exportables.

### Edge Cases

- Exportación de grandes volúmenes: generar en streaming/lotes para no agotar memoria.
- La exportación es un evento sensible (extrae datos): debe **auditarse** (quién exportó qué).
- PII en Excel: el archivo hereda la sensibilidad; advertir y registrar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mostrar totales de fornituras por estado (disponibles, asignadas,
  con incidencia, baja) y conteo de elementos.
- **FR-002**: El sistema MUST listar **asignaciones activas** con paginación y filtros: QR de
  fornitura, nombre, RFC, placa, CURP y municipio.
- **FR-003**: El sistema MUST permitir **exportar a Excel** el reporte vigente con sus filtros,
  respetando el enmascaramiento de PII según rol.
- **FR-004**: El sistema MUST ofrecer reportes operativos y de control predefinidos (inventario
  general/por unidad/por región, asignados/disponibles/mantenimiento, próximos a vencer,
  caducados, historial de movimientos, auditoría).
- **FR-005**: Toda **exportación** MUST quedar auditada (quién, qué reporte/filtros, cuándo) sin
  escribir PII en el log (Principio V).
- **FR-006**: Los reportes MUST requerir autorización por rol; los datos PII se muestran/exportan
  solo a roles autorizados.

### Key Entities

- No introduce entidades nuevas; consume agregados y vistas de **Fornitura** (**001**),
  **Asignación** (**004**), **Elemento** (**003**), **Incidencia** (**008**) y **Baja** (**009**).

## Success Criteria *(mandatory)*

- **SC-001**: Los totales del reporte coinciden con el inventario real y con el tablero (**010**).
- **SC-002**: La exportación de 10.000 filas se completa sin agotar memoria y en tiempo
  razonable.
- **SC-003**: El 100% de exportaciones quedan auditadas.
- **SC-004**: Ningún rol sin autorización exporta PII en claro.

## Dependencies

- Constitución (Principios IV, V).
- Features: **001**, **003**, **004**, **008**, **009**, **010-dashboard**, **012-auditoria**.
- Decisión de PII (**003** / ADR `0003-pii-elementos`) aplica al contenido exportable.
- Modelo de datos: [`docs/03-modelo-datos.md`](../../docs/03-modelo-datos.md).
