package com.numobiz.solutions.fornituras.modules.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No-PII (T009/SC-002): si un detalle de auditoría contuviera un identificador sensible (CURP/RFC),
 * la redacción lo enmascara antes de persistir. Ningún registro guarda PII en claro.
 */
@WithMockUser(roles = "ADMIN")
class AuditNoPiiTest extends AuditApiTestSupport {

	@Test
	void detailWithCurp_isRedactedBeforePersist() {
		auditWriter.recordEvent("TEST_EXPORT", "filtro curp=CURP010101MDFXYZ01 fin");

		String evidencia = jdbcTemplate.queryForObject(
				"SELECT evidencia FROM audit_log WHERE accion = 'TEST_EXPORT'", String.class);

		assertThat(evidencia).doesNotContain("CURP010101MDFXYZ01").contains("***");
	}
}
