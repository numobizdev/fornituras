package com.numobiz.solutions.fornituras.modules.incidents;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentCreateRequest;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;
import com.numobiz.solutions.fornituras.modules.incidents.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de {@code PATCH /api/v1/incidents/{id}} (T020): resolver una incidencia registra la
 * transición y devuelve la fornitura a DISPONIBLE si seguía retirada por ella; un id inexistente da
 * 404. La incidencia de partida se crea con el servicio real (deja la fornitura EN_MANTENIMIENTO).
 */
@WithMockUser(roles = "ADMIN")
class IncidentUpdateTest extends IncidentApiTestSupport {

	@Autowired
	private IncidentService service;

	@Test
	void resolve_returnsEquipmentToDisponible() throws Exception {
		long incidentId = service.report(
				new IncidentCreateRequest(seed.equipmentId(), IncidentType.DANO, "Correa rota")).id();
		assertThat(equipmentRepository.findById(seed.equipmentId()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.EN_MANTENIMIENTO);

		mockMvc.perform(patch("/api/v1/incidents/{id}", incidentId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"RESUELTA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.estado").value("RESUELTA"))
				.andExpect(jsonPath("$.data.fechaResolucion").isNotEmpty());

		assertThat(equipmentRepository.findById(seed.equipmentId()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.DISPONIBLE);
	}

	@Test
	void update_unknownId_returns404() throws Exception {
		mockMvc.perform(patch("/api/v1/incidents/{id}", 999999L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"RESUELTA\"}"))
				.andExpect(status().isNotFound());
	}
}
