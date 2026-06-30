# Implementation Plan: Reportes y estadística

**Branch**: `dev` (feature **011-reportes**) | **Date**: 2026-06-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/011-reportes/spec.md`

## Summary

Construir la **vista de control consolidada**: totales de fornituras por estado, tabla **paginada de
asignaciones activas** con filtros (QR, nombre, RFC, placa, CURP, municipio), y **exportación a Excel**
del reporte vigente — respetando el **enmascaramiento de PII por rol**. Incluye reportes operativos
predefinidos. No introduce entidades: **consume agregados y vistas** de Fornitura (**001**), Asignación
(**004**), Elemento (**003**), Incidencia (**008**) y Baja (**009**).

Dos ejes críticos: (1) **PII** — el reporte y el Excel solo muestran/exportan PII a roles autorizados, y
la **exportación se audita** (extrae datos, Principio V); (2) **escala** — exportar grandes volúmenes en
**streaming/lotes** sin agotar memoria (SC-002).

Enfoque: módulo backend `reports` (consultas agregadas + generación de Excel en streaming), y feature
`reportes` en el frontend `sigefor/`.

## Technical Context

**Language/Version**: Java 25 (backend `fornituras-api/`); TypeScript + Angular/Ionic 8 (`sigefor/`).

**Primary Dependencies**: Spring Boot (Web MVC, Security, Data JPA), `mssql-jdbc`. **Excel**: librería de
generación en streaming (p. ej. Apache POI **SXSSF**) — **introducir dependencia requiere justificar
necesidad/licencia/mantenimiento** (Principio VI) y registrarla. Reutiliza repositorios de 001/003/004/
008/009. Frontend: servicios HTTP + descarga de archivo.

**Storage**: SQL Server 2022. **Sin tablas nuevas**: consultas agregadas y de asignaciones activas
(join `assignment`↔`equipment`↔`officers`). La PII se lee de 003 **enmascarada según rol** (nunca se
descifra para roles no autorizados).

**Testing**: JUnit 5 + Spring Boot Test; Testcontainers (MSSQL); pruebas de que el Excel contiene
exactamente las filas filtradas y respeta el enmascaramiento; pruebas de auditoría de exportación;
prueba de volumen (10.000 filas sin agotar memoria). Frontend: pruebas de servicio + descarga.

**Target Platform**: API REST en contenedor Linux; cliente Ionic.

**Project Type**: Web — monorepo `fornituras-api/` + `sigefor/`.

**Performance Goals**: exportación de 10.000 filas sin agotar memoria y en tiempo razonable (SC-002);
totales que coinciden con el tablero 010 (SC-001).

**Constraints**: PII solo a roles autorizados, en pantalla y en Excel (FR-006, SC-004); **toda
exportación auditada** sin PII en el log (FR-005, SC-003); enmascaramiento heredado de 003; totales
coinciden con 010 (SC-001).

**Scale/Scope**: 1 pantalla (totales + tabla + export) + reportes predefinidos; alto volumen en export.

## Constitution Check

*GATE: debe pasar antes de Phase 0 y re-verificarse tras Phase 1.*

| Principio | Cómo lo cumple este plan | Estado |
|-----------|--------------------------|--------|
| I. Seguridad/privacidad primero | PII enmascarada por rol en pantalla y Excel; el archivo hereda sensibilidad y se advierte | ✅ |
| II. QR sin PII | El reporte referencia fornituras por código/id; no expone PII vía QR | ✅ (N/A directo) |
| III. Cero secretos | Sin secretos | ✅ |
| IV. Mínimo privilegio | Reportes y export por rol; PII solo a autorizados; rechazo por defecto | ✅ |
| V. Auditoría sin fugas | **Exportación auditada** (quién, qué reporte/filtros, cuándo) sin PII en el log | ✅ |
| VI. ADR / stack congelado | **Dependencia de Excel (POI/SXSSF)** → justificar y registrar (Principio VI) | ⚠️ ver research |

**Resultado del gate**: PASA con **decisión a registrar**: la librería de Excel (licencia/mantenimiento).
La PII reutiliza el enmascaramiento de 003; el export es un evento sensible auditado.

## Project Structure

### Documentation (this feature)

```text
specs/011-reportes/
├── plan.md              # Este archivo
└── tasks.md             # Phase 2: lo genera /speckit-tasks
```

> Sin entidades nuevas; contrato inline. La librería de Excel se registra (nota de decisión/ADR).

### Source Code (repository root)

```text
fornituras-api/
└── src/
    ├── main/java/com/numobiz/solutions/fornituras/modules/reports/
    │   ├── controller/     # ReportController (totales, asignaciones activas, export Excel)
    │   ├── service/        # ReportService (agregados, asignaciones activas), ExcelExportService (streaming)
    │   ├── repository/     # consultas de reporte (o reutiliza repos de 001/003/004)
    │   └── dto/            # ReportTotals, ActiveAssignmentRow (enmascarado por rol)
    └── test/java/.../modules/reports/

sigefor/
└── src/app/features/reportes/
    ├── pages/reportes/          # totales + tabla paginada + filtros + botón exportar
    └── data/reports.service.ts
```

**Structure Decision**: módulo `reports/` que **reutiliza** el enmascaramiento de PII de **003** (no
reimplementa reglas de visibilidad): las filas de asignaciones activas se construyen con el mapper de 003
según el rol del solicitante. El Excel se genera en **streaming** (SXSSF) para no cargar todo en memoria.
La **auditoría de exportación** usa el escritor de 012.

## Phase 0 — Research

Decisiones / incógnitas:
- **Librería de Excel** → Apache POI **SXSSF** (streaming) como candidato; **registrar la decisión**
  (licencia Apache-2.0, mantenimiento) por Principio VI. Alternativa: CSV en streaming si se quiere evitar
  la dependencia (decisión a confirmar).
- **Enmascaramiento**: reutilizar el `OfficerMapper`/reglas de 003 para que pantalla y Excel respeten el
  rol; **un rol sin permiso exporta PII enmascarada** (FR-006).
- **Volumen**: generación en streaming/lotes; paginación en pantalla; consultas indexadas.
- **Auditoría de export**: registrar quién exportó qué reporte/filtros y cuándo (sin PII) — evento
  sensible (Principio V).

## Phase 1 — Design & Contracts

- **Contract** (inline): `GET /reports/totals` (totales por estado + conteo de elementos),
  `GET /reports/active-assignments` (paginado + filtros QR/nombre/RFC/placa/CURP/municipio, enmascarado
  por rol), `GET /reports/active-assignments/export` (Excel en streaming, auditado),
  `GET /reports/{tipo}` (reportes operativos predefinidos). Authn + authz por rol.
- **Quickstart** (inline): totales coinciden con 010; filtrar por municipio → solo esas asignaciones;
  exportar con rol sin PII → Excel con campos sensibles enmascarados; cada export genera 1 evento de
  auditoría.

Re-check Constitution tras diseño: PII enmascarada en pantalla y Excel; export auditado sin PII; totales
consistentes con 010. **Gate sigue en PASA** (con la dependencia de Excel registrada).

## Complexity Tracking

> Sin violaciones. La única decisión abierta (librería de Excel) se registra por Principio VI. El
> enmascaramiento se **reutiliza** de 003; no se duplican reglas de PII.
