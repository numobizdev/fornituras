package com.numobiz.solutions.fornituras.modules.decommissions;

import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.assignments.dto.AssignRequest;
import com.numobiz.solutions.fornituras.modules.assignments.service.AssignmentService;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferCreateRequest;
import com.numobiz.solutions.fornituras.modules.transfers.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardas sobre una fornitura dada de baja (T011, SC-002): una fornitura en BAJA_DEFINITIVA no puede
 * asignarse ni trasladarse; ambas operaciones se rechazan al no estar disponible. Verifica que la
 * baja cierra el ciclo de vida y no reabre la fornitura a operaciones.
 */
@WithMockUser(roles = "ADMIN")
class DecommissionGuardTest extends DecommissionApiTestSupport {

	@Autowired
	private AssignmentService assignmentService;
	@Autowired
	private TransferService transferService;

	@Test
	void assign_decommissionedEquipment_isRejected() {
		long equipmentId = seedEquipment("FOR-BD", EquipmentStatus.BAJA_DEFINITIVA);

		assertThatThrownBy(() -> assignmentService.assign(new AssignRequest(equipmentId, 1L, null)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void transfer_decommissionedEquipment_isRejected() {
		long equipmentId = seedEquipment("FOR-BD", EquipmentStatus.BAJA_DEFINITIVA);
		long destino = seedActiveWarehouse("ALM-DST", "Almacén Destino");

		assertThatThrownBy(() -> transferService.create(
				new TransferCreateRequest(seed.warehouseId(), destino, List.of(equipmentId), null)))
				.isInstanceOf(ConflictException.class);
	}
}
