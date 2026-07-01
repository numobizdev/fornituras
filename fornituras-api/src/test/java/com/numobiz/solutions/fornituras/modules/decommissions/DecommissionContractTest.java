package com.numobiz.solutions.fornituras.modules.decommissions;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code POST /api/v1/decommissions} (T009): baja válida (201, estado BAJA_DEFINITIVA),
 * validación del motivo/código (400), fornitura inexistente (404) y bloqueo con asignación vigente
 * (409).
 */
@WithMockUser(roles = "ADMIN")
class DecommissionContractTest extends DecommissionApiTestSupport {

	@Test
	void decommission_validRequest_returns201() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Chaleco perforado")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.equipmentCodigo").value("FOR-B1"))
				.andExpect(jsonPath("$.data.motivoNombre").value("Daño"));

		assertThat(decommissionRepository.count()).isEqualTo(1);
	}

	@Test
	void decommission_missingMotivo_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), null, "Sin motivo")))
				.andExpect(status().isBadRequest());

		assertThat(decommissionRepository.count()).isZero();
	}

	@Test
	void decommission_blankCodigo_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson("   ", seed.motivoId(), "Sin código")))
				.andExpect(status().isBadRequest());

		assertThat(decommissionRepository.count()).isZero();
	}

	@Test
	void decommission_unknownEquipment_returns404() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson("NO-EXISTE", seed.motivoId(), "Fornitura inexistente")))
				.andExpect(status().isNotFound());

		assertThat(decommissionRepository.count()).isZero();
	}

	@Test
	void decommission_withActiveAssignment_returns409() throws Exception {
		seedActiveAssignment(seed.equipmentId());

		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Intento bloqueado")))
				.andExpect(status().isConflict());

		assertThat(decommissionRepository.count()).isZero();
	}
}
