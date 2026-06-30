package com.numobiz.solutions.fornituras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qr")
public record QrProperties(
		String prefix,
		int suffixLength,
		int maxBatchSize,
		int maxGenerationRetries
) {
}
