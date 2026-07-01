package com.numobiz.solutions.fornituras.modules.incidents.dto;

import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una incidencia sobre una fornitura (por id). El tipo determina el retiro de la fornitura
 * (extravío → extraviada; resto → en mantenimiento). El estado inicial siempre es "abierta".
 */
public record IncidentCreateRequest(
		@NotNull(message = "La fornitura es obligatoria")
		Long equipmentId,

		@NotNull(message = "El tipo de incidencia es obligatorio")
		IncidentType tipo,

		@NotBlank(message = "La descripción es obligatoria")
		@Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
		String descripcion
) {
}
