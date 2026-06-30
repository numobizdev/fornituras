package com.numobiz.solutions.fornituras.modules.municipios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MunicipioCreateRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 120, message = "El nombre no debe exceder 120 caracteres")
		String nombre
) {
}
