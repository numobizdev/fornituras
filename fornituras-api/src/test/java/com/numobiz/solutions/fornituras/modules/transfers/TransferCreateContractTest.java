package com.numobiz.solutions.fornituras.modules.transfers;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code POST /api/v1/transfers} (T009/T010): alta válida (201, estado ENVIADO, fornituras
 * a EN_TRASLADO), origen == destino (400), fornitura no disponible (409), fornitura fuera del origen
 * (409), destino inactivo (400) y lista de fornituras vacía (400).
 */
@WithMockUser(roles = "ADMIN")
class TransferCreateContractTest extends TransferApiTestSupport {

	@Test
	void create_validRequest_returns201AndMovesEquipmentIntoTransit() throws Exception {
		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(),
								List.of(seed.equipment1Id(), seed.equipment2Id()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("ENVIADO"))
				.andExpect(jsonPath("$.data.items.length()").value(2));

		assertThat(equipmentRepository.findById(seed.equipment1Id()).orElseThrow().getStatus())
				.isEqualTo(EquipmentStatus.EN_TRASLADO);
	}

	@Test
	void create_sameOrigenAndDestino_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.origenId(), List.of(seed.equipment1Id()))))
				.andExpect(status().isBadRequest());

		assertThat(transferRepository.count()).isZero();
	}

	@Test
	void create_unavailableEquipment_returns409() throws Exception {
		long asignada = seedEquipment("FOR-ASG", seed.origenId(), EquipmentStatus.ASIGNADA);

		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(), List.of(asignada))))
				.andExpect(status().isConflict());
	}

	@Test
	void create_equipmentNotInOrigen_returns409() throws Exception {
		long enDestino = seedEquipment("FOR-DES", seed.destinoId(), EquipmentStatus.DISPONIBLE);

		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(), List.of(enDestino))))
				.andExpect(status().isConflict());
	}

	@Test
	void create_inactiveDestino_returns400() throws Exception {
		long inactivo = seedWarehouse("ALM-OFF", "Almacén Inactivo", false);

		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), inactivo, List.of(seed.equipment1Id()))))
				.andExpect(status().isBadRequest());

		assertThat(transferRepository.count()).isZero();
	}

	@Test
	void create_emptyEquipmentList_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(), List.of())))
				.andExpect(status().isBadRequest());

		assertThat(transferRepository.count()).isZero();
	}
}
