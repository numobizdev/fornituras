package com.numobiz.solutions.fornituras.modules.decommissions;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización de {@code /decommissions} (T007): dar de baja está restringido a ADMIN (rol elevado);
 * consultar el listado y el catálogo de motivos es para cualquier rol autenticado. Un rol operativo
 * (CAPTURISTA) que intenta dar de baja queda denegado (403) sin persistir nada.
 */
class DecommissionAuthTest extends DecommissionApiTestSupport {

	@Test
	@WithMockUser(roles = "CONSULTA")
	void list_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/decommissions")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CONSULTA")
	void reasons_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/decommissions/reasons")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void decommission_withoutAdminRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Sin permiso")))
				.andExpect(status().isForbidden());

		assertThat(decommissionRepository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void decommission_withAdminRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/decommissions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(decommissionJson(seed.equipmentCodigo(), seed.motivoId(), "Baja autorizada")))
				.andExpect(status().isCreated());

		assertThat(decommissionRepository.count()).isEqualTo(1);
	}
}
