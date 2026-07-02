package com.numobiz.solutions.fornituras.modules.media;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T024 (US2): la foto de elemento (PII) exige rol autorizado para verse — 200 con rol autorizado
 * (regla 3 de PII), 403 con rol no autorizado (enmascaramiento por defecto) y 401 sin sesión.
 */
class MediaPiiAuthzIT extends MediaApiTestSupport {

	@Test
	void authorizedRoleGetsPiiPhotoOthersForbiddenAnonymousUnauthorized() throws Exception {
		String id = uploadOfficerPhotoAsAdmin();

		// Rol autorizado a ver PII completa (ADMIN): 200.
		mockMvc.perform(get(MEDIA_URL + "/" + id).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Rol autenticado pero sin autorización de PII (ALMACEN): 403 (enmascarado por defecto).
		mockMvc.perform(get(MEDIA_URL + "/" + id).with(user("almacen").roles("ALMACEN")))
				.andExpect(status().isForbidden());

		// Sin sesión: 401.
		mockMvc.perform(get(MEDIA_URL + "/" + id))
				.andExpect(status().isUnauthorized());
	}

	private String uploadOfficerPhotoAsAdmin() throws Exception {
		MvcResult result = mockMvc.perform(multipart(MEDIA_URL)
						.file(jpegPart())
						.param("context", "officer")
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
		return data.get("id").asString();
	}
}
