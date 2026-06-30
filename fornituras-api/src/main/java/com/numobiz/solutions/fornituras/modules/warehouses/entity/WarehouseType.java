package com.numobiz.solutions.fornituras.modules.warehouses.entity;

/**
 * Clasificación operativa del almacén. Habilita reglas (p. ej. qué almacén puede ser origen de
 * un traslado) y reportes por tipo.
 */
public enum WarehouseType {
	CENTRAL,
	REGIONAL,
	MOVIL,
	TEMPORAL
}
