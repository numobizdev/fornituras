package com.numobiz.solutions.fornituras.modules.transfers.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Alta de traslado: mueve las fornituras indicadas del almacén origen al destino. Las fornituras
 * llegan resueltas server-side por su código (014 → {@code GET /equipment/by-codigo}); aquí se
 * envían por id. Origen y destino deben ser distintos (validado en el servicio y por {@code CHECK}).
 */
public record TransferCreateRequest(
		@NotNull(message = "El almacén origen es obligatorio")
		Long origenId,

		@NotNull(message = "El almacén destino es obligatorio")
		Long destinoId,

		@NotEmpty(message = "Debe incluir al menos una fornitura")
		List<Long> equipmentIds,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones
) {
}
