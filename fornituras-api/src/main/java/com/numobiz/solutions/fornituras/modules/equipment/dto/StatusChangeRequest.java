package com.numobiz.solutions.fornituras.modules.equipment.dto;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

/** Cambio de estado operativo de una fornitura (catálogo controlado). */
public record StatusChangeRequest(
		@NotNull(message = "El estado es obligatorio")
		EquipmentStatus status
) {
}
