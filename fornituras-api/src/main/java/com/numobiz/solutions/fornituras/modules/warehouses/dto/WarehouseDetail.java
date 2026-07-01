package com.numobiz.solutions.fornituras.modules.warehouses.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ficha completa del almacén, incluidos los campos sensibles (ubicación, responsable, contacto).
 * Se expone solo a roles autorizados (ADMIN). {@code ocupacion}/{@code porcentajeOcupacion} se
 * derivan por conteo (puerto {@code WarehouseUsageQuery}); no se almacenan.
 */
public record WarehouseDetail(
		Long id,
		String codigo,
		String nombre,
		Long tipoItemId,
		String tipoNombre,
		String municipio,
		String estado,
		String direccion,
		String cp,
		BigDecimal latitud,
		BigDecimal longitud,
		Long responsableId,
		String telefono,
		String emailContacto,
		Integer capacidad,
		String observaciones,
		boolean active,
		long ocupacion,
		Double porcentajeOcupacion,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
