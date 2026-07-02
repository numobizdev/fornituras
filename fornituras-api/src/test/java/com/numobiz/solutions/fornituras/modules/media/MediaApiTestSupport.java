package com.numobiz.solutions.fornituras.modules.media;

import com.numobiz.solutions.fornituras.config.MediaProperties;
import com.numobiz.solutions.fornituras.modules.media.repository.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Base de las pruebas de integración del módulo de media (017, patrón ADR 0009): arranca la app sobre
 * el perfil H2 con MockMvc y seguridad real, dejando limpia la tabla {@code media_asset} antes de cada
 * prueba. Aporta generadores de imágenes válidas y de payloads inválidos para ejercitar la validación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class MediaApiTestSupport {

	protected static final String MEDIA_URL = "/api/v1/media";

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected MediaAssetRepository mediaAssetRepository;
	@Autowired
	protected MediaProperties mediaProperties;

	@BeforeEach
	void cleanMedia() {
		mediaAssetRepository.deleteAll();
	}

	protected MockMultipartFile jpegPart(int width, int height) {
		return new MockMultipartFile("image", "photo.jpg", "image/jpeg", pngOrJpeg(width, height, "jpeg"));
	}

	protected MockMultipartFile jpegPart() {
		return jpegPart(64, 48);
	}

	protected MockMultipartFile svgPart() {
		byte[] svg = ("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>")
				.getBytes(StandardCharsets.UTF_8);
		return new MockMultipartFile("image", "evil.svg", "image/svg+xml", svg);
	}

	/** Archivo de texto renombrado a .jpg: supera la extensión pero falla los magic bytes. */
	protected MockMultipartFile fakeJpegPart() {
		return new MockMultipartFile("image", "notreally.jpg", "image/jpeg",
				"esto no es una imagen".getBytes(StandardCharsets.UTF_8));
	}

	/** Payload por encima del límite de peso configurado en el perfil de test (1 MB) → 413. */
	protected MockMultipartFile oversizePart() {
		byte[] big = new byte[(int) mediaProperties.maxSizeBytes() + 1024];
		return new MockMultipartFile("image", "big.jpg", "image/jpeg", big);
	}

	protected byte[] rawJpeg(int width, int height) {
		return pngOrJpeg(width, height, "jpeg");
	}

	private byte[] pngOrJpeg(int width, int height, String format) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.setColor(Color.BLUE);
		g.fillRect(0, 0, width, height);
		g.dispose();
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, format, out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
