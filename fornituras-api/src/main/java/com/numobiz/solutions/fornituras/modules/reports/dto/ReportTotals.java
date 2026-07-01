package com.numobiz.solutions.fornituras.modules.reports.dto;

/**
 * Totales de la vista de control (011). Los contadores por estado de fornitura se calculan con los
 * mismos agregados que el tablero (010), de modo que <b>coinciden</b> con él (SC-001). Sin PII.
 */
public record ReportTotals(
		long totalFornituras,
		long disponibles,
		long asignadas,
		long enMantenimiento,
		long conIncidencia,
		long baja,
		long totalElementos) {
}
