package com.numobiz.solutions.fornituras.modules.assignments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Reasignación: cierra la asignación vigente de una fornitura y abre una nueva para otro elemento. */
public record ReassignRequest(
		@NotNull(message = "La fornitura es obligatoria")
		Long equipmentId,

		@NotNull(message = "El nuevo elemento es obligatorio")
		Long newOfficerId,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones
) {
}
