package com.numobiz.solutions.fornituras.modules.assignments.service;

import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentLifecycleQuery;
import org.springframework.stereotype.Component;

/**
 * Responde la parte de <b>asignaciones</b> del puerto de ciclo de vida de 001: una fornitura con
 * asignación vigente queda comprometida. Ya no es {@code @Primary}: desde 007 la implementación
 * primaria es {@code TransferLifecycleQuery}, que compone esta respuesta con la de traslados en
 * curso. Se mantiene como pieza reutilizable (LEGO) inyectada por esa composición.
 *
 * <p>El "traslado en curso" lo resuelve 007; aquí se responde "no" para esa parte.
 */
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
