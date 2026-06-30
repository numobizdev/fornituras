package com.numobiz.solutions.fornituras.modules.qrcodes.dto;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GenerateQrForm(
		@NotBlank(message = "La descripción es obligatoria")
		@Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
		String descripcion,

		@NotNull(message = "Quantity is required")
		@Min(value = 1, message = "Quantity must be at least 1")
		@Max(value = 10000, message = "Quantity must not exceed 10000")
		Integer cantidad,

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
	public static GenerateQrForm defaults() {
		return new GenerateQrForm("", 10, new BigDecimal("3.0"), new BigDecimal("0.5"), LabelPosition.BOTTOM, true);
	}
}
