package com.numobiz.solutions.fornituras.common.ratelimit;

/**
 * Puerto de limitación de tasa (rate limiting). Aísla a los consumidores del mecanismo concreto
 * (LEGO/DIP): hoy un token-bucket en memoria (Bucket4j), mañana un backend distribuido (Redis) sin
 * tocar quien lo usa. Cada {@code key} identifica un cubo independiente (p. ej. usuario+operación).
 */
public interface RateLimiter {

	/**
	 * Intenta consumir una unidad del cubo asociado a {@code key}.
	 *
	 * @return {@code true} si había cupo (permitido); {@code false} si se agotó (rechazar).
	 */
	boolean tryConsume(String key);
}
