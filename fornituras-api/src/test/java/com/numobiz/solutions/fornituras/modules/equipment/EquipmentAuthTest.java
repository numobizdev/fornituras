package com.numobiz.solutions.fornituras.modules.equipment;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización de {@code /equipment} (T014, matriz ADR 0013): la escritura de inventario se restringe a
 * {@code WRITE_INVENTORY} (ADMIN/ALMACEN/CAPTURISTA); un rol sin permiso —incluido AUDITOR, que es de
 * solo lectura— queda denegado (403) y no crea nada; la consulta es para cualquier autenticado. El
 * rechazo ocurre de forma declarativa en la capa de seguridad ({@code @PreAuthorize}), antes de
 * llegar al servicio, por lo que no genera auditoría de negocio.
 */
class EquipmentAuthTest extends EquipmentApiTestSupport {

	@Test
	@WithMockUser(roles = "CONSULTA")
	void create_withoutWriteRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-100")))
				.andExpect(status().isForbidden());

		assertThat(equipmentRepository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void create_withCapturistaRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-101")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.count()).isEqualTo(1);
	}

	@Test
	@WithMockUser(roles = "ALMACEN")
	void create_withAlmacenRole_isAllowed() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-102")))
				.andExpect(status().isCreated());

		assertThat(equipmentRepository.count()).isEqualTo(1);
	}

	@Test
	@WithMockUser(roles = "AUDITOR")
	void create_withReadOnlyAuditorRole_isForbidden() throws Exception {
		mockMvc.perform(post("/api/v1/equipment")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJson("FOR-103")))
				.andExpect(status().isForbidden());

		assertThat(equipmentRepository.count()).isZero();
	}

	@Test
	@WithMockUser(roles = "CONSULTA")
	void list_withAnyAuthenticatedRole_isAllowed() throws Exception {
		mockMvc.perform(get("/api/v1/equipment"))
				.andExpect(status().isOk());
	}
}
