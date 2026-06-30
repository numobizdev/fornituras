package com.numobiz.solutions.fornituras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed.admin")
public record SeedAdminProperties(
		boolean enabled,
		String name,
		String email,
		String password
) {
}
