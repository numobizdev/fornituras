package com.numobiz.solutions.fornituras.modules.dashboard;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SC-002: cada contador de {@code GET /dashboard/summary} coincide exactamente con el {@code COUNT}
 * del listado filtrado equivalente de 001 ({@code /equipment}) y de 008 ({@code /alerts/vigencia}).
 */
@WithMockUser(roles = "ADMIN")
class DashboardSummaryIntegrationTest extends DashboardApiTestSupport {

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void counters_matchEquivalentFilteredListings() throws Exception {
		LocalDate today = LocalDate.now();
		// Estados operativos.
		seedEquipment("FOR-D1", EquipmentStatus.DISPONIBLE, today.plusDays(200));
		seedEquipment("FOR-D2", EquipmentStatus.DISPONIBLE, null);
		seedEquipment("FOR-A1", EquipmentStatus.ASIGNADA, today.plusDays(200));
		seedEquipment("FOR-M1", EquipmentStatus.EN_MANTENIMIENTO, today.plusDays(200));
		seedEquipment("FOR-T1", EquipmentStatus.EN_TRASLADO, today.plusDays(200));
		// Vigencia (dentro de estados no dados de baja).
		seedEquipment("FOR-PROX", EquipmentStatus.DISPONIBLE, today.plusDays(30));   // próxima
		seedEquipment("FOR-CAD", EquipmentStatus.ASIGNADA, today.minusDays(1));      // caducada
		// Excluidas de vigencia: la baja definitiva no cuenta como caducada.
		seedEquipment("FOR-BAJA", EquipmentStatus.BAJA_DEFINITIVA, today.minusDays(5));

		JsonNode summary = data(perform("/api/v1/dashboard/summary"));

		assertThat(summary.get("total").asLong()).isEqualTo(totalElements("/api/v1/equipment"));
		assertThat(summary.get("disponibles").asLong())
				.isEqualTo(totalElements("/api/v1/equipment?status=DISPONIBLE"));
		assertThat(summary.get("asignadas").asLong())
				.isEqualTo(totalElements("/api/v1/equipment?status=ASIGNADA"));
		assertThat(summary.get("enMantenimiento").asLong())
				.isEqualTo(totalElements("/api/v1/equipment?status=EN_MANTENIMIENTO"));

		JsonNode alerts = data(perform("/api/v1/alerts/vigencia"));
		long expectedCaducadas = countExpiry(alerts, "CADUCADA");
		long expectedProximas = countExpiry(alerts, "PROXIMA_A_VENCER");
		assertThat(summary.get("caducadas").asLong()).isEqualTo(expectedCaducadas).isEqualTo(1);
		assertThat(summary.get("proximasAVencer").asLong()).isEqualTo(expectedProximas).isEqualTo(1);
	}

	private ResultActions perform(String url) throws Exception {
		return mockMvc.perform(get(url)).andExpect(status().isOk());
	}

	private JsonNode data(ResultActions result) throws Exception {
		String body = result.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("data");
	}

	private long totalElements(String url) throws Exception {
		return data(perform(url)).get("totalElements").asLong();
	}

	private long countExpiry(JsonNode alerts, String expiryStatus) {
		long count = 0;
		for (JsonNode alert : alerts) {
			if (expiryStatus.equals(alert.get("expiryStatus").asString())) {
				count++;
			}
		}
		return count;
	}
}
