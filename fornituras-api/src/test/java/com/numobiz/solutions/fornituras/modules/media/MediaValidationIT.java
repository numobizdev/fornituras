package com.numobiz.solutions.fornituras.modules.media;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T014 (US1): la validación de contenido rechaza lo inseguro antes de almacenar nada — SVG y archivos
 * no-imagen renombrados dan 400 (magic bytes), y un archivo por encima del peso máximo da 413.
 */
class MediaValidationIT extends MediaApiTestSupport {

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void rejectsSvg() throws Exception {
		mockMvc.perform(multipart(MEDIA_URL).file(svgPart()).param("context", "equipment"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void rejectsNonImageRenamedToJpg() throws Exception {
		mockMvc.perform(multipart(MEDIA_URL).file(fakeJpegPart()).param("context", "equipment"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void rejectsOversizeWithPayloadTooLarge() throws Exception {
		mockMvc.perform(multipart(MEDIA_URL).file(oversizePart()).param("context", "equipment"))
				.andExpect(status().isPayloadTooLarge());
	}
}
