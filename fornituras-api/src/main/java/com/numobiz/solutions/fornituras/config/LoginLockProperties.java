package com.numobiz.solutions.fornituras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del bloqueo de cuenta por intentos de login fallidos (feature 013, FR-005). Tras
 * {@code maxAttempts} fallos consecutivos la cuenta queda bloqueada {@code lockMinutes} minutos.
 * Valores por defecto conservadores: frenan la fuerza bruta sin castigar errores legítimos ocasionales.
 */
@ConfigurationProperties(prefix = "app.security.login-lock")
public record LoginLockProperties(Integer maxAttempts, Integer lockMinutes) {

	public LoginLockProperties {
		if (maxAttempts == null || maxAttempts <= 0) {
			maxAttempts = 5;
		}
		if (lockMinutes == null || lockMinutes <= 0) {
			lockMinutes = 15;
		}
	}
}
