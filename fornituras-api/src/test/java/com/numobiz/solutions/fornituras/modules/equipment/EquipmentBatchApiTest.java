package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alta por lote (T028 contrato + T029 integración): crea N fornituras con datos comunes; rechaza
 * duplicados intra-lote y contra la base; ante cualquier duplicado no persiste nada (atomicidad).
 */
@WithMockUser(roles = "ADMIN")
class EquipmentBatchApiTest extends EquipmentApiTestSupport {

	private String batchJson(List<String> codigos) throws Exception {
		return objectMapper.writeValueAsString(Map.of(
				"equipmentTypeId", seed.tipoPrendaId(),
				"sizeId", seed.tallaId(),
				"warehouseId", seed.warehouseId(),
				"descripcion", "Lote de prueba",
				"codigos", codigos));
	}

	@Test
	void batch_createsOnePerCode() throws Exception {
		mockMvc.perform(post("/api/v1/equipment/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchJson(List.of("FOR-1", "FOR-2", "FOR-3"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.length()").value(3));

		assertThat(equipmentRepository.count()).isEqualTo(3);
	}

	@Test
	void batch_rejectsDuplicateWithinLot() throws Exception {
		mockMvc.perform(post("/api/v1/equipment/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchJson(List.of("FOR-9", "for-9"))))
				.andExpect(status().isConflict());

		assertThat(equipmentRepository.count()).isZero();
	}

	@Test
	void batch_isAtomicWhenOneCodeAlreadyExists() throws Exception {
		persistEquipment("FOR-EXIST", EquipmentStatus.DISPONIBLE, null);

		mockMvc.perform(post("/api/v1/equipment/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(batchJson(List.of("FOR-NEW-1", "FOR-EXIST", "FOR-NEW-2"))))
				.andExpect(status().isConflict());

		// Ninguno del lote se creó: solo persiste la fornitura previa (rollback).
		assertThat(equipmentRepository.count()).isEqualTo(1);
		assertThat(equipmentRepository.existsByCodigoNormalizado("FORNEW1")).isFalse();
		assertThat(equipmentRepository.existsByCodigoNormalizado("FORNEW2")).isFalse();
	}
}
