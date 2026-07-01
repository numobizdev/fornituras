package com.numobiz.solutions.fornituras.modules.landing;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD de {@code /api/v1/landing/sections} (T019, US2): la edición es solo ADMIN (403 en otro rol),
 * el alta/edición válidas responden 201/200, y la validación en el borde rechaza (400) URLs peligrosas,
 * accesos rápidos ausentes en QUICK_LINKS y longitudes excedidas. El contenido se guarda literal.
 */
class LandingSectionCrudTest extends LandingApiTestSupport {

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void create_withoutAdminRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/landing/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content(heroJson("Bienvenido")))
				.andExpect(status().isForbidden());

		assertThat(repository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void listSections_withoutAdminRole_isForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/landing/sections").param("scope", "HOME"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_validHero_returns201AndPersists() throws Exception {
		mockMvc.perform(post("/api/v1/landing/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content(heroJson("Bienvenido")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.type").value("HERO"))
				.andExpect(jsonPath("$.data.active").value(true))
				.andExpect(jsonPath("$.data.titulo").value("Bienvenido"));

		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void update_existingSection_returns200() throws Exception {
		long id = seedSection(LandingScope.HOME, LandingSectionType.HERO, "Original", 0, true);

		String body = """
				{"scope":"HOME","type":"HERO","titulo":"Editado","orden":0}
				""";
		mockMvc.perform(put("/api/v1/landing/sections/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.titulo").value("Editado"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void deactivate_existingSection_returns200AndInactive() throws Exception {
		long id = seedSection(LandingScope.HOME, LandingSectionType.HERO, "Baja", 0, true);

		mockMvc.perform(patch("/api/v1/landing/sections/{id}/deactivate", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(false));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_dangerousUrl_returns400() throws Exception {
		String body = """
				{"scope":"PUBLIC","type":"HERO","titulo":"X","imagenUrl":"javascript:alert(1)","orden":0}
				""";
		mockMvc.perform(post("/api/v1/landing/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(repository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_quickLinksWithoutItems_returns400() throws Exception {
		String body = """
				{"scope":"HOME","type":"QUICK_LINKS","titulo":"Accesos","orden":0}
				""";
		mockMvc.perform(post("/api/v1/landing/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(repository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_tituloTooLong_returns400() throws Exception {
		String tooLong = "A".repeat(161);
		String body = "{\"scope\":\"HOME\",\"type\":\"HERO\",\"titulo\":\"" + tooLong + "\",\"orden\":0}";
		mockMvc.perform(post("/api/v1/landing/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());

		assertThat(repository.count()).isZero();
	}

	private static String heroJson(String titulo) {
		return "{\"scope\":\"HOME\",\"type\":\"HERO\",\"titulo\":\"" + titulo + "\",\"orden\":0}";
	}
}
