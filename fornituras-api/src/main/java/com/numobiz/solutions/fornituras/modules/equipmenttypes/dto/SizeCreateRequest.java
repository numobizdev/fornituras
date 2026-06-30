package com.numobiz.solutions.fornituras.modules.equipmenttypes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SizeCreateRequest(
		@NotBlank(message = "La etiqueta es obligatoria")
		@Size(max = 50, message = "La etiqueta no debe exceder 50 caracteres")
		String etiqueta,

		/** NULL = talla global; si se indica, la talla queda acotada a ese tipo. */
		Long equipmentTypeId
) {
}
