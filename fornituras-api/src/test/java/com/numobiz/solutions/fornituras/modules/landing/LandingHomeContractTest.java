package com.numobiz.solutions.fornituras.modules.landing;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code GET /api/v1/landing/home} (T014, US1): autenticado devuelve las secciones HOME
 * activas ordenadas; sin sesión responde 401. Las inactivas y las de otra cara no se incluyen.
 */
class LandingHomeContractTest extends LandingApiTestSupport {

	@Test
	@WithMockUser(roles = "CONSULTA")
	void home_authenticated_returnsActiveHomeSectionsOrdered() throws Exception {
		seedSection(LandingScope.HOME, LandingSectionType.ANNOUNCEMENT, "Aviso", 1, true);
		seedSection(LandingScope.HOME, LandingSectionType.HERO, "Bienvenido", 0, true);
		seedSection(LandingScope.HOME, LandingSectionType.ANNOUNCEMENT, "Oculto", 2, false);
		seedSection(LandingScope.PUBLIC, LandingSectionType.HERO, "Público", 0, true);

		mockMvc.perform(get("/api/v1/landing/home"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].titulo").value("Bienvenido"))
				.andExpect(jsonPath("$.data[1].titulo").value("Aviso"));
	}

	@Test
	void home_withoutSession_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/landing/home"))
				.andExpect(status().isUnauthorized());
	}
}
