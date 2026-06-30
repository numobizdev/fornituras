package com.numobiz.solutions.fornituras.modules.equipment.dto;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ficha completa de la fornitura. No expone datos del elemento asignado (la asignación vive en
 * 004); {@code vigencia} se deriva de {@code fechaVencimiento}.
 */
public record EquipmentDetail(
		Long id,
		String codigoQr,
		Long equipmentTypeId,
		String tipoNombre,
		Long sizeId,
		String tallaEtiqueta,
		Long warehouseId,
		String almacenNombre,
		EquipmentStatus status,
		ExpiryStatus vigencia,
		String descripcion,
		String marca,
		String modelo,
		String nivelBalistico,
		String numeroInventario,
		LocalDate fechaFabricacion,
		LocalDate fechaAdquisicion,
		Integer vidaUtilMeses,
		LocalDate fechaVencimiento,
		String observaciones,
		String fotoUrl,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
