package com.numobiz.solutions.fornituras.modules.qrcodes.dto;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Individual QR code within a batch")
public record CodigoQrResponseDTO(
		@Schema(description = "Code ID", example = "1001")
		Long id,

		@Schema(description = "Unique code value", example = "FOR-ABC12")
		String codigo,

		@Schema(description = "Parent batch ID", example = "42")
		Long loteQrId,

		@Schema(description = "Creation timestamp")
		LocalDateTime createdAt
) {
	public static CodigoQrResponseDTO from(CodigoQR codigoQR, Long loteQrId) {
		return new CodigoQrResponseDTO(
				codigoQR.getId(),
				codigoQR.getCodigo(),
				loteQrId,
				codigoQR.getCreatedAt());
	}
}
