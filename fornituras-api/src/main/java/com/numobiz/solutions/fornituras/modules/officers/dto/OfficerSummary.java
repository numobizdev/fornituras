package com.numobiz.solutions.fornituras.modules.officers.dto;

/**
 * Fila del listado del padrón. Muestra el nombre (necesario para identificar) y datos operativos;
 * NO incluye CURP/RFC (esos solo aparecen, y enmascarados según rol, en la ficha).
 */
public record OfficerSummary(
		Long id,
		String nombreCompleto,
		String placa,
		String sexoNombre,
		String tipoSangreEtiqueta,
		String municipio,
		String estado,
		String fotoUrl,
		boolean active
) {
}
