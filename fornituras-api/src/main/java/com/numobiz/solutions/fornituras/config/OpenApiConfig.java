package com.numobiz.solutions.fornituras.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
		name = "Bearer Authentication",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer"
)
public class OpenApiConfig {

	@Bean
	public OpenAPI forniturasOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Fornituras API")
						.description("REST API for Fornituras SaaS platform")
						.version("v1")
						.contact(new Contact()
								.name("Numobiz Solutions")
								.email("support@numobiz.com")))
				.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
	}
}
