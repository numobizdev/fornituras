package com.numobiz.solutions.fornituras.modules.media;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T026 (US2): con la captura de foto de elemento restringida (gate ADR 0003 desactivado, FR-015),
 * {@code POST /media} con {@code context=officer} responde 403 aunque el rol sí pueda capturar padrón.
 */
@TestPropertySource(properties = "app.media.officer-photo-enabled=false")
class MediaGatingIT extends MediaApiTestSupport {

	@Test
	void officerPhotoUploadForbiddenWhenGateDisabled() throws Exception {
		mockMvc.perform(multipart(MEDIA_URL)
						.file(jpegPart())
						.param("context", "officer")
						.with(user("admin").roles("ADMIN")))
				.andExpect(status().isForbidden());
	}
}
