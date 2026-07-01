package com.numobiz.solutions.fornituras.modules.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización de la consulta (T016/FR-004): solo ADMIN (auditoría) consulta la bitácora; un rol
 * operativo queda denegado.
 */
class AuditQueryAuthTest extends AuditApiTestSupport {

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void nonAdmin_isForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void admin_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isOk());
	}
}
