package com.numobiz.solutions.fornituras.modules.equipmenttypes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentTypeCreateRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 120, message = "El nombre no debe exceder 120 caracteres")
		String nombre,

		@Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
		String descripcion,

		@Size(max = 500, message = "La URL de foto no debe exceder 500 caracteres")
		String fotoUrl
) {
}
