package com.numobiz.solutions.fornituras.modules.incidents;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del reporte de incidencias (T010): un reporte de daño/falla/mantenimiento retira la
 * fornitura a EN_MANTENIMIENTO; uno de extravío la marca EXTRAVIADA; y una fornitura bajo custodia
 * activa (EN_TRASLADO) no se altera al reportar (se registra la incidencia sin tocar su estado).
 */
@WithMockUser(roles = "ADMIN")
class IncidentCreateIntegrationTest extends IncidentApiTestSupport {

	@Test
	void report_dano_movesEquipmentToMaintenance() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), "DANO", "Placa dañada")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.findById(seed.equipmentId()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.EN_MANTENIMIENTO);
	}

	@Test
	void report_extravio_marksEquipmentExtraviada() throws Exception {
		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(seed.equipmentId(), "EXTRAVIO", "No se localiza")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.findById(seed.equipmentId()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.EXTRAVIADA);
	}

	@Test
	void report_onEquipmentInTransit_doesNotChangeStatus() throws Exception {
		long enTraslado = seedEquipment("FOR-TR", EquipmentStatus.EN_TRASLADO, null);

		mockMvc.perform(post("/api/v1/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(reportJson(enTraslado, "DANO", "Reportada durante traslado")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.findById(enTraslado).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.EN_TRASLADO);
		assertThat(incidentRepository.count()).isEqualTo(1);
	}
}
