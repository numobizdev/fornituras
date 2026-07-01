package com.numobiz.solutions.fornituras.modules.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización del catálogo (T013): la consulta es para cualquier rol autenticado; la
 * administración (alta/edición/desactivación) se restringe a ADMIN. Un rol operativo que intenta
 * escribir queda denegado (403) por la capa de seguridad ({@code @PreAuthorize}) sin crear nada.
 */
class CatalogAuthTest extends CatalogApiTestSupport {

	@Test
	@WithMockUser(roles = "CONSULTA")
	void listItems_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CONSULTA")
	void createItem_withoutAdminRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("Casco")))
				.andExpect(status().isForbidden());

		assertThat(itemRepository.count()).isEqualTo(1);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createItem_withAdminRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("Casco")))
				.andExpect(status().isCreated());

		assertThat(itemRepository.count()).isEqualTo(2);
	}
}
