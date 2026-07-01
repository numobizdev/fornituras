package com.numobiz.solutions.fornituras.modules.incidents;

import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code GET /api/v1/incidents} (T016): listado paginado y filtro por estado; y
 * {@code GET /api/v1/incidents/{id}} para la ficha.
 */
@WithMockUser(roles = "ADMIN")
class IncidentListContractTest extends IncidentApiTestSupport {

	@Test
	void list_isPaginated() throws Exception {
		seedIncident(seed.equipmentId(), IncidentType.DANO, IncidentStatus.ABIERTA);
		seedIncident(seed.equipmentId(), IncidentType.FALLA, IncidentStatus.RESUELTA);

		mockMvc.perform(get("/api/v1/incidents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	void list_filtersByEstado() throws Exception {
		seedIncident(seed.equipmentId(), IncidentType.DANO, IncidentStatus.ABIERTA);
		seedIncident(seed.equipmentId(), IncidentType.FALLA, IncidentStatus.RESUELTA);

		mockMvc.perform(get("/api/v1/incidents").param("estado", "RESUELTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].estado").value("RESUELTA"));
	}

	@Test
	void getById_returnsDetailWithEquipmentCodigo() throws Exception {
		long id = seedIncident(seed.equipmentId(), IncidentType.DANO, IncidentStatus.ABIERTA);

		mockMvc.perform(get("/api/v1/incidents/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id))
				.andExpect(jsonPath("$.data.equipmentCodigo").value("FOR-I1"));
	}
}
