package com.numobiz.solutions.fornituras.modules.transfers;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferCreateRequest;
import com.numobiz.solutions.fornituras.modules.transfers.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato del ciclo de vida de un traslado (T017): recibir un traslado enviado lo pasa a RECIBIDO y
 * deja las fornituras disponibles en el destino; recibir uno que no está en curso da 409; cancelar
 * devuelve las fornituras al origen; un id inexistente da 404. El traslado de partida se crea con el
 * servicio real para tener estado consistente (fornituras EN_TRASLADO).
 */
@WithMockUser(roles = "ADMIN")
class TransferLifecycleContractTest extends TransferApiTestSupport {

	@Autowired
	private TransferService service;

	private long createEnviado() {
		return service.create(new TransferCreateRequest(
				seed.origenId(), seed.destinoId(), List.of(seed.equipment1Id()), null)).id();
	}

	@Test
	void receive_marksRecibidoAndReleasesEquipmentInDestino() throws Exception {
		long id = createEnviado();

		mockMvc.perform(post("/api/v1/transfers/{id}/receive", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RECIBIDO"));

		var equipment = equipmentRepository.findById(seed.equipment1Id()).orElseThrow();
		assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.DISPONIBLE);
		assertThat(equipment.getWarehouseId()).isEqualTo(seed.destinoId());
	}

	@Test
	void receive_alreadyReceived_returns409() throws Exception {
		long id = createEnviado();
		mockMvc.perform(post("/api/v1/transfers/{id}/receive", id)).andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/transfers/{id}/receive", id))
				.andExpect(status().isConflict());
	}

	@Test
	void cancel_returnsEquipmentToOrigen() throws Exception {
		long id = createEnviado();

		mockMvc.perform(post("/api/v1/transfers/{id}/cancel", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CANCELADO"));

		var equipment = equipmentRepository.findById(seed.equipment1Id()).orElseThrow();
		assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.DISPONIBLE);
		assertThat(equipment.getWarehouseId()).isEqualTo(seed.origenId());
	}

	@Test
	void receive_unknownId_returns404() throws Exception {
		mockMvc.perform(post("/api/v1/transfers/{id}/receive", 999999L))
				.andExpect(status().isNotFound());
	}
}
