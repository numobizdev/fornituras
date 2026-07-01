package com.numobiz.solutions.fornituras.modules.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Alta/edición de una fornitura individual. La validación de borde asegura código y catálogos
 * requeridos; la unicidad (normalizada) y la coherencia de fechas se verifican en el servicio.
 * {@code fechaVencimiento} es opcional: si no se envía pero hay {@code fechaFabricacion} y
 * {@code vidaUtilMeses}, el servicio la deriva.
 */
public record EquipmentCreateRequest(
		@NotBlank(message = "El código (QR/serie) es obligatorio")
		@Size(max = 60, message = "El código no debe exceder 60 caracteres")
		String codigoQr,

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

		@Size(max = 60, message = "El número de inventario no debe exceder 60 caracteres")
		String numeroInventario,

		LocalDate fechaFabricacion,

		LocalDate fechaAdquisicion,

		@PositiveOrZero(message = "La vida útil en meses no puede ser negativa")
		Integer vidaUtilMeses,

		LocalDate fechaVencimiento,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones,

		@Size(max = 500, message = "La URL de foto no debe exceder 500 caracteres")
		String fotoUrl
) {
}
