package com.numobiz.solutions.fornituras.modules.media.service;

import com.numobiz.solutions.fornituras.common.crypto.PiiCipher;
import com.numobiz.solutions.fornituras.config.MediaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Adaptador de {@link FileStoragePort} que guarda los objetos como archivos cifrados AES-256-GCM en un
 * directorio del servidor <b>fuera del repo</b> ({@code app.media.storage-path}, ADR 0016). Reutiliza
 * el cifrado ya aprobado ({@link PiiCipher}, ADR 0006): cada archivo es autocontenido
 * {@code IV ‖ ciphertext ‖ tag}, de modo que ningún byte queda en claro en disco.
 *
 * <p>Las {@code storageKey} se normalizan y se validan contra {@code ..} para impedir escape del
 * directorio base (path traversal), aunque hoy las genera el servidor.
 */
@Component
public class LocalEncryptedFileStorage implements FileStoragePort {

	private static final Logger log = LoggerFactory.getLogger(LocalEncryptedFileStorage.class);

	private final Path baseDir;

	public LocalEncryptedFileStorage(MediaProperties properties) {
		this.baseDir = Paths.get(properties.storagePath()).toAbsolutePath().normalize();
	}

	@Override
	public StoredObject store(String storageKey, byte[] content) {
		byte[] encrypted = PiiCipher.encryptBytes(content);
		Path target = resolve(storageKey);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, encrypted);
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo escribir el objeto de media", e);
		}
		byte[] iv = Arrays.copyOfRange(encrypted, 0, PiiCipher.ivLength());
		return new StoredObject(iv, encrypted.length);
	}

	@Override
	public byte[] load(String storageKey) {
		Path source = resolve(storageKey);
		try {
			byte[] encrypted = Files.readAllBytes(source);
			return PiiCipher.decryptBytes(encrypted);
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer el objeto de media", e);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolve(storageKey));
		} catch (IOException e) {
			// El borrado no debe tumbar el flujo; se deja rastro para la limpieza posterior (FR-016).
			log.warn("No se pudo borrar el objeto de media storageKey={} : {}", storageKey, e.getMessage());
		}
	}

	/** Resuelve la ruta dentro del directorio base rechazando cualquier intento de path traversal. */
	private Path resolve(String storageKey) {
		Path resolved = baseDir.resolve(storageKey).normalize();
		if (!resolved.startsWith(baseDir)) {
			throw new IllegalArgumentException("storageKey inválido (fuera del directorio de media)");
		}
		return resolved;
	}
}
