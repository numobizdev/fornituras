package com.numobiz.solutions.fornituras.modules.assignments.service;

import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentLifecycleQuery;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Implementación real del puerto que 001 dejó abierto: ahora que existe el módulo de asignaciones,
 * una fornitura con asignación vigente queda comprometida. Marcada {@code @Primary} para reemplazar
 * el {@code DefaultEquipmentLifecycleQuery} (que respondía "no"), activando el bloqueo de baja y
 * traslado de fornituras asignadas sin tocar el módulo de inventario.
 *
 * <p>El "traslado en curso" lo resolverá 007; hasta entonces se responde "no" para esa parte.
 */
@Primary
@Component
public class AssignmentLifecycleQuery implements EquipmentLifecycleQuery {

	private final AssignmentRepository assignmentRepository;

	public AssignmentLifecycleQuery(AssignmentRepository assignmentRepository) {
		this.assignmentRepository = assignmentRepository;
	}

	@Override
	public boolean hasActiveAssignment(Long equipmentId) {
		return assignmentRepository.existsByEquipmentIdAndFechaDevolucionIsNull(equipmentId);
	}

	@Override
	public boolean hasOngoingTransfer(Long equipmentId) {
		return false;
	}
}
