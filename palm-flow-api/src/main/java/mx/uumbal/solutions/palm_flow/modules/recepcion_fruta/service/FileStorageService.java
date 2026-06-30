package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.BadRequestException;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png");

    private final Path rootDir;
    private final long maxFileSizeBytes;

    public FileStorageService(
            @Value("${app.storage.recepciones-dir:./data/recepciones-fotos}") String recepcionesDir,
            @Value("${app.storage.max-file-size-bytes:5242880}") long maxFileSizeBytes) {
        this.rootDir = Path.of(recepcionesDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public StoredFile storeRecepcionFoto(UUID recepcionUuid, String tipo, MultipartFile file) {
        validateFile(file);

        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        String extension = extensionFor(file.getContentType());
        String filename = tipo.toLowerCase() + "_" + Instant.now().toEpochMilli() + extension;
        Path targetDir = rootDir.resolve(tenantId).resolve(recepcionUuid.toString());

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("Stored reception photo at {}", targetFile);
            return new StoredFile(targetFile, file.getContentType());
        } catch (IOException e) {
            throw new BadRequestException("No se pudo guardar la imagen: " + e.getMessage());
        }
    }

    public Resource loadAsResource(String storagePath) {
        try {
            Path file = rootDir.resolve(storagePath).normalize();
            if (!file.startsWith(rootDir) || !Files.exists(file)) {
                throw new BadRequestException("Archivo no encontrado");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("Archivo no legible");
            }
            return resource;
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer la imagen: " + e.getMessage());
        }
    }

    public void deleteIfExists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path file = rootDir.resolve(storagePath).normalize();
            if (file.startsWith(rootDir)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", storagePath, e.getMessage());
        }
    }

    public String toRelativeStoragePath(Path absolutePath) {
        return rootDir.relativize(absolutePath.normalize()).toString().replace('\\', '/');
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo de imagen es obligatorio");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("La imagen excede el tamaño máximo permitido (5 MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Tipo de imagen no permitido. Use JPEG o PNG");
        }
    }

    private String extensionFor(String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("png")) {
            return ".png";
        }
        return ".jpg";
    }

    public record StoredFile(Path absolutePath, String contentType) {}
}
