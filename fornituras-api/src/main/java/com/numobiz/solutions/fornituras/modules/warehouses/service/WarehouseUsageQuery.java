package com.numobiz.solutions.fornituras.modules.warehouses.service;

/**
 * Puerto que cuenta cuántas entidades (fornituras de 001, traslados de 007) referencian a un
 * almacén. Aísla a {@code WarehouseService} de features que aún no existen: mientras esas tablas
 * no estén, la implementación por defecto devuelve 0. Cuando 001/007 se implementen, se provee
 * una implementación real (que consulta sus repositorios) y el bloqueo de borrado en uso queda
 * activo sin tocar el servicio.
 */
public interface WarehouseUsageQuery {

	long countUsage(Long warehouseId);
}
