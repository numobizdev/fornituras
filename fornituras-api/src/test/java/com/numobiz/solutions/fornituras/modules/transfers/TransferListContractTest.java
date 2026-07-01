package com.numobiz.solutions.fornituras.modules.transfers;

import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code GET /api/v1/transfers} (T021): listado paginado y filtros por estado y por
 * almacén origen; y {@code GET /api/v1/transfers/{id}} para la ficha.
 */
@WithMockUser(roles = "ADMIN")
class TransferListContractTest extends TransferApiTestSupport {

	@Test
	void list_isPaginated() throws Exception {
		seedTransfer(seed.origenId(), seed.destinoId(), TransferStatus.ENVIADO);
		seedTransfer(seed.origenId(), seed.destinoId(), TransferStatus.RECIBIDO);

		mockMvc.perform(get("/api/v1/transfers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	void list_filtersByStatus() throws Exception {
		seedTransfer(seed.origenId(), seed.destinoId(), TransferStatus.ENVIADO);
		seedTransfer(seed.origenId(), seed.destinoId(), TransferStatus.RECIBIDO);

		mockMvc.perform(get("/api/v1/transfers").param("status", "RECIBIDO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].status").value("RECIBIDO"));
	}

	@Test
	void list_filtersByOrigen() throws Exception {
		// Un traslado sale del origen; otro sale del destino (usado como origen inverso).
		seedTransfer(seed.origenId(), seed.destinoId(), TransferStatus.ENVIADO);
		seedTransfer(seed.destinoId(), seed.origenId(), TransferStatus.ENVIADO);

		mockMvc.perform(get("/api/v1/transfers").param("origenId", String.valueOf(seed.origenId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].origenId").value(seed.origenId()));
	}

	@Test
	void getById_returnsDetail() throws Exception {
		long id = seedTransfer(seed.origenId(), seed.destinoId(), TransferStatus.ENVIADO);

		mockMvc.perform(get("/api/v1/transfers/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id))
				.andExpect(jsonPath("$.data.status").value("ENVIADO"));
	}
}
