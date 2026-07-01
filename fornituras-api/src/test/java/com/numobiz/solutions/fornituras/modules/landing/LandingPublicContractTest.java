package com.numobiz.solutions.fornituras.modules.landing;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code GET /api/v1/landing/public} (T026, US3): accesible sin sesión, devuelve solo las
 * secciones PUBLIC activas y <b>sin PII</b> (la proyección excluye id, scope y banderas internas). El
 * endpoint está limitado por tasa: al exceder el cupo por IP responde 429.
 */
class LandingPublicContractTest extends LandingApiTestSupport {

	@Test
	void publicLanding_withoutAuth_returnsOnlyActivePublicSectionsWithoutPii() throws Exception {
		seedSection(LandingScope.PUBLIC, LandingSectionType.HERO, "Institucional", 0, true);
		seedSection(LandingScope.PUBLIC, LandingSectionType.ANNOUNCEMENT, "Inactiva", 1, false);
		seedSection(LandingScope.HOME, LandingSectionType.HERO, "Interno", 0, true);

		mockMvc.perform(get("/api/v1/landing/public"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].titulo").value("Institucional"))
				.andExpect(jsonPath("$.data[0].type").value("HERO"))
				// La proyección pública no expone id interno, scope ni banderas administrativas.
				.andExpect(jsonPath("$.data[0].id").doesNotExist())
				.andExpect(jsonPath("$.data[0].scope").doesNotExist())
				.andExpect(jsonPath("$.data[0].active").doesNotExist());
	}

	@Test
	void publicLanding_exceedingRateLimit_returns429() throws Exception {
		seedSection(LandingScope.PUBLIC, LandingSectionType.HERO, "Institucional", 0, true);

		// Cubo aislado por IP única: 30 peticiones (capacidad por defecto) pasan; la siguiente se rechaza.
		RequestPostProcessor fromFixedIp = request -> {
			request.setRemoteAddr("203.0.113.7");
			return request;
		};
		for (int i = 0; i < 30; i++) {
			mockMvc.perform(get("/api/v1/landing/public").with(fromFixedIp))
					.andExpect(status().isOk());
		}
		mockMvc.perform(get("/api/v1/landing/public").with(fromFixedIp))
				.andExpect(status().isTooManyRequests());
	}
}
