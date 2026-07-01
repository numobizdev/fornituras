package com.numobiz.solutions.fornituras.modules.warehouses.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos de alta/edición de un almacén. La validación en el borde garantiza unicidad de
 * clave/nombre (verificada en el servicio), tipo válido y rangos geográficos correctos.
 */
public record WarehouseCreateRequest(
		@NotBlank(message = "La clave (código) es obligatoria")
		@Size(max = 40, message = "La clave no debe exceder 40 caracteres")
		String codigo,

		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 120, message = "El nombre no debe exceder 120 caracteres")
		String nombre,

		@NotNull(message = "El tipo de almacén es obligatorio")
		Long tipoItemId,

		@Size(max = 120, message = "El municipio no debe exceder 120 caracteres")
		String municipio,

		@Size(max = 120, message = "El estado no debe exceder 120 caracteres")
		String estado,

		@Size(max = 255, message = "La dirección no debe exceder 255 caracteres")
		String direccion,

		@Size(max = 10, message = "El código postal no debe exceder 10 caracteres")
		String cp,

		@DecimalMin(value = "-90.0", message = "Latitud fuera de rango")
		@DecimalMax(value = "90.0", message = "Latitud fuera de rango")
		BigDecimal latitud,

		@DecimalMin(value = "-180.0", message = "Longitud fuera de rango")
		@DecimalMax(value = "180.0", message = "Longitud fuera de rango")
		BigDecimal longitud,

		Long responsableId,

		@Size(max = 30, message = "El teléfono no debe exceder 30 caracteres")
		String telefono,

		@Email(message = "El correo de contacto no es válido")
		@Size(max = 255, message = "El correo no debe exceder 255 caracteres")
		String emailContacto,

		@PositiveOrZero(message = "La capacidad no puede ser negativa")
		Integer capacidad,

		@Size(max = 500, message = "Las observaciones no deben exceder 500 caracteres")
		String observaciones
) {
}
