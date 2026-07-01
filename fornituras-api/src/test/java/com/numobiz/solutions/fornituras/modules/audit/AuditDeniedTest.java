package com.numobiz.solutions.fornituras.modules.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Eventos denegados (T010/FR-006): un acceso rechazado por autorización (un rol operativo que intenta
 * consultar la bitácora, reservada a ADMIN) también queda auditado como {@code ACCESS_DENIED}.
 */
@WithMockUser(roles = "CAPTURISTA")
class AuditDeniedTest extends AuditApiTestSupport {

	@Test
	void deniedAccess_isAudited() throws Exception {
		mockMvc.perform(get("/api/v1/audit")).andExpect(status().isForbidden());

		assertThat(countAudit("ACCESS_DENIED")).isGreaterThanOrEqualTo(1);
	}
}
