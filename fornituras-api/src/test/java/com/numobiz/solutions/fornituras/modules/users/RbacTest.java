package com.numobiz.solutions.fornituras.modules.users;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC (T017, SC-002): una acción fuera del alcance del rol se deniega (rechazo por defecto) y queda
 * auditada como {@code ACCESS_DENIED} (reusa el listener de 012).
 */
@WithMockUser(roles = "CAPTURISTA")
class RbacTest extends UserAdminTestSupport {

	@Test
	void outOfScopeAction_isDeniedAndAudited() throws Exception {
		mockMvc.perform(post("/api/v1/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"X\",\"email\":\"x@fornituras.local\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isForbidden());

		assertThat(countAudit("ACCESS_DENIED")).isGreaterThanOrEqualTo(1);
	}
}
