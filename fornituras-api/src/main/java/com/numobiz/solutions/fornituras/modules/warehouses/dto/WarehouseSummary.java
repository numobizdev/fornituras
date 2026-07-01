package com.numobiz.solutions.fornituras.modules.warehouses.dto;

/**
 * Vista de listado del almacén: solo campos NO sensibles. Apta para cualquier rol autenticado
 * (selector en alta de fornituras y traslados). Los datos sensibles viven en {@link WarehouseDetail}.
 */
public record WarehouseSummary(
		Long id,
		String codigo,
		String nombre,
		Long tipoItemId,
		String tipoNombre,
		boolean active
) {
}
