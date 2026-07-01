package com.numobiz.solutions.fornituras.modules.incidents.dto;

import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Actualización del estado de una incidencia. Al pasar a resuelta/cerrada se registra la fecha de
 * resolución y, si la fornitura estaba retirada por la incidencia, vuelve a disponible.
 */
public record IncidentUpdateRequest(
		@NotNull(message = "El estado es obligatorio")
		IncidentStatus estado
) {
}
