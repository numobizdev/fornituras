package com.numobiz.solutions.fornituras.modules.media.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.common.exception.PayloadTooLargeException;
import com.numobiz.solutions.fornituras.config.MediaProperties;
import com.numobiz.solutions.fornituras.modules.media.entity.MediaAsset;
import com.numobiz.solutions.fornituras.modules.media.entity.MediaContext;
import com.numobiz.solutions.fornituras.modules.media.repository.MediaAssetRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Orquesta el ciclo de una foto (017, ADR 0016): autorización/gating → validación de peso → saneo
 * (EXIF stripping, {@link ImageSanitizer}) → cifrado + persistencia del objeto ({@link FileStoragePort})
 * → alta de metadatos ({@link MediaAsset}). La lectura descifra y sirve solo a quien esté autorizado.
 *
 * <p>La foto de <b>elemento</b> ({@link MediaContext#OFFICER}) es PII: su subida exige rol autorizado y
 * el <b>gate legal</b> de ADR 0003 (FR-015), y tanto la subida como la visualización se <b>auditan</b>
 * sin escribir PII en logs (Principio V). Las fotos de equipo/tipo no son PII.
 */
@Service
public class MediaService {

	private static final String ACTION_PII_UPLOAD = "MEDIA_PII_UPLOAD";
	private static final String ACTION_PII_VIEW = "MEDIA_PII_VIEW";
	private static final String ACTION_PII_DELETE = "MEDIA_PII_DELETE";

	private final MediaProperties properties;
	private final ImageSanitizer sanitizer;
	private final FileStoragePort storage;
	private final MediaAssetRepository repository;
	private final UserRepository userRepository;
	private final AuditWriter auditWriter;

	public MediaService(
			MediaProperties properties,
			ImageSanitizer sanitizer,
			FileStoragePort storage,
			MediaAssetRepository repository,
			UserRepository userRepository,
			AuditWriter auditWriter) {
		this.properties = properties;
		this.sanitizer = sanitizer;
		this.storage = storage;
		this.repository = repository;
		this.userRepository = userRepository;
		this.auditWriter = auditWriter;
	}

	/** Imagen descifrada lista para servir. */
	public record LoadedImage(byte[] bytes, String contentType, boolean pii) {
	}

	@Transactional
	public MediaAsset store(byte[] raw, MediaContext context) {
		Authentication actor = SecurityContextHolder.getContext().getAuthentication();
		if (context.isPii()) {
			authorizeOfficerUpload(actor);
		}
		if (raw.length > properties.maxSizeBytes()) {
			throw new PayloadTooLargeException("La imagen excede el peso máximo permitido.");
		}

		ImageSanitizer.SanitizedImage sanitized = sanitizer.sanitize(raw);
		String storageKey = UUID.randomUUID().toString();
		FileStoragePort.StoredObject stored = storage.store(storageKey, sanitized.bytes());

		MediaAsset asset = new MediaAsset();
		asset.setContentType(sanitized.contentType());
		asset.setSizeBytes(sanitized.bytes().length);
		asset.setSha256(sha256Hex(sanitized.bytes()));
		asset.setStorageKey(storageKey);
		asset.setIv(stored.iv());
		asset.setPii(context.isPii());
		asset.setUploadedBy(currentUserId(actor));
		MediaAsset saved = repository.save(asset);

		if (saved.isPii()) {
			auditWriter.recordEvent(ACTION_PII_UPLOAD, "mediaId=" + saved.getId());
		}
		return saved;
	}

	@Transactional
	public LoadedImage load(UUID id) {
		MediaAsset asset = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Foto no encontrada."));
		if (asset.isPii()) {
			Authentication actor = SecurityContextHolder.getContext().getAuthentication();
			if (!RolePolicy.canViewFullPii(actor)) {
				throw new AccessDeniedException("Sin autorización para ver la foto del elemento.");
			}
			auditWriter.recordEvent(ACTION_PII_VIEW, "mediaId=" + id);
		}
		byte[] bytes = storage.load(asset.getStorageKey());
		return new LoadedImage(bytes, asset.getContentType(), asset.isPii());
	}

	@Transactional
	public void delete(UUID id) {
		MediaAsset asset = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Foto no encontrada."));
		if (asset.isPii()) {
			Authentication actor = SecurityContextHolder.getContext().getAuthentication();
			authorizeOfficerUpload(actor);
			auditWriter.recordEvent(ACTION_PII_DELETE, "mediaId=" + id);
		}
		storage.delete(asset.getStorageKey());
		repository.delete(asset);
	}

	/** Exige gate legal habilitado (ADR 0003) y rol de captura de padrón; si no, 403 (FR-015). */
	private void authorizeOfficerUpload(Authentication actor) {
		if (!properties.officerPhotoEnabled()) {
			throw new AccessDeniedException(
					"La captura de foto de elemento está deshabilitada hasta confirmar la base legal (ADR 0003).");
		}
		if (!RolePolicy.canUploadOfficerPhoto(actor)) {
			throw new AccessDeniedException("Sin autorización para subir la foto del elemento.");
		}
	}

	private Long currentUserId(Authentication actor) {
		if (actor == null || actor.getName() == null) {
			return null;
		}
		return userRepository.findByEmail(actor.getName()).map(User::getId).orElse(null);
	}

	private String sha256Hex(byte[] bytes) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible en la plataforma", e);
		}
	}
}
