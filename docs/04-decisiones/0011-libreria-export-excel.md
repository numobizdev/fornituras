# 0011. Exportación de reportes a Excel con Apache POI (SXSSF, streaming)

- **Estado:** **Aceptado**
- **Fecha:** 2026-07-01
- **Feature:** [011-reportes](../../specs/011-reportes/) (tareas T003, T017-T018)

## Contexto

La feature 011 (Reportes y estadística) exige **exportar a Excel** el reporte vigente con sus
filtros (FR-003), respetando el **enmascaramiento de PII por rol** y **auditando** cada exportación
(FR-005). El reporte puede alcanzar decenas de miles de filas y debe generarse **sin agotar
memoria** (SC-002). El proyecto no tenía librería de generación de hojas de cálculo.

Opciones consideradas:

1. **CSV en streaming (sin dependencia):** cero dependencias nuevas; se abre en Excel, pero no es un
   `.xlsx` nativo (problemas de separador/encoding, sin tipos ni formato) — menos fiel a "Excel".
2. **Apache POI clásico (XSSF):** genera `.xlsx` nativo pero **carga todo el libro en memoria**;
   inviable para grandes volúmenes (SC-002).
3. **Apache POI SXSSF** (`org.apache.poi:poi-ooxml`, **Apache-2.0**): API de streaming que mantiene
   en memoria solo una ventana de filas y vuelca el resto a disco temporal; produce `.xlsx` nativo y
   escala a decenas de miles de filas.

## Decisión

1. Usar **Apache POI SXSSF** para generar los `.xlsx`. La generación va en un
   `ExcelExportService` desacoplado, detrás de la lógica de reporte (el controlador solo orquesta la
   descarga vía `StreamingResponseBody`).
2. El servicio de export **reutiliza el enmascaramiento de PII de 003** (mismo criterio de rol que
   la pantalla): un rol sin permiso exporta la PII enmascarada (FR-006, SC-004).
3. Cada exportación se **audita** (`EXPORT_REPORT`) con actor, tipo de reporte y filtros aplicados,
   **sin PII** en el registro (FR-005, Principio V).
4. **Dependencia (Principio VI / regla 4):** se registra POI con **necesidad** (exportar `.xlsx`
   nativo a gran volumen), **licencia** aceptable (Apache-2.0) y **mantenimiento** activo.

## Alternativas consideradas

- **CSV** (opción 1): descartada como formato principal por no cumplir "Excel" de forma nativa; se
  mantiene como posible formato adicional a futuro si se requiere ligereza.
- **XSSF en memoria** (opción 2): descartada por no cumplir SC-002 (agota memoria en alto volumen).

## Consecuencias

- **Positivas:** `.xlsx` nativo; escala a gran volumen con huella de memoria acotada (ventana de
  filas + disco temporal); API madura y estándar.
- **Límites:** POI arrastra dependencias transitivas (xmlbeans, commons-compress/-io/-collections4);
  SXSSF usa **archivos temporales** que deben limpiarse tras el volcado (`dispose()`), lo que el
  servicio hace siempre en `finally`.
- **Seguridad:** el `.xlsx` **hereda la sensibilidad** de los datos; se advierte en la UI y el
  contenido respeta el enmascaramiento por rol. La exportación queda auditada.
