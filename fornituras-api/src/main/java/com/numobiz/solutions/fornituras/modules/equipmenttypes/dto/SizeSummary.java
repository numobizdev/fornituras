package com.numobiz.solutions.fornituras.modules.equipmenttypes.dto;

public record SizeSummary(
		Long id,
		String etiqueta,
		Long equipmentTypeId,
		boolean active
) {
}
