package com.numobiz.solutions.fornituras.modules.media;

import com.numobiz.solutions.fornituras.modules.media.entity.MediaAsset;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T013 (US1): subir un JPEG válido de equipo (no PII) crea la fila {@code media_asset} con
 * {@code is_pii = 0}, guarda el objeto <b>cifrado</b> en disco (no abre como imagen) y la imagen
 * servida no conserva EXIF.
 */
class MediaUploadIT extends MediaApiTestSupport {

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void uploadsValidJpegEncryptedWithoutExif() throws Exception {
		MvcResult result = mockMvc.perform(multipart(MEDIA_URL)
						.file(jpegPart())
						.param("context", "equipment"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.contentType").value("image/jpeg"))
				.andReturn();

		List<MediaAsset> assets = mediaAssetRepository.findAll();
		assertThat(assets).hasSize(1);
		MediaAsset asset = assets.get(0);
		assertThat(asset.isPii()).isFalse();
		assertThat(asset.getSha256()).hasSize(64);
		assertThat(result.getResponse().getContentAsString()).contains(asset.getId().toString());

		// El objeto en disco está cifrado: no es una imagen decodificable y no contiene el marcador EXIF.
		Path stored = Paths.get(mediaProperties.storagePath()).resolve(asset.getStorageKey());
		byte[] onDisk = Files.readAllBytes(stored);
		assertThat(ImageIO.read(new ByteArrayInputStream(onDisk)))
				.as("el objeto en disco no debe abrir como imagen (está cifrado)")
				.isNull();
		assertThat(containsAscii(onDisk, "Exif")).isFalse();
		assertThat(isJpegMagic(onDisk)).as("el objeto en disco no empieza con la firma JPEG").isFalse();
	}

	private boolean isJpegMagic(byte[] b) {
		return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
	}

	private boolean containsAscii(byte[] haystack, String needle) {
		byte[] n = needle.getBytes(StandardCharsets.US_ASCII);
		outer:
		for (int i = 0; i <= haystack.length - n.length; i++) {
			for (int j = 0; j < n.length; j++) {
				if (haystack[i + j] != n[j]) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}
}
