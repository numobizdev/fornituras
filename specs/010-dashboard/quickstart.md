# Quickstart / Validación — Tablero de control (010)

Resultados de la validación de los escenarios del `spec.md`/`plan.md`. La feature no introduce
entidades: consume agregados de 001 (estados + `fecha_vencimiento`) y 008 (criterio de vigencia).

## Contrato

`GET /api/v1/dashboard/summary` → `{ total, disponibles, asignadas, proximasAVencer, caducadas,
enMantenimiento }`. Requiere autenticación (cualquier rol); solo contadores numéricos, **cero PII**.

## Escenarios validados

| Escenario | Cómo se valida | Estado |
|-----------|----------------|--------|
| Los contadores coinciden con los listados filtrados equivalentes (SC-002) | `DashboardSummaryIntegrationTest`: compara cada contador contra el `totalElements` de `/equipment[?status=…]` y contra la clasificación de `/alerts/vigencia` | ✅ |
| Inventario vacío → todos los contadores en cero, sin error (Edge Case) | `DashboardEmptyTest` | ✅ |
| Requiere autenticación; respuesta sin PII; cualquier rol | `DashboardAuthTest` (sin auth → 403; ADMIN/CAPTURISTA → 200; la respuesta expone exactamente los 6 contadores numéricos) | ✅ |
| Criterio de vigencia idéntico a 001/008 (≤ 90 días / vencida) (FR-005) | El servicio reutiliza `ExpiryCalculator.WARNING_WINDOW_DAYS`; las cuentas de caducadas/próximas usan la misma semántica que `AlertService` (008) | ✅ |
| Cambios de datos → la recarga refleja el estado actual | Agregación sin estado (consulta en cada request) + `ion-refresher` (pull-to-refresh) en la página `inicio` | ✅ |
| Color semántico institucional por indicador (FR-002) | Variables `--status-*` (docs/05-ui-ux.md §3) aplicadas en `inicio.page.scss`; cada tarjeta lleva etiqueta + color (no solo color) | ✅ |

## Rendimiento (SC-001)

Los contadores se calculan con **consultas agregadas** del lado servidor (1 `GROUP BY status` +
2 conteos por rango de `fecha_vencimiento`), apoyadas en los índices de 001
(`idx_equipment_status`, `idx_equipment_vencimiento`). No se transfiere el inventario al cliente y
se resuelve en una sola llamada HTTP. No se requirió caché para el MVP.
