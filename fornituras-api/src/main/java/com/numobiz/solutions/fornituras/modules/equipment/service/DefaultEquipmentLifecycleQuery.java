package com.numobiz.solutions.fornituras.modules.equipment.service;

import org.springframework.stereotype.Component;

/**
 * Implementación por defecto del puerto mientras 004 (asignaciones) y 007 (traslados) no existen:
 * ninguna fornitura está comprometida. Se reemplaza por una implementación real (marcada como
 * {@code @Primary}) cuando esas features se implementen, sin tocar {@code EquipmentService}.
 */
@Component
public class DefaultEquipmentLifecycleQuery implements EquipmentLifecycleQuery {

	@Override
	public boolean hasActiveAssignment(Long equipmentId) {
		return false;
	}

	@Override
	public boolean hasOngoingTransfer(Long equipmentId) {
		return false;
	}
}
