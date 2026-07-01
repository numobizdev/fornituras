package com.numobiz.solutions.fornituras.modules.equipment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Alta por lote: datos generales capturados una vez y N códigos físicos. El servicio crea una
 * fornitura por código, rechazando duplicados intra-lote y contra la base, de forma atómica.
 */
public record BatchCreateRequest(
		@NotNull(message = "El tipo de prenda es obligatorio")
		Long equipmentTypeId,

		Long sizeId,

		@NotNull(message = "El almacén es obligatorio")
		Long warehouseId,

		@Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
		String descripcion,

		@Size(max = 120, message = "La marca no debe exceder 120 caracteres")
		String marca,

		@Size(max = 120, message = "El modelo no debe exceder 120 caracteres")
		String modelo,

		@Size(max = 60, message = "El nivel balístico no debe exceder 60 caracteres")
		String nivelBalistico,

		LocalDate fechaFabricacion,

		LocalDate fechaAdquisicion,

		@PositiveOrZero(message = "La vida útil en meses no puede ser negativa")
		Integer vidaUtilMeses,

		LocalDate fechaVencimiento,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones,

		@NotEmpty(message = "Debe capturar al menos un código")
		List<@NotNull String> codigos
) {
}
