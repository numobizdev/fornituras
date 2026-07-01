package com.numobiz.solutions.fornituras.modules.users;

import com.numobiz.solutions.fornituras.modules.auth.service.EmailService;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Base de las pruebas de administración de usuarios (013). Arranca la app completa sobre H2 con MockMvc
 * y seguridad real. El envío de correo se mockea (el alta dispara un código de activación) para que las
 * pruebas sean deterministas y no dependan de un SMTP. La limpieza entre pruebas borra los datos por
 * JDBC respetando las FKs (tokens y bitácora antes que usuarios).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class UserAdminTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected UserRepository userRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@MockitoBean
	protected EmailService emailService;

	@BeforeEach
	void cleanUsers() {
		jdbcTemplate.execute("DELETE FROM verification_tokens");
		jdbcTemplate.execute("DELETE FROM password_reset_tokens");
		jdbcTemplate.execute("DELETE FROM audit_log");
		jdbcTemplate.execute("DELETE FROM users");
	}

	protected User seedUser(String name, String email, Role role, boolean enabled) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode("Secret123#"));
		user.setRole(role);
		user.setEnabled(enabled);
		return userRepository.save(user);
	}

	protected long countAudit(String accion) {
		Long n = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_log WHERE accion = ?", Long.class, accion);
		return n == null ? 0 : n;
	}
}
