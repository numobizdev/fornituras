package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reportes predefinidos (T020): {@code GET /reports/predefined/{tipo}} entrega los datos correctos
 * por tipo, paginados.
 */
@WithMockUser(roles = "ADMIN")
class PredefinedReportsTest extends ReportApiTestSupport {

	@Test
	void predefinedReports_returnCorrectDataByType() throws Exception {
		seedEquipment("FOR-D1", EquipmentStatus.DISPONIBLE);
		seedEquipment("FOR-D2", EquipmentStatus.DISPONIBLE);
		seedEquipment("FOR-A1", EquipmentStatus.ASIGNADA);

		mockMvc.perform(get("/api/v1/reports/predefined/INVENTARIO_GENERAL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(3));

		mockMvc.perform(get("/api/v1/reports/predefined/DISPONIBLES"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2));

		mockMvc.perform(get("/api/v1/reports/predefined/ASIGNADAS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void predefinedReport_isPaginated() throws Exception {
		seedEquipment("FOR-D1", EquipmentStatus.DISPONIBLE);
		seedEquipment("FOR-D2", EquipmentStatus.DISPONIBLE);

		mockMvc.perform(get("/api/v1/reports/predefined/DISPONIBLES").param("size", "1").param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content.length()").value(1));
	}
}
