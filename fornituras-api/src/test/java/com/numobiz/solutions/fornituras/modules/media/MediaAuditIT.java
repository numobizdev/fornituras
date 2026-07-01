package com.numobiz.solutions.fornituras.modules.media;

import com.numobiz.solutions.fornituras.modules.audit.entity.AuditLog;
import com.numobiz.solutions.fornituras.modules.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T025 (US2): subir y visualizar la foto de un elemento generan registro de auditoría (quién, qué
 * media, cuándo), y la evidencia referencia el asset por id opaco — nunca PII.
 */
class MediaAuditIT extends MediaApiTestSupport {

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Test
	void uploadAndViewOfPiiPhotoAreAudited() throws Exception {
		String id = uploadOfficerPhotoAsAdmin();
		assertThat(auditCount("MEDIA_PII_UPLOAD")).isGreaterThanOrEqualTo(1);

		mockMvc.perform(get(MEDIA_URL + "/" + id).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		Page<AuditLog> viewEvents = auditLogRepository.search(
				null, "MEDIA_PII_VIEW", null, null, null, PageRequest.of(0, 10));
		assertThat(viewEvents.getTotalElements()).isGreaterThanOrEqualTo(1);
		// La evidencia referencia el media por id opaco y no contiene PII.
		AuditLog event = viewEvents.getContent().get(0);
		assertThat(event.getEvidencia()).contains("mediaId=").contains(id);
	}

	private long auditCount(String action) {
		return auditLogRepository.search(null, action, null, null, null, PageRequest.of(0, 10))
				.getTotalElements();
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
