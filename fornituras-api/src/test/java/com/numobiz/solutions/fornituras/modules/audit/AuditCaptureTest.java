package com.numobiz.solutions.fornituras.modules.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Captura automática (T008/SC-001): una operación sensible genera <b>exactamente 1</b> registro con
 * los campos requeridos. Se usa el acceso a la ficha de un elemento (VIEW_OFFICER), audita por id.
 */
@WithMockUser(username = "admin@fornituras.local", roles = "ADMIN")
class AuditCaptureTest extends AuditApiTestSupport {

	@Test
	void sensitiveOperation_writesExactlyOneRecord() throws Exception {
		long officerId = seedOfficer("Juan", "PLA-1");

		mockMvc.perform(get("/api/v1/officers/{id}", officerId)).andExpect(status().isOk());

		assertThat(countAudit("VIEW_OFFICER")).isEqualTo(1);

		Long entidadId = jdbcTemplate.queryForObject(
				"SELECT entidad_id FROM audit_log WHERE accion = 'VIEW_OFFICER'", Long.class);
		String actor = jdbcTemplate.queryForObject(
				"SELECT actor FROM audit_log WHERE accion = 'VIEW_OFFICER'", String.class);
		Object occurredAt = jdbcTemplate.queryForObject(
				"SELECT occurred_at FROM audit_log WHERE accion = 'VIEW_OFFICER'", Object.class);

		assertThat(entidadId).isEqualTo(officerId);
		assertThat(actor).isEqualTo("admin@fornituras.local");
		assertThat(occurredAt).isNotNull();
	}
}
