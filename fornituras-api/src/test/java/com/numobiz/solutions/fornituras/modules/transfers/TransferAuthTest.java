package com.numobiz.solutions.fornituras.modules.transfers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización de {@code /transfers} (T007, matriz ADR 0013): la consulta es para cualquier rol
 * autenticado; crear, recibir y cancelar quedan restringidos a {@code WRITE_TRANSFERS}
 * (ADMIN/SUPERVISOR/ALMACEN/CAPTURISTA). Un rol solo-consulta que intenta crear queda denegado (403)
 * sin persistir nada.
 */
class TransferAuthTest extends TransferApiTestSupport {

	@Test
	@WithMockUser(roles = "CONSULTA")
	void list_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/transfers")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CONSULTA")
	void create_withoutWriteRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(), List.of(seed.equipment1Id()))))
				.andExpect(status().isForbidden());

		assertThat(transferRepository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void create_withCapturistaRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(), List.of(seed.equipment1Id()))))
				.andExpect(status().isCreated());

		assertThat(transferRepository.count()).isEqualTo(1);
	}

	@Test
	@WithMockUser(roles = "ALMACEN")
	void create_withAlmacenRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/transfers")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson(seed.origenId(), seed.destinoId(), List.of(seed.equipment1Id()))))
				.andExpect(status().isCreated());

		assertThat(transferRepository.count()).isEqualTo(1);
	}
}
