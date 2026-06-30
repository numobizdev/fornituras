package com.numobiz.solutions.fornituras.modules.qrcodes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Individual QR code within a batch")
public record CodigoQrResponseDTO(
		@Schema(description = "Unique code value", example = "FOR-000001")
		String codigo,

		@Schema(description = "Parent batch ID", example = "42")
		Long loteQrId
) {
	public static CodigoQrResponseDTO from(String codigo, Long loteQrId) {
		return new CodigoQrResponseDTO(codigo, loteQrId);
	}
}
