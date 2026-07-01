package com.numobiz.solutions.fornituras.modules.users;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Anti-fuerza-bruta (T021, FR-005): tras varios intentos fallidos consecutivos la cuenta se bloquea
 * temporalmente y el login responde 429, incluso con la contraseña correcta, hasta que expire el bloqueo.
 */
class LoginLockoutTest extends UserAdminTestSupport {

	private static final int MAX_ATTEMPTS = 5;

	@Test
	void accountLocks_afterTooManyFailedAttempts() throws Exception {
		seedUser("Locky", "locky@fornituras.local", Role.ADMIN, true);

		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			mockMvc.perform(login("locky@fornituras.local", "wrong-password"))
					.andExpect(status().isUnauthorized());
		}

		// Superado el umbral, la cuenta queda bloqueada: incluso la contraseña correcta responde 429.
		mockMvc.perform(login("locky@fornituras.local", "Secret123#"))
				.andExpect(status().isTooManyRequests());
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
			String email, String password) throws Exception {
		String body = objectMapper.writeValueAsString(
				new com.numobiz.solutions.fornituras.modules.auth.dto.LoginRequestDTO(email, password));
		return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body);
	}
}
