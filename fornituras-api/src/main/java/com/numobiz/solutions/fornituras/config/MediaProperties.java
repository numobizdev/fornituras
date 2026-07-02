package com.numobiz.solutions.fornituras.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración del módulo de media (017, ADR 0016). La ruta de almacenamiento vive fuera del repo
 * y llega por entorno (Principio III); aquí solo se declara el nombre de la variable. Los límites de
 * peso/dimensiones acotan el peor caso de la re-codificación y de la subida.
 *
 * @param storagePath  Directorio en el servidor (fuera del repo) donde se guardan los objetos cifrados.
 * @param maxSizeBytes Peso máximo del archivo aceptado (objetivo 5 MB).
 * @param maxWidth     Ancho máximo en píxeles de la imagen saneada.
 * @param maxHeight    Alto máximo en píxeles de la imagen saneada.
 * @param allowedTypes Content-types permitidos tras validar magic bytes (whitelist).
 * @param officerPhotoEnabled Gate de captura de foto de elemento (PII); {@code false} hasta base
 *                            legal confirmada por ADR 0003 (FR-015).
 */
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
		String storagePath,
		long maxSizeBytes,
		int maxWidth,
		int maxHeight,
		List<String> allowedTypes,
		boolean officerPhotoEnabled
) {
}
