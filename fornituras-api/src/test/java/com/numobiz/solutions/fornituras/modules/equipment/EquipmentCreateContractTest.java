package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code POST /api/v1/equipment} (T012): alta válida (201, estado inicial disponible),
 * duplicado normalizado (409), validación de borde (400) y catálogo inactivo (400).
 */
@WithMockUser(roles = "ADMIN")
class EquipmentCreateContractTest extends EquipmentApiTestSupport {

	@Test
	void create_validRequest_persistsDisponible() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-001")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("DISPONIBLE"));

		assertThat(equipmentRepository.existsByCodigoNormalizado("FOR001")).isTrue();
	}

	@Test
	void create_duplicateNormalizedCode_returns409() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-002")))
				.andExpect(status().isCreated());

		// Mismo código con distinto formato (espacios/minúsculas) → mismo normalizado → rechazo.
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("  for-002 ")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));

		assertThat(equipmentRepository.count()).isEqualTo(1);
	}

	@Test
	void create_blankCode_returns400() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of(
				"codigoQr", "",
				"equipmentTypeId", seed.tipoPrendaId(),
				"warehouseId", seed.warehouseId()));

		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest());

		assertThat(equipmentRepository.count()).isZero();
	}

	@Test
	void create_inactiveType_returns400() throws Exception {
		long inactiveType = seedInactiveTipoPrenda();
		String json = objectMapper.writeValueAsString(Map.of(
				"codigoQr", "FOR-003",
				"equipmentTypeId", inactiveType,
				"warehouseId", seed.warehouseId()));

		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest());

		assertThat(equipmentRepository.count()).isZero();
	}

	@Test
	void create_initialStatusIsDisponible() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-004")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.findByCodigoNormalizado("FOR004"))
				.get()
				.extracting(e -> e.getStatus())
				.isEqualTo(EquipmentStatus.DISPONIBLE);
	}
}
