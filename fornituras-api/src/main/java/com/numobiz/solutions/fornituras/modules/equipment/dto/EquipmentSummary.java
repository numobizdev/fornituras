package com.numobiz.solutions.fornituras.modules.equipment.dto;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;

import java.time.LocalDate;

/**
 * Fila del listado de fornituras (sin PII del elemento). Incluye los nombres resueltos de los
 * catálogos y la vigencia derivada para pintar el color semántico en la UI.
 */
public record EquipmentSummary(
		Long id,
		String codigoQr,
		String descripcion,
		String tipoNombre,
		String tallaEtiqueta,
		String almacenNombre,
		EquipmentStatus status,
		ExpiryStatus vigencia,
		LocalDate fechaVencimiento
) {
}
