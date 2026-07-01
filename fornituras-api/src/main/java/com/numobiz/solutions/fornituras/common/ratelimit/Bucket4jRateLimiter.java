package com.numobiz.solutions.fornituras.common.ratelimit;

import com.numobiz.solutions.fornituras.config.ByCodigoRateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del puerto {@link RateLimiter} con Bucket4j (token-bucket): un cubo por
 * {@code key}, con la capacidad y el ritmo de recarga configurados para {@code by-codigo}. Es local
 * a la instancia (suficiente para el despliegue actual); para escenarios multi-instancia se puede
 * sustituir por una implementación distribuida (Redis) sin tocar a los consumidores (DIP). Ver
 * ADR 0010.
 */
@Component
public class Bucket4jRateLimiter implements RateLimiter {

	private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
	private final long capacity;
	private final Duration refillPeriod;

	public Bucket4jRateLimiter(ByCodigoRateLimitProperties properties) {
		this.capacity = properties.capacity();
		this.refillPeriod = Duration.ofSeconds(properties.refillPeriodSeconds());
	}

	@Override
	public boolean tryConsume(String key) {
		return buckets.computeIfAbsent(key, k -> newBucket()).tryConsume(1);
	}

	private Bucket newBucket() {
		Bandwidth limit = Bandwidth.builder()
				.capacity(capacity)
				.refillGreedy(capacity, refillPeriod)
				.build();
		return Bucket.builder().addLimit(limit).build();
	}
}
