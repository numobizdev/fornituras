# Quickstart / Validación — Reportes y estadística (011)

Resultados de la validación de los escenarios de `spec.md`/`plan.md`. La feature no introduce
entidades: consume agregados y vistas de 001/003/004/008 y reutiliza el enmascaramiento de PII de
003. La exportación usa Apache POI SXSSF en streaming ([ADR 0011](../../docs/04-decisiones/0011-libreria-export-excel.md)).

## Contrato

- `GET /api/v1/reports/totals` → totales por estado + conteo de elementos (coinciden con 010).
- `GET /api/v1/reports/active-assignments` → asignaciones vigentes, paginadas y filtrables
  (QR, nombre, RFC, placa, CURP, municipio); CURP/RFC enmascaradas salvo ADMIN.
- `GET /api/v1/reports/active-assignments/export` → `.xlsx` en streaming, auditado.
- `GET /api/v1/reports/predefined/{tipo}` (+ `/export`) → reportes operativos por estado.

## Escenarios validados

| Escenario | Cómo se valida | Estado |
|-----------|----------------|--------|
| Totales coinciden con el tablero 010 (SC-001) | `ReportConsistencyTest.totals_matchDashboardSummary` | ✅ |
| Asignaciones activas paginadas y filtradas (FR-002) | `ReportQueryContractTest` (municipio, placa, paginación) | ✅ |
| Filtro por nombre (dato cifrado) | En memoria sobre el nombre descifrado del conjunto ya filtrado en BD | ✅ |
| PII enmascarada por rol en pantalla (FR-006) | `ReportConsistencyTest` (ADMIN ve CURP en claro; CAPTURISTA la ve enmascarada) | ✅ |
| El Excel contiene exactamente las filas del filtro y respeta el enmascaramiento (FR-003/FR-006) | `ExcelExportTest` (lee el `.xlsx` con POI) | ✅ |
| Cada exportación se audita sin PII (FR-005/SC-003) | `ExportAuditTest` (evento `EXPORT_REPORT`; el detalle no contiene el valor de la CURP) | ✅ |
| Exportar 10.000 filas sin agotar memoria (SC-002) | `ExportVolumeTest` (SXSSF, ventana de filas + disco temporal) | ✅ |
| Reportes predefinidos por tipo, paginados y exportables (FR-004) | `PredefinedReportsTest` | ✅ |

## Seguridad (endurecimiento, T024/T025)

- **PII por rol** en pantalla y en el `.xlsx` (solo ADMIN ve CURP/RFC en claro; SC-004).
- **Auditoría de exportación** con actor, tipo de reporte y **nombres** de los filtros usados; nunca
  los valores (sin PII en el log).
- **Sin PII en nombres de archivo** (`asignaciones-activas.xlsx`, `reporte-<tipo>.xlsx`).
- La UI **advierte** que el archivo hereda la sensibilidad de los datos exportados.

## Alcance / decisiones

- Los reportes de **vigencia** (próximos a vencer / caducados) se sirven desde las alertas de 008 y
  el tablero de 010 (no se duplican aquí). **Movimientos** (007) y **auditoría** (012) se integrarán
  como reportes predefinidos cuando esas vistas estén disponibles.
- El nombre no se filtra en SQL por estar **cifrado de forma no determinista** (ADR 0006); se filtra
  en memoria sobre el conjunto de asignaciones **vigentes** (acotado).
