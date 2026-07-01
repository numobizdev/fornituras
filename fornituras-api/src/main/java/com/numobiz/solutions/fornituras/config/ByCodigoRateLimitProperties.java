package com.numobiz.solutions.fornituras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del rate limiting de la resolución por código ({@code GET /equipment/by-codigo}).
 * {@code capacity} peticiones por cada {@code refillPeriodSeconds} y por actor. Valores por defecto
 * conservadores para uso legítimo, suficientes para frenar la enumeración (ADR 0010).
 */
@ConfigurationProperties(prefix = "app.ratelimit.by-codigo")
public record ByCodigoRateLimitProperties(Integer capacity, Integer refillPeriodSeconds) {

	public ByCodigoRateLimitProperties {
		if (capacity == null || capacity <= 0) {
			capacity = 30;
		}
		if (refillPeriodSeconds == null || refillPeriodSeconds <= 0) {
			refillPeriodSeconds = 60;
		}
	}
}
