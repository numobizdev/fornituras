package com.numobiz.solutions.fornituras.modules.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización del tablero (T008): requiere autenticación; cualquier rol autenticado lo consulta y la
 * respuesta es solo numérica (sin PII ni registros). El RBAC fino por indicador se define en 013.
 */
class DashboardAuthTest extends DashboardApiTestSupport {

	@Test
	void summary_withoutAuthentication_isDenied() throws Exception {
		// Sin autenticación, la API responde 401 (no autenticado) vía RestAuthenticationEntryPoint;
		// el 403 se reserva para un usuario autenticado sin permiso (convención REST, ADR 0015).
		mockMvc.perform(get("/api/v1/dashboard/summary"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void summary_withAdminRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void summary_withCapturistaRole_returnsOnlyNumericCounters_noPii() throws Exception {
		// La respuesta expone exactamente los seis contadores numéricos y nada más (cero PII).
		mockMvc.perform(get("/api/v1/dashboard/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").isNumber())
				.andExpect(jsonPath("$.data.disponibles").isNumber())
				.andExpect(jsonPath("$.data.asignadas").isNumber())
				.andExpect(jsonPath("$.data.proximasAVencer").isNumber())
				.andExpect(jsonPath("$.data.caducadas").isNumber())
				.andExpect(jsonPath("$.data.enMantenimiento").isNumber())
				.andExpect(jsonPath("$.data.length()").value(6));
	}
}
