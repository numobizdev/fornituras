package com.numobiz.solutions.fornituras.modules.incidents;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alertas de vigencia derivadas (T024): {@code GET /api/v1/alerts/vigencia} clasifica las próximas a
 * vencer (≤ 90 días) y las caducadas con el umbral compartido con 001/010, ordena las críticas
 * primero y excluye las vigentes, sin fecha y las de baja definitiva.
 */
@WithMockUser(roles = "CONSULTA")
class AlertVigenciaTest extends IncidentApiTestSupport {

	@Test
	void vigencia_classifiesNearAndExpired_criticalFirst() throws Exception {
		LocalDate today = LocalDate.now();
		seedEquipment("FOR-PROX", EquipmentStatus.DISPONIBLE, today.plusDays(30));   // próxima → naranja
		seedEquipment("FOR-CAD", EquipmentStatus.DISPONIBLE, today.minusDays(1));    // caducada → roja
		seedEquipment("FOR-VIG", EquipmentStatus.DISPONIBLE, today.plusDays(200));   // vigente → excluida
		seedEquipment("FOR-SINF", EquipmentStatus.DISPONIBLE, null);                 // sin fecha → excluida
		seedEquipment("FOR-BAJA", EquipmentStatus.BAJA_DEFINITIVA, today.minusDays(5)); // baja → excluida

		mockMvc.perform(get("/api/v1/alerts/vigencia"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].expiryStatus").value("CADUCADA"))
				.andExpect(jsonPath("$.data[0].equipmentCodigo").value("FOR-CAD"))
				.andExpect(jsonPath("$.data[1].expiryStatus").value("PROXIMA_A_VENCER"));
	}
}
