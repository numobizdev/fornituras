package com.numobiz.solutions.fornituras.modules.users;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización del CRUD de usuarios (T010): un rol no administrador (CAPTURISTA) no puede administrar
 * usuarios — rechazo por defecto (403) en listar, crear, cambiar estado y cambiar rol.
 */
@WithMockUser(roles = "CAPTURISTA")
class UserAdminAuthTest extends UserAdminTestSupport {

	@Test
	void capturista_cannotListUsers() throws Exception {
		mockMvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
	}

	@Test
	void capturista_cannotCreateUser() throws Exception {
		mockMvc.perform(post("/api/v1/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"X\",\"email\":\"x@fornituras.local\",\"role\":\"CAPTURISTA\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void capturista_cannotChangeEnabledOrRole() throws Exception {
		User user = seedUser("Gala", "gala@fornituras.local", Role.CAPTURISTA, true);

		mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/enabled")
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/role")
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isForbidden());
	}
}
