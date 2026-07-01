package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consulta de fornituras (T021 contrato + T022 integración): paginación, filtro por estado,
 * resolución server-side por código y vigencia derivada en la respuesta.
 */
@WithMockUser(roles = "CONSULTA")
class EquipmentListApiTest extends EquipmentApiTestSupport {

	@Test
	void list_isPaged() throws Exception {
		persistEquipment("FOR-001", EquipmentStatus.DISPONIBLE, null);
		persistEquipment("FOR-002", EquipmentStatus.DISPONIBLE, null);
		persistEquipment("FOR-003", EquipmentStatus.DISPONIBLE, null);

		mockMvc.perform(get("/api/v1/equipment").param("page", "0").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(2))
				.andExpect(jsonPath("$.data.totalElements").value(3));
	}

	@Test
	void list_filtersByStatus() throws Exception {
		persistEquipment("FOR-010", EquipmentStatus.DISPONIBLE, null);
		persistEquipment("FOR-011", EquipmentStatus.DISPONIBLE, null);
		persistEquipment("FOR-012", EquipmentStatus.EN_MANTENIMIENTO, null);

		mockMvc.perform(get("/api/v1/equipment").param("status", "EN_MANTENIMIENTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(1))
				.andExpect(jsonPath("$.data.content[0].codigoQr").value("FOR-012"))
				.andExpect(jsonPath("$.data.content[0].status").value("EN_MANTENIMIENTO"));
	}

	@Test
	void byCodigo_resolvesServerSide() throws Exception {
		persistEquipment("FOR-020", EquipmentStatus.DISPONIBLE, null);

		// El código se resuelve normalizado, aunque se consulte con otro formato.
		mockMvc.perform(get("/api/v1/equipment/by-codigo/{codigo}", "for-020"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.codigoQr").value("FOR-020"));
	}

	@Test
	void byCodigo_unknownReturns404() throws Exception {
		mockMvc.perform(get("/api/v1/equipment/by-codigo/{codigo}", "NOPE-999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void list_derivesExpiryStatus() throws Exception {
		persistEquipment("FOR-030", EquipmentStatus.DISPONIBLE, LocalDate.now().plusDays(30));
		persistEquipment("FOR-031", EquipmentStatus.DISPONIBLE, LocalDate.now().minusDays(1));
		persistEquipment("FOR-032", EquipmentStatus.DISPONIBLE, LocalDate.now().plusYears(2));

		mockMvc.perform(get("/api/v1/equipment").param("q", "FOR-030"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].vigencia").value("PROXIMA_A_VENCER"));

		mockMvc.perform(get("/api/v1/equipment").param("q", "FOR-031"))
				.andExpect(jsonPath("$.data.content[0].vigencia").value("CADUCADA"));

		mockMvc.perform(get("/api/v1/equipment").param("q", "FOR-032"))
				.andExpect(jsonPath("$.data.content[0].vigencia").value("VIGENTE"));
	}
}
