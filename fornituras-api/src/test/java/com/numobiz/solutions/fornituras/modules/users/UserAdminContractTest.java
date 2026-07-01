package com.numobiz.solutions.fornituras.modules.users;

import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserUpdateRequest;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato del CRUD admin de usuarios (T008): listado paginado, alta (201, sin exponer hash), edición,
 * activar/desactivar y cambio de rol; email duplicado → 409.
 */
@WithMockUser(roles = "ADMIN")
class UserAdminContractTest extends UserAdminTestSupport {

	@Test
	void list_isPaginated() throws Exception {
		seedUser("Ana", "ana@fornituras.local", Role.ADMIN, true);
		seedUser("Beto", "beto@fornituras.local", Role.CAPTURISTA, true);

		mockMvc.perform(get("/api/v1/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content[0].password").doesNotExist());

		mockMvc.perform(get("/api/v1/users").param("size", "1").param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content.length()").value(1));
	}

	@Test
	void create_returns201_andNeverExposesPassword() throws Exception {
		String body = objectMapper.writeValueAsString(
				new UserRequestDTO("Carla", "carla@fornituras.local", Role.CAPTURISTA));

		mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.email").value("carla@fornituras.local"))
				.andExpect(jsonPath("$.data.enabled").value(false))
				.andExpect(jsonPath("$.data.password").doesNotExist());
	}

	@Test
	void create_duplicateEmail_returns409() throws Exception {
		seedUser("Dora", "dora@fornituras.local", Role.CAPTURISTA, true);
		String body = objectMapper.writeValueAsString(
				new UserRequestDTO("Otra Dora", "dora@fornituras.local", Role.CAPTURISTA));

		mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void update_editsName() throws Exception {
		User user = seedUser("Eva", "eva@fornituras.local", Role.CAPTURISTA, true);
		String body = objectMapper.writeValueAsString(new UserUpdateRequest("Eva María"));

		mockMvc.perform(put("/api/v1/users/" + user.getId())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("Eva María"));
	}

	@Test
	void patchEnabledAndRole_work() throws Exception {
		seedUser("Root", "root@fornituras.local", Role.ADMIN, true);
		User user = seedUser("Fito", "fito@fornituras.local", Role.CAPTURISTA, true);

		mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/enabled")
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.enabled").value(false));

		mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/role")
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("ADMIN"));
	}
}
