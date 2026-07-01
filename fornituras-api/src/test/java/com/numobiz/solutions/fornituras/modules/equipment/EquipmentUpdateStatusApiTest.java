package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Edición y cambio de estado (T034 contrato + T035 integración): edición no identitaria; el código
 * es inmutable (409); transición de estado válida; bloqueo de baja/traslado con asignación vigente
 * a través del puerto real {@code EquipmentLifecycleQuery}.
 */
@WithMockUser(roles = "ADMIN")
class EquipmentUpdateStatusApiTest extends EquipmentApiTestSupport {

	private String updateJson(String codigo, String descripcion) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("codigoQr", codigo);
		body.put("equipmentTypeId", seed.tipoPrendaId());
		body.put("sizeId", seed.tallaId());
		body.put("warehouseId", seed.warehouseId());
		body.put("descripcion", descripcion);
		return objectMapper.writeValueAsString(body);
	}

	private String statusJson(EquipmentStatus status) throws Exception {
		return objectMapper.writeValueAsString(Map.of("status", status.name()));
	}

	@Test
	void update_editsNonIdentityAttributes() throws Exception {
		long id = persistEquipment("FOR-200", EquipmentStatus.DISPONIBLE, null);

		mockMvc.perform(put("/api/v1/equipment/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateJson("FOR-200", "Descripción corregida")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.descripcion").value("Descripción corregida"));

		assertThat(equipmentRepository.findById(id)).get()
				.extracting(e -> e.getDescripcion())
				.isEqualTo("Descripción corregida");
	}

	@Test
	void update_codeIsImmutable() throws Exception {
		long id = persistEquipment("FOR-201", EquipmentStatus.DISPONIBLE, null);

		mockMvc.perform(put("/api/v1/equipment/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(updateJson("FOR-999", "otro")))
				.andExpect(status().isConflict());

		assertThat(equipmentRepository.findByCodigoNormalizado("FOR201")).isPresent();
		assertThat(equipmentRepository.existsByCodigoNormalizado("FOR999")).isFalse();
	}

	@Test
	void changeStatus_validTransition() throws Exception {
		long id = persistEquipment("FOR-210", EquipmentStatus.DISPONIBLE, null);

		mockMvc.perform(patch("/api/v1/equipment/{id}/status", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(statusJson(EquipmentStatus.EN_MANTENIMIENTO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("EN_MANTENIMIENTO"));

		assertThat(equipmentRepository.findById(id)).get()
				.extracting(e -> e.getStatus())
				.isEqualTo(EquipmentStatus.EN_MANTENIMIENTO);
	}

	@Test
	void changeStatus_bajaBlockedByActiveAssignment() throws Exception {
		long id = persistEquipment("FOR-220", EquipmentStatus.ASIGNADA, null);
		seedActiveAssignment(id);

		mockMvc.perform(patch("/api/v1/equipment/{id}/status", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(statusJson(EquipmentStatus.BAJA_DEFINITIVA)))
				.andExpect(status().isConflict());

		assertThat(equipmentRepository.findById(id)).get()
				.extracting(e -> e.getStatus())
				.isEqualTo(EquipmentStatus.ASIGNADA);
	}

	@Test
	void changeStatus_trasladoBlockedByActiveAssignment() throws Exception {
		long id = persistEquipment("FOR-221", EquipmentStatus.ASIGNADA, null);
		seedActiveAssignment(id);

		mockMvc.perform(patch("/api/v1/equipment/{id}/status", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(statusJson(EquipmentStatus.EN_TRASLADO)))
				.andExpect(status().isConflict());
	}
}
