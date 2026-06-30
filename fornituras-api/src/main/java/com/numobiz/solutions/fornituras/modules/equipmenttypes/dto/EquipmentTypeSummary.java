package com.numobiz.solutions.fornituras.modules.equipmenttypes.dto;

public record EquipmentTypeSummary(
		Long id,
		String nombre,
		String descripcion,
		String fotoUrl,
		boolean active
) {
}
