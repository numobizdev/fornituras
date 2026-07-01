package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate limiting de la resolución por código (T040, ADR 0010): con la capacidad agotada por el mismo
 * actor, la siguiente resolución responde 429 sin filtrar detalles. Se usa un cupo bajo propio para
 * no interferir con el contexto compartido del resto de pruebas.
 */
@TestPropertySource(properties = {
		"app.ratelimit.by-codigo.capacity=2",
		"app.ratelimit.by-codigo.refill-period-seconds=3600"
})
@WithMockUser(roles = "CONSULTA")
class EquipmentRateLimitTest extends EquipmentApiTestSupport {

	@Test
	void byCodigo_exceedingCapacity_returns429() throws Exception {
		persistEquipment("FOR-RL", EquipmentStatus.DISPONIBLE, null);

		// Capacidad = 2 → las dos primeras pasan, la tercera se rechaza.
		mockMvc.perform(get("/api/v1/equipment/by-codigo/{codigo}", "FOR-RL"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/equipment/by-codigo/{codigo}", "FOR-RL"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/equipment/by-codigo/{codigo}", "FOR-RL"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.success").value(false));
	}
}
