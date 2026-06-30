package com.numobiz.solutions.fornituras.modules.equipmenttypes.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EquipmentTypeDetail(
		Long id,
		String nombre,
		String descripcion,
		String fotoUrl,
		boolean active,
		List<SizeSummary> sizes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
