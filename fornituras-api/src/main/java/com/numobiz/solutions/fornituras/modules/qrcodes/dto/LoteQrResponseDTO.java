package com.numobiz.solutions.fornituras.modules.qrcodes.dto;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "QR code batch summary")
public record LoteQrResponseDTO(
		@Schema(description = "Batch ID", example = "42")
		Long id,

		@Schema(description = "First consecutive number in the batch", example = "1")
		int consecutivoInicial,

		@Schema(description = "Last consecutive number in the batch", example = "100")
		int consecutivoFinal,

		@Schema(description = "Batch description", example = "Códigos prendas Chiapas")
		String descripcion,

		@Schema(description = "Number of codes in the batch", example = "100")
		int cantidad,

		@Schema(description = "QR area size in centimeters", example = "3.0")
		BigDecimal qrSizeCm,

		@Schema(description = "Padding around the QR in centimeters", example = "0.5")
		BigDecimal paddingCm,

		@Schema(description = "Position of the readable code label")
		LabelPosition labelPosition,

		@Schema(description = "Whether cut borders are drawn around each code")
		boolean mostrarBordes,

		@Schema(description = "Creation timestamp")
		LocalDateTime createdAt,

		@Schema(description = "Last update timestamp")
		LocalDateTime updatedAt
) {
	public static LoteQrResponseDTO from(LoteQR lote) {
		return new LoteQrResponseDTO(
				lote.getId(),
				lote.getConsecutivoInicial(),
				lote.getConsecutivoFinal(),
				lote.getDescripcion(),
				lote.getCantidad(),
				lote.getQrSizeCm(),
				lote.getPaddingCm(),
				lote.getLabelPosition(),
				lote.isMostrarBordes(),
				lote.getCreatedAt(),
				lote.getUpdatedAt());
	}
}
