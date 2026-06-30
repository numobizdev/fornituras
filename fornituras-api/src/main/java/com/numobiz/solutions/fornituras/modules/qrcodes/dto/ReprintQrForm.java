package com.numobiz.solutions.fornituras.modules.qrcodes.dto;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReprintQrForm(
		@NotNull(message = "QR size is required")
		@DecimalMin(value = "1.0", message = "QR size must be at least 1.0 cm")
		@DecimalMax(value = "15.0", message = "QR size must not exceed 15.0 cm")
		BigDecimal qrSizeCm,

		@NotNull(message = "Padding is required")
		@DecimalMin(value = "0.0", message = "Padding must be at least 0.0 cm")
		@DecimalMax(value = "5.0", message = "Padding must not exceed 5.0 cm")
		BigDecimal paddingCm,

		@NotNull(message = "Label position is required")
		LabelPosition labelPosition,

		@NotNull(message = "Border option is required")
		Boolean mostrarBordes
) {
	public static ReprintQrForm from(LoteQR lote) {
		return new ReprintQrForm(lote.getQrSizeCm(), lote.getPaddingCm(), lote.getLabelPosition(), lote.isMostrarBordes());
	}
}
