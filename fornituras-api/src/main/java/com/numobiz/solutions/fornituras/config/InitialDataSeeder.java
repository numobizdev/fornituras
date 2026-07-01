package com.numobiz.solutions.fornituras.config;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialDataSeeder.class);

	private final SeedAdminProperties seedAdminProperties;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public InitialDataSeeder(
			SeedAdminProperties seedAdminProperties,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.seedAdminProperties = seedAdminProperties;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/** Usuario de prueba (solo dev, mismo flag que el admin): sirve como responsable seleccionable. */
	private static final String TEST_USER_EMAIL = "responsable.prueba@fornituras.local";
	private static final String TEST_USER_NAME = "Responsable de Prueba";
	private static final String TEST_USER_PASSWORD = "Prueba#2026";

	@Override
	public void run(ApplicationArguments args) {
		if (!seedAdminProperties.enabled()) {
			return;
		}

		seedAdmin();
		seedTestUser();
	}

	private void seedAdmin() {
		if (userRepository.existsByEmail(seedAdminProperties.email())) {
			log.debug("Initial admin user already exists: {}", seedAdminProperties.email());
			return;
		}

		User admin = new User();
		admin.setName(seedAdminProperties.name());
		admin.setEmail(seedAdminProperties.email());
		admin.setPassword(passwordEncoder.encode(seedAdminProperties.password()));
		admin.setRole(Role.ADMIN);
		admin.setEnabled(true);

		userRepository.save(admin);
		log.info("Initial admin user seeded: {}", seedAdminProperties.email());
	}

	private void seedTestUser() {
		if (userRepository.existsByEmail(TEST_USER_EMAIL)) {
			log.debug("Test user already exists: {}", TEST_USER_EMAIL);
			return;
		}

		User testUser = new User();
		testUser.setName(TEST_USER_NAME);
		testUser.setEmail(TEST_USER_EMAIL);
		testUser.setPassword(passwordEncoder.encode(TEST_USER_PASSWORD));
		testUser.setRole(Role.ALMACEN);
		testUser.setEnabled(true);

		userRepository.save(testUser);
		log.info("Test user seeded (dev): {} / {}", TEST_USER_EMAIL, TEST_USER_PASSWORD);
	}
}
