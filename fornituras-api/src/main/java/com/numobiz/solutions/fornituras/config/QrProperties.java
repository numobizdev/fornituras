package com.numobiz.solutions.fornituras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.qr")
public record QrProperties(
		String prefix,
		int sequenceLength,
		int maxBatchSize
) {
}
