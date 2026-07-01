package com.numobiz.solutions.fornituras.modules.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización de la consulta (T016/FR-004, matriz ADR 0013 regla 4): la bitácora la consultan los roles
 * de auditoría {@code READ_AUDIT} (ADMIN y AUDITOR); un rol operativo o de inventario queda denegado.
 */
class AuditQueryAuthTest extends AuditApiTestSupport {

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void nonAudit_isForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ALMACEN")
	void inventoryRole_isForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void admin_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "AUDITOR")
	void auditor_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isOk());
	}
}
