package com.numobiz.solutions.fornituras.modules.landing;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSection;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;
import com.numobiz.solutions.fornituras.modules.landing.repository.LandingSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Base de las pruebas de contrato/integración de la landing (feature 016): arranca la app completa sobre
 * el perfil H2 con MockMvc y seguridad real. Antes de cada prueba deja limpia la tabla de secciones. Las
 * pruebas siembran las filas que necesitan (Flyway está deshabilitado en test; el esquema lo genera JPA).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class LandingApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected LandingSectionRepository repository;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	protected long seedSection(
			LandingScope scope, LandingSectionType type, String titulo, int orden, boolean active) {
		return seedSection(scope, type, titulo, orden, active, null);
	}

	protected long seedSection(
			LandingScope scope, LandingSectionType type, String titulo, int orden, boolean active,
			String configJson) {
		LandingSection section = new LandingSection();
		section.setScope(scope);
		section.setType(type);
		section.setTitulo(titulo);
		section.setOrden(orden);
		section.setActive(active);
		section.setConfigJson(configJson);
		return repository.save(section).getId();
	}
}
