package com.numobiz.solutions.fornituras.modules.incidents;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización de {@code /incidents} y {@code /alerts} (T007): la consulta es para cualquier rol
 * autenticado; reportar/actualizar quedan restringidos a ADMIN/CAPTURISTA. Un rol solo-consulta que
 * intenta reportar queda denegado (403) sin persistir nada.
 */
class IncidentAuthTest extends IncidentApiTestSupport {

	@Test
	@WithMockUser(roles = "CONSULTA")
	void list_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/incidents")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CONSULTA")
	void alerts_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/alerts/vigencia")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CONSULTA")
	void report_withoutWriteRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), "DANO", "Intento sin permiso")))
				.andExpect(status().isForbidden());

		assertThat(incidentRepository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void report_withCapturistaRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), "DANO", "Reporte válido")))
				.andExpect(status().isCreated());

		assertThat(incidentRepository.count()).isEqualTo(1);
	}
}
