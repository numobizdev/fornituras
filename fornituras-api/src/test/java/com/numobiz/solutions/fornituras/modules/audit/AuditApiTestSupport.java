package com.numobiz.solutions.fornituras.modules.audit;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Base de las pruebas de la bitácora (012). Arranca la app completa sobre H2 con MockMvc y seguridad
 * real. Como {@code audit_log} es append-only (el repositorio no expone borrado), la limpieza entre
 * pruebas se hace por JDBC (en H2 no existen los triggers de inmutabilidad, que son solo de SQL Server).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AuditApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected AuditWriter auditWriter;
	@Autowired
	protected OfficerRepository officerRepository;
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	void clearAudit() {
		jdbcTemplate.execute("DELETE FROM audit_log");
	}

	protected long countAudit() {
		Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
		return n == null ? 0 : n;
	}

	protected long countAudit(String accion) {
		Long n = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_log WHERE accion = ?", Long.class, accion);
		return n == null ? 0 : n;
	}

	protected long seedOfficer(String nombre, String placa) {
		Officer officer = new Officer();
		officer.setNombre(nombre);
		officer.setApellidoPaterno("Apellido");
		officer.setPlaca(placa.trim().toUpperCase(Locale.ROOT));
		officer.setPlacaNormalizada(CodeNormalizer.normalize(placa));
		officer.setSexoId(1L);
		officer.setMunicipio("Guadalajara");
		officer.setActive(true);
		return officerRepository.save(officer).getId();
	}
}
