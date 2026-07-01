package com.numobiz.solutions.fornituras.modules.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de la consulta (T015): {@code GET /audit} pagina y filtra por acción.
 */
@WithMockUser(roles = "ADMIN")
class AuditQueryContractTest extends AuditApiTestSupport {

	@Test
	void query_paginatesAndFiltersByAccion() throws Exception {
		auditWriter.record("CREATE_EQUIPMENT", 1L);
		auditWriter.record("CREATE_EQUIPMENT", 2L);
		auditWriter.recordEvent("EXPORT_REPORT", "report=ACTIVE_ASSIGNMENTS");

		mockMvc.perform(get("/api/v1/audit"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(3));

		mockMvc.perform(get("/api/v1/audit").param("accion", "CREATE_EQUIPMENT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2));

		mockMvc.perform(get("/api/v1/audit").param("size", "1").param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(3))
				.andExpect(jsonPath("$.data.content.length()").value(1));
	}
}
