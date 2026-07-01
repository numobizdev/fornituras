package com.numobiz.solutions.fornituras.modules.media.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.modules.media.dto.MediaUploadResponse;
import com.numobiz.solutions.fornituras.modules.media.entity.MediaAsset;
import com.numobiz.solutions.fornituras.modules.media.entity.MediaContext;
import com.numobiz.solutions.fornituras.modules.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * API de fotos (017, ADR 0016). Todos los endpoints exigen autenticación; la foto de <b>elemento</b>
 * (PII) añade RBAC + gating + auditoría, resueltos en {@link MediaService}. Los identificadores son
 * UUID opacos (no enumerables) y nunca se sirve una imagen de forma anónima.
 */
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "Fotos cifradas de equipos, tipos y elementos (017)")
@SecurityRequirement(name = "Bearer Authentication")
public class MediaController {

	private final MediaService service;

	public MediaController(MediaService service) {
		this.service = service;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Subir foto", description = "Valida, sanea (elimina EXIF), cifra y almacena una imagen. context=officer exige rol PII y gating (ADR 0003).")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<MediaUploadResponse>> upload(
			@RequestParam("image") MultipartFile image,
			@RequestParam("context") String context) {
		if (image == null || image.isEmpty()) {
			throw new BadRequestException("Se requiere una imagen.");
		}
		MediaContext mediaContext = parseContext(context);
		MediaAsset asset = service.store(readBytes(image), mediaContext);
		MediaUploadResponse body = new MediaUploadResponse(
				asset.getId(), reference(asset.getId()), asset.getContentType());
		return ResponseEntity.ok(ApiResponse.ok("Foto subida", body));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Descargar foto", description = "Devuelve la imagen descifrada si el solicitante está autorizado. Foto de elemento (PII): solo roles autorizados; cada acceso se audita.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<byte[]> download(@PathVariable UUID id) {
		MediaService.LoadedImage image = service.load(id);
		CacheControl cacheControl = image.pii()
				? CacheControl.noStore().cachePrivate()
				: CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePrivate();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.contentType()))
				.cacheControl(cacheControl)
				.body(image.bytes());
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar foto", description = "Borra la foto y purga el objeto (retención/ARCO). Foto de elemento: rol autorizado; evento auditado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	private MediaContext parseContext(String context) {
		try {
			return MediaContext.valueOf(context.trim().toUpperCase());
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new BadRequestException("context inválido: use equipment | equipment_type | officer.");
		}
	}

	private byte[] readBytes(MultipartFile image) {
		try {
			return image.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer la imagen subida", e);
		}
	}

	private String reference(UUID id) {
		return "/api/v1/media/" + id;
	}
}
