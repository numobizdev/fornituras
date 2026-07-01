package com.numobiz.solutions.fornituras.modules.decommissions;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de {@code GET /api/v1/decommissions} (T017): listado paginado y filtros por fecha, tipo y
 * motivo. La consulta es para cualquier rol autenticado.
 */
@WithMockUser(roles = "CONSULTA")
class DecommissionListContractTest extends DecommissionApiTestSupport {

	@Test
	void list_returnsPagedDecommissions() throws Exception {
		seedDecommission(seed.equipmentId(), seed.motivoId(), LocalDate.of(2026, 6, 1));
		long otro = seedEquipment("FOR-B2", EquipmentStatus.DISPONIBLE);
		seedDecommission(otro, seed.motivoId(), LocalDate.of(2026, 6, 2));

		mockMvc.perform(get("/api/v1/decommissions").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content[0].equipmentCodigo").exists())
				.andExpect(jsonPath("$.data.content[0].motivoNombre").value("Daño"));
	}

	@Test
	void list_filterByMotivo_narrowsResults() throws Exception {
		long otroMotivo = seedReason("Caducidad");
		seedDecommission(seed.equipmentId(), seed.motivoId(), LocalDate.of(2026, 6, 1));
		long otro = seedEquipment("FOR-B2", EquipmentStatus.DISPONIBLE);
		seedDecommission(otro, otroMotivo, LocalDate.of(2026, 6, 2));

		mockMvc.perform(get("/api/v1/decommissions").param("motivoId", String.valueOf(otroMotivo)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].motivoNombre").value("Caducidad"));
	}

	@Test
	void list_filterByFecha_narrowsResults() throws Exception {
		seedDecommission(seed.equipmentId(), seed.motivoId(), LocalDate.of(2026, 5, 1));
		long otro = seedEquipment("FOR-B2", EquipmentStatus.DISPONIBLE);
		seedDecommission(otro, seed.motivoId(), LocalDate.of(2026, 6, 20));

		mockMvc.perform(get("/api/v1/decommissions")
						.param("fechaDesde", "2026-06-01")
						.param("fechaHasta", "2026-06-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void list_filterByTipo_narrowsResults() throws Exception {
		seedDecommission(seed.equipmentId(), seed.motivoId(), LocalDate.of(2026, 6, 1));
		long otroTipo = seedEquipmentOfType("FOR-B3", EquipmentStatus.DISPONIBLE, 2L);
		seedDecommission(otroTipo, seed.motivoId(), LocalDate.of(2026, 6, 2));

		mockMvc.perform(get("/api/v1/decommissions").param("tipoId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].equipmentCodigo").value("FOR-B3"));
	}
}
