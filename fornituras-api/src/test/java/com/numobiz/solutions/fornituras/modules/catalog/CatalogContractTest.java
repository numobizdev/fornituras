package com.numobiz.solutions.fornituras.modules.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato del CRUD genérico de catálogos (T011): listado paginado con filtro {@code active},
 * alta (201), edición (200), desactivación (200), duplicado de nombre por catálogo (409),
 * catálogo inexistente (404), validación de nombre en blanco (400) y rechazo de foto con esquema
 * inseguro (400, T028). La escritura exige rol ADMIN.
 */
@WithMockUser(roles = "ADMIN")
class CatalogContractTest extends CatalogApiTestSupport {

	@Test
	void listItems_isPaginatedAndFiltersByActive() throws Exception {
		seedItem(catalogRepository.findById(seed.tipoPrendaCatalogId()).orElseThrow(),
				"Descontinuada", null, false);

		mockMvc.perform(get("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(2));

		mockMvc.perform(get("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.param("active", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].nombre").value("Descontinuada"));
	}

	@Test
	void createItem_validRequest_returns201() throws Exception {
		mockMvc.perform(post("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("Casco")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.nombre").value("Casco"));

		assertThat(itemRepository.count()).isEqualTo(2);
	}

	@Test
	void createItem_duplicateNameInSameCatalog_returns409() throws Exception {
		// "fornitura" (distinto formato) normaliza igual que el valor sembrado "Fornitura".
		mockMvc.perform(post("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("  FORNITURA ")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));

		assertThat(itemRepository.count()).isEqualTo(1);
	}

	@Test
	void createItem_unknownCatalogCode_returns404() throws Exception {
		mockMvc.perform(post("/api/v1/catalogs/{code}/items", "NO_EXISTE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("X")))
				.andExpect(status().isNotFound());
	}

	@Test
	void createItem_blankName_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("   ")))
				.andExpect(status().isBadRequest());

		assertThat(itemRepository.count()).isEqualTo(1);
	}

	@Test
	void createItem_unsafeFotoScheme_returns400() throws Exception {
		String json = objectMapper.writeValueAsString(Map.of(
				"nombre", "Chaleco",
				"fotoUrl", "javascript:alert(1)"));

		mockMvc.perform(post("/api/v1/catalogs/{code}/items", CatalogCodes.TIPO_PRENDA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest());

		assertThat(itemRepository.count()).isEqualTo(1);
	}

	@Test
	void updateItem_persistsNewName_returns200() throws Exception {
		mockMvc.perform(put("/api/v1/catalogs/items/{itemId}", seed.fornituraItemId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemJson("Fornitura táctica")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nombre").value("Fornitura táctica"));

		assertThat(itemRepository.findById(seed.fornituraItemId()).orElseThrow().getNombre())
				.isEqualTo("Fornitura táctica");
	}

	@Test
	void deactivateItem_marksInactive_returns200() throws Exception {
		mockMvc.perform(patch("/api/v1/catalogs/items/{itemId}/deactivate", seed.fornituraItemId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		assertThat(itemRepository.findById(seed.fornituraItemId()).orElseThrow().isActive()).isFalse();
	}
}
