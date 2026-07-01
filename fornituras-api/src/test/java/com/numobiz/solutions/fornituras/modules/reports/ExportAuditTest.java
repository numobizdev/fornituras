package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoría de exportación (T015): cada export escribe un evento {@code EXPORT_REPORT} y su detalle
 * NO contiene PII (FR-005/SC-003).
 */
@WithMockUser(roles = "ADMIN")
class ExportAuditTest extends ReportApiTestSupport {

	@MockitoBean
	private AuditWriter audit;

	@Test
	void export_writesAuditEvent_withoutPii() throws Exception {
		long e = seedEquipment("FOR-1", EquipmentStatus.ASIGNADA);
		long o = seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");
		seedActiveAssignment(e, o);

		MvcResult async = mockMvc.perform(get("/api/v1/reports/active-assignments/export")
						.param("curp", "CURP010101MDFXYZ01"))
				.andExpect(request().asyncStarted())
				.andReturn();
		mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());

		ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
		verify(audit).recordEvent(eq("EXPORT_REPORT"), detail.capture());
		assertThat(detail.getValue())
				.contains("ACTIVE_ASSIGNMENTS")
				.contains("curp")                       // el nombre del filtro sí puede registrarse
				.doesNotContain("CURP010101MDFXYZ01");  // el valor PII nunca
	}
}
