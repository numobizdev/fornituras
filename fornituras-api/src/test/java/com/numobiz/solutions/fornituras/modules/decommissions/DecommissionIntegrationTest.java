package com.numobiz.solutions.fornituras.modules.decommissions;

import com.numobiz.solutions.fornituras.modules.decommissions.entity.Decommission;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de la baja (T010): una baja exitosa deja la fornitura en BAJA_DEFINITIVA y registra el
 * historial (motivo, fecha) preservando la fornitura; una fornitura con asignación vigente o en un
 * traslado en curso no puede darse de baja (409) y ni su estado ni el historial cambian.
 */
@WithMockUser(roles = "ADMIN")
class DecommissionIntegrationTest extends DecommissionApiTestSupport {

	@Test
	void decommission_available_setsBajaDefinitivaAndKeepsHistory() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Baja por daño")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.findById(seed.equipmentId()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.BAJA_DEFINITIVA);
		// La fornitura se conserva (no se borra), y queda un registro de baja con su motivo y fecha.
		assertThat(equipmentRepository.existsById(seed.equipmentId())).isTrue();
		assertThat(decommissionRepository.count()).isEqualTo(1);
		Decommission record = decommissionRepository.findAll().getFirst();
		assertThat(record.getEquipmentId()).isEqualTo(seed.equipmentId());
		assertThat(record.getMotivoId()).isEqualTo(seed.motivoId());
		assertThat(record.getFecha()).isNotNull();
	}

	@Test
	void decommission_withActiveAssignment_isBlocked() throws Exception {
		seedActiveAssignment(seed.equipmentId());

		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Bloqueada")))
				.andExpect(status().isConflict());

		assertThat(equipmentRepository.findById(seed.equipmentId()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.DISPONIBLE);
		assertThat(decommissionRepository.count()).isZero();
	}

	@Test
	void decommission_withOngoingTransfer_isBlocked() throws Exception {
		seedOngoingTransfer(seed.equipmentId());

		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Bloqueada")))
				.andExpect(status().isConflict());

		assertThat(decommissionRepository.count()).isZero();
	}
}
