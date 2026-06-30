package com.numobiz.solutions.fornituras.modules.equipment.entity;

/**
 * Estado operativo de una fornitura (catálogo controlado). Persiste como cadena para legibilidad
 * en BD y se valida con un {@code CHECK} en la migración. La transición entre estados la gobierna
 * {@code EquipmentService} (p. ej. no se da de baja una fornitura con asignación vigente).
 */
public enum EquipmentStatus {
	DISPONIBLE,
	ASIGNADA,
	EN_MANTENIMIENTO,
	EN_TRASLADO,
	EXTRAVIADA,
	BAJA_DEFINITIVA
}
