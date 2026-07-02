package com.numobiz.solutions.fornituras.modules.media;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T015 (US1): descargar un asset no-PII autenticado devuelve 200 con el content-type correcto; sin
 * sesión devuelve 401 (nunca acceso anónimo a las fotos).
 */
class MediaDownloadIT extends MediaApiTestSupport {

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void downloadsNonPiiImageAuthenticated() throws Exception {
		String id = uploadEquipmentPhoto();

		mockMvc.perform(get(MEDIA_URL + "/" + id))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));
	}

	@Test
	void downloadWithoutSessionIsUnauthorized() throws Exception {
		// Sin @WithMockUser el request es anónimo: el asset ni siquiera hace falta, /media/** exige sesión.
		mockMvc.perform(get(MEDIA_URL + "/00000000-0000-0000-0000-000000000000"))
				.andExpect(status().isUnauthorized());
	}

	/** Sube una foto de equipo bajo el contexto de seguridad del test que llama (CAPTURISTA). */
	private String uploadEquipmentPhoto() throws Exception {
		MvcResult result = mockMvc.perform(multipart(MEDIA_URL)
						.file(jpegPart())
						.param("context", "equipment"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
		return data.get("id").asString();
	}
}
