package com.numobiz.solutions.fornituras.modules.incidents;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code POST /api/v1/incidents} (T009): reporte válido (201, estado inicial ABIERTA),
 * validación de tipo y descripción (400) y fornitura inexistente (404).
 */
@WithMockUser(roles = "ADMIN")
class IncidentCreateContractTest extends IncidentApiTestSupport {

	@Test
	void report_validRequest_returns201AndAbierta() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), "DANO", "Chaleco con costura rota")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.estado").value("ABIERTA"))
				.andExpect(jsonPath("$.data.tipo").value("DANO"))
				.andExpect(jsonPath("$.data.equipmentCodigo").value("FOR-I1"));

		assertThat(incidentRepository.count()).isEqualTo(1);
	}

	@Test
	void report_missingTipo_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), null, "Sin tipo")))
				.andExpect(status().isBadRequest());

		assertThat(incidentRepository.count()).isZero();
	}

	@Test
	void report_blankDescripcion_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), "FALLA", "   ")))
				.andExpect(status().isBadRequest());

		assertThat(incidentRepository.count()).isZero();
	}

	@Test
	void report_unknownEquipment_returns404() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(999999L, "DANO", "Fornitura inexistente")))
				.andExpect(status().isNotFound());

		assertThat(incidentRepository.count()).isZero();
	}
}
