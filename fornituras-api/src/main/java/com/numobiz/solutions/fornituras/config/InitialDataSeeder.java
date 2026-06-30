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

	@Override
	public void run(ApplicationArguments args) {
		if (!seedAdminProperties.enabled()) {
			return;
		}

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
}
