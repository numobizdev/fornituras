# 0018. Exportación de reportes a Excel en .NET (ClosedXML)

- **Estado:** Aceptado
- **Fecha:** 2026-07-01
- **Relacionado:** [0011 Exportación a Excel con Apache POI](./0011-libreria-export-excel.md) (Java,
  superado en .NET), [0016 Backend ASP.NET Core](./0016-backend-aspnetcore.md),
  spec [`011-reportes`](../../specs/011-reportes/spec.md),
  auditoría [`018`](../../specs/018-auditoria-migracion-dotnet/findings-fr.md) (hallazgo G-1)

## Contexto

La spec **011** exige exportar reportes **a Excel** (FR-003). En el backend Java, [ADR 0011]
eligió **Apache POI** (SXSSF, streaming). Tras la migración a **.NET** ([ADR 0016]), POI ya no
aplica. La auditoría requisito por requisito (spec 018, **G-1**) detectó que la implementación .NET
**etiquetaba** la respuesta como `.xlsx`
(`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) pero devolvía **bytes CSV**:
un archivo mal formado que Excel abre con advertencia o error. Hay que producir Excel real.

## Decisión

Generar libros **`.xlsx` reales** con **ClosedXML** (`0.104.x`, licencia **MIT**), sobre
`DocumentFormat.OpenXml`. Se encapsula en un helper `Common/XlsxWriter.cs` (cabecera + filas), usado
por `ReportService` para `active-assignments` y los reportes predefinidos.

- **Licencia/mantenimiento (Principio VI):** MIT, proyecto activo y ampliamente usado; sin
  dependencias nativas. Justificada por ser requisito de la spec (formato Excel).
- **Alternativas descartadas:** (a) `DocumentFormat.OpenXml` a pelo → API de bajo nivel, más código
  y más frágil; (b) mantener CSV → incumple FR-003 y deja el archivo mal etiquetado; (c) EPPlus →
  licencia comercial (Polyform) desde la v5, descartada por coste/licencia.

## Consecuencias

- (+) La exportación cumple FR-003: `.xlsx` auténtico, coherente con el content-type declarado.
- (+) Helper reutilizable para futuras exportaciones; la exportación queda **auditada** (G-2).
- (−) Nueva dependencia gestionada (ClosedXML + OpenXml); se asume por el requisito de formato.
- (−) ClosedXML materializa el libro en memoria (no streaming como POI/SXSSF). Aceptable para el
  volumen de estos reportes; si creciera mucho, evaluar OpenXml en modo streaming.

## Reversión

Si se decidiera otro formato/estrategia, se cambia solo `XlsxWriter` y el content-type del
controller; el resto de `ReportService` no depende de la librería.
