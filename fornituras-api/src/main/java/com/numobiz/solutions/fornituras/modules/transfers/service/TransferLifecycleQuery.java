package com.numobiz.solutions.fornituras.modules.transfers.service;

import com.numobiz.solutions.fornituras.modules.assignments.service.AssignmentLifecycleQuery;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentLifecycleQuery;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Implementación completa del puerto de ciclo de vida que 001 dejó abierto: compone la respuesta de
 * asignaciones (004) con la de traslados (007). Reemplaza como {@code @Primary} a la de asignaciones
 * (que ya no es primaria) para que ahora una fornitura <b>en un traslado en curso</b> también quede
 * comprometida (no se puede asignar ni dar de baja), sin tocar el módulo de inventario (LEGO/DIP).
 */
@Primary
@Component
public class TransferLifecycleQuery implements EquipmentLifecycleQuery {

	private final AssignmentLifecycleQuery assignmentLifecycle;
	private final TransferRepository transferRepository;

	public TransferLifecycleQuery(
			AssignmentLifecycleQuery assignmentLifecycle,
			TransferRepository transferRepository) {
		this.assignmentLifecycle = assignmentLifecycle;
		this.transferRepository = transferRepository;
	}

	@Override
	public boolean hasActiveAssignment(Long equipmentId) {
		return assignmentLifecycle.hasActiveAssignment(equipmentId);
	}

	@Override
	public boolean hasOngoingTransfer(Long equipmentId) {
		return transferRepository.existsOngoingByEquipmentId(equipmentId);
	}
}
