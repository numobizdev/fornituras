package com.numobiz.solutions.fornituras.modules.users;

import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del alta y de la regla de "no dejar el sistema sin admin" (T009, FR-007): las contraseñas
 * se guardan hasheadas (nunca en claro) y no se puede desactivar ni degradar al último administrador.
 */
@WithMockUser(roles = "ADMIN")
class UserAdminIntegrationTest extends UserAdminTestSupport {

	@Test
	void create_persistsHashedPassword_neverPlaintext() throws Exception {
		String body = objectMapper.writeValueAsString(
				new UserRequestDTO("Nora", "nora@fornituras.local", Role.CAPTURISTA));

		mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());

		User saved = userRepository.findByEmail("nora@fornituras.local").orElseThrow();
		assertThat(saved.getPassword()).isNotBlank();
		assertThat(saved.getPassword()).startsWith("$2");
	}

	@Test
	void disableLastAdmin_isBlocked() throws Exception {
		User admin = seedUser("Solo", "solo@fornituras.local", Role.ADMIN, true);

		mockMvc.perform(patch("/api/v1/users/" + admin.getId() + "/enabled")
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isConflict());

		assertThat(userRepository.findById(admin.getId()).orElseThrow().isEnabled()).isTrue();
	}

	@Test
	void demoteLastAdmin_isBlocked() throws Exception {
		User admin = seedUser("Jefe", "jefe@fornituras.local", Role.ADMIN, true);

		mockMvc.perform(patch("/api/v1/users/" + admin.getId() + "/role")
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"CAPTURISTA\"}"))
				.andExpect(status().isConflict());

		assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void disableAdmin_allowedWhenAnotherAdminRemains() throws Exception {
		User first = seedUser("Admin1", "a1@fornituras.local", Role.ADMIN, true);
		seedUser("Admin2", "a2@fornituras.local", Role.ADMIN, true);

		mockMvc.perform(patch("/api/v1/users/" + first.getId() + "/enabled")
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isOk());

		assertThat(userRepository.findById(first.getId()).orElseThrow().isEnabled()).isFalse();
	}
}
