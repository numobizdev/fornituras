package com.numobiz.solutions.fornituras.modules.media.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.UnprocessableEntityException;
import com.numobiz.solutions.fornituras.config.MediaProperties;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Valida y sanea una imagen subida antes de cifrarla y guardarla (017, ADR 0016 §4-5).
 *
 * <p>Reglas (rechazo por defecto, no confiar en extensión ni en {@code Content-Type} declarado):
 * <ol>
 *   <li>Detecta el tipo por <b>magic bytes</b> (JPEG/PNG/WEBP) y rechaza cualquier otro, incluido
 *       <b>SVG</b> (contenido activo → XSS).</li>
 *   <li><b>Re-codifica</b> con {@code ImageIO} leyendo a {@link BufferedImage} y volviendo a escribir:
 *       el resultado <b>no conserva EXIF</b> (crítico: EXIF puede llevar GPS → fuga de ubicación).</li>
 *   <li>Valida dimensiones máximas.</li>
 * </ol>
 * El límite de <b>peso</b> se comprueba antes, en {@link MediaService} (produce 413).
 */
@Component
public class ImageSanitizer {

	private static final String JPEG = "image/jpeg";
	private static final String PNG = "image/png";
	private static final String WEBP = "image/webp";

	private final MediaProperties properties;

	public ImageSanitizer(MediaProperties properties) {
		this.properties = properties;
	}

	/** Imagen saneada lista para cifrar: bytes re-codificados (sin EXIF) y su content-type final. */
	public record SanitizedImage(byte[] bytes, String contentType) {
	}

	public SanitizedImage sanitize(byte[] raw) {
		String detected = detectType(raw);
		if (detected == null) {
			throw new BadRequestException(
					"El archivo no es una imagen JPEG/PNG/WEBP válida (contenido no permitido).");
		}

		BufferedImage image = decode(raw);
		if (image == null) {
			throw new UnprocessableEntityException("La imagen está corrupta o no se puede procesar.");
		}
		validateDimensions(image);

		// Re-codifica al formato normalizado equivalente; WEBP se normaliza a PNG (sin pérdida) porque el
		// JDK estándar no trae codificador WEBP. El re-write descarta cualquier metadato EXIF del origen.
		String outputFormat = JPEG.equals(detected) ? "jpeg" : "png";
		String outputType = JPEG.equals(detected) ? JPEG : PNG;
		return new SanitizedImage(reencode(image, outputFormat), outputType);
	}

	/** Determina el tipo real por firma de bytes; devuelve {@code null} si no es una imagen permitida. */
	private String detectType(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return JPEG;
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
				&& (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
			return PNG;
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
				&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
			return WEBP;
		}
		return null;
	}

	private BufferedImage decode(byte[] raw) {
		try {
			return ImageIO.read(new ByteArrayInputStream(raw));
		} catch (IOException e) {
			return null;
		}
	}

	private void validateDimensions(BufferedImage image) {
		if (image.getWidth() > properties.maxWidth() || image.getHeight() > properties.maxHeight()) {
			throw new BadRequestException("La imagen excede las dimensiones máximas permitidas ("
					+ properties.maxWidth() + "x" + properties.maxHeight() + ").");
		}
	}

	private byte[] reencode(BufferedImage image, String format) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (!ImageIO.write(image, format, out)) {
				throw new UnprocessableEntityException("No se pudo re-codificar la imagen al formato " + format + ".");
			}
			return out.toByteArray();
		} catch (IOException e) {
			throw new UnprocessableEntityException("No se pudo re-codificar la imagen.");
		}
	}
}
