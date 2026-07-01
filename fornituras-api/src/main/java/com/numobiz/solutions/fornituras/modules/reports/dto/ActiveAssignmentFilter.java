package com.numobiz.solutions.fornituras.modules.reports.dto;

/**
 * Filtros del reporte de asignaciones activas. QR y placa filtran por coincidencia; CURP y RFC por
 * igualdad exacta (vía blind index); municipio por coincidencia. El nombre se filtra en memoria
 * (dato cifrado no determinista). Todos son opcionales; nulo/blanco = sin filtrar.
 */
public record ActiveAssignmentFilter(
		String qr,
		String nombre,
		String rfc,
		String placa,
		String curp,
		String municipio) {

	public static ActiveAssignmentFilter empty() {
		return new ActiveAssignmentFilter(null, null, null, null, null, null);
	}
}
