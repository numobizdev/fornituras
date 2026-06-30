package com.numobiz.solutions.fornituras.modules.warehouses.service;

import org.springframework.stereotype.Component;

/**
 * Implementación por defecto del puerto de uso: 0 referencias, porque las features que referencian
 * almacenes (001-inventario, 007-traslados) aún no existen. Al integrarlas, la implementación real
 * (que consulta sus repositorios) se registra como {@code @Primary} y el bloqueo de borrado en uso
 * queda activo sin tocar {@code WarehouseService}.
 */
@Component
public class DefaultWarehouseUsageQuery implements WarehouseUsageQuery {

	@Override
	public long countUsage(Long warehouseId) {
		return 0L;
	}
}
