package com.numobiz.solutions.fornituras.modules.assignments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de asignación: liga una fornitura disponible a un elemento. */
public record AssignRequest(
		@NotNull(message = "La fornitura es obligatoria")
		Long equipmentId,

		@NotNull(message = "El elemento es obligatorio")
		Long officerId,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones
) {
}
