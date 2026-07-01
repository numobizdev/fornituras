package com.numobiz.solutions.fornituras.modules.decommissions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una baja definitiva. La fornitura se identifica por su código (QR/serie) y se resuelve en
 * el servidor (001); el motivo referencia el catálogo {@code decommission_reason}. Las observaciones
 * son opcionales. El responsable y la fecha los fija el servidor (no se confían al cliente).
 */
public record DecommissionRequest(
		@NotBlank(message = "El código de la fornitura es obligatorio")
		String codigo,

		@NotNull(message = "El motivo de baja es obligatorio")
		Long motivoId,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones
) {
}
