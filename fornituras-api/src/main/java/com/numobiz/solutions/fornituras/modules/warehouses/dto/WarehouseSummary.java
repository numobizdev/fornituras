package com.numobiz.solutions.fornituras.modules.warehouses.dto;

import com.numobiz.solutions.fornituras.modules.warehouses.entity.WarehouseType;

/**
 * Vista de listado del almacén: solo campos NO sensibles. Apta para cualquier rol autenticado
 * (selector en alta de fornituras y traslados). Los datos sensibles viven en {@link WarehouseDetail}.
 */
public record WarehouseSummary(
		Long id,
		String codigo,
		String nombre,
		WarehouseType tipo,
		boolean active
) {
}
