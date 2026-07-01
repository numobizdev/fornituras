package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de la vista de control (T008): totales y asignaciones activas con paginación y filtros
 * (QR, nombre, placa, CURP, RFC, municipio).
 */
@WithMockUser(roles = "ADMIN")
class ReportQueryContractTest extends ReportApiTestSupport {

	@Test
	void totals_returnsCountsAndElementCount() throws Exception {
		seedEquipment("FOR-D1", EquipmentStatus.DISPONIBLE);
		seedEquipment("FOR-A1", EquipmentStatus.ASIGNADA);
		seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");

		mockMvc.perform(get("/api/v1/reports/totals"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalFornituras").value(2))
				.andExpect(jsonPath("$.data.disponibles").value(1))
				.andExpect(jsonPath("$.data.asignadas").value(1))
				.andExpect(jsonPath("$.data.totalElementos").value(1));
	}

	@Test
	void activeAssignments_paginatesAndFiltersByMunicipio() throws Exception {
		long e1 = seedEquipment("FOR-1", EquipmentStatus.ASIGNADA);
		long e2 = seedEquipment("FOR-2", EquipmentStatus.ASIGNADA);
		long a = seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");
		long b = seedOfficer("Beto", "Ruiz", "PLA-2", "CURP020202HDFXYZ02", "RUBB020202BBB", "Zapopan");
		seedActiveAssignment(e1, a);
		seedActiveAssignment(e2, b);

		mockMvc.perform(get("/api/v1/reports/active-assignments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2));

		mockMvc.perform(get("/api/v1/reports/active-assignments").param("municipio", "Guadalajara"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].placa").value("PLA-1"));

		mockMvc.perform(get("/api/v1/reports/active-assignments").param("placa", "PLA-2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].elementoNombre").value("Beto Ruiz"));

		mockMvc.perform(get("/api/v1/reports/active-assignments")
						.param("size", "1").param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content.length()").value(1));
	}
}
