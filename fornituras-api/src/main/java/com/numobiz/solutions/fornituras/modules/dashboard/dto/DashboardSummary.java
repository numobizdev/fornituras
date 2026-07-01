package com.numobiz.solutions.fornituras.modules.dashboard.dto;

/**
 * Indicadores agregados del inventario para el tablero de control (feature 010). Solo contadores
 * numéricos: <b>cero PII</b> y ningún registro individual. Los estados operativos
 * (disponibles/asignadas/en mantenimiento) se cuentan sobre {@code equipment.status}; la vigencia
 * (próximas a vencer/caducadas) se deriva de {@code fecha_vencimiento} con el mismo criterio de
 * 001/008 para que los números coincidan con sus listados.
 */
public record DashboardSummary(
		long total,
		long disponibles,
		long asignadas,
		long proximasAVencer,
		long caducadas,
		long enMantenimiento) {
}
