package com.numobiz.solutions.fornituras.modules.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Edge case: con el inventario vacío el tablero responde con todos los contadores en cero, sin error.
 */
@WithMockUser(roles = "ADMIN")
class DashboardEmptyTest extends DashboardApiTestSupport {

	@Test
	void emptyInventory_returnsAllZeros() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(0))
				.andExpect(jsonPath("$.data.disponibles").value(0))
				.andExpect(jsonPath("$.data.asignadas").value(0))
				.andExpect(jsonPath("$.data.proximasAVencer").value(0))
				.andExpect(jsonPath("$.data.caducadas").value(0))
				.andExpect(jsonPath("$.data.enMantenimiento").value(0));
	}
}
