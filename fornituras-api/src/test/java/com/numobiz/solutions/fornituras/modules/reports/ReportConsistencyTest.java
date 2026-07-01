package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consistencia y PII (T009): los totales del reporte coinciden con el tablero 010 (SC-001) y las
 * filas enmascaran CURP/RFC para roles no autorizados (FR-006).
 */
class ReportConsistencyTest extends ReportApiTestSupport {

	@Test
	@WithMockUser(roles = "ADMIN")
	void totals_matchDashboardSummary() throws Exception {
		seedEquipment("FOR-D1", EquipmentStatus.DISPONIBLE);
		seedEquipment("FOR-D2", EquipmentStatus.DISPONIBLE);
		seedEquipment("FOR-A1", EquipmentStatus.ASIGNADA);
		seedEquipment("FOR-M1", EquipmentStatus.EN_MANTENIMIENTO);

		JsonNode totals = data(get("/api/v1/reports/totals"));
		JsonNode summary = data(get("/api/v1/dashboard/summary"));

		assertThat(totals.get("disponibles").asLong()).isEqualTo(summary.get("disponibles").asLong());
		assertThat(totals.get("asignadas").asLong()).isEqualTo(summary.get("asignadas").asLong());
		assertThat(totals.get("enMantenimiento").asLong()).isEqualTo(summary.get("enMantenimiento").asLong());
		assertThat(totals.get("totalFornituras").asLong()).isEqualTo(summary.get("total").asLong());
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void activeAssignments_maskPiiForNonAdmin() throws Exception {
		long e = seedEquipment("FOR-1", EquipmentStatus.ASIGNADA);
		long o = seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");
		seedActiveAssignment(e, o);

		mockMvc.perform(get("/api/v1/reports/active-assignments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].piiMasked").value(true))
				.andExpect(jsonPath("$.data.content[0].curp").value(containsString("•")))
				.andExpect(jsonPath("$.data.content[0].elementoNombre").value("Ana López"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void activeAssignments_showPiiForAdmin() throws Exception {
		long e = seedEquipment("FOR-1", EquipmentStatus.ASIGNADA);
		long o = seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");
		seedActiveAssignment(e, o);

		mockMvc.perform(get("/api/v1/reports/active-assignments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].piiMasked").value(false))
				.andExpect(jsonPath("$.data.content[0].curp").value("CURP010101MDFXYZ01"));
	}

	private JsonNode data(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
			throws Exception {
		String body = mockMvc.perform(request).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("data");
	}
}
