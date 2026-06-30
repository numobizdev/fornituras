package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.common.exception.NotFoundException;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaFotoResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFruta;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFrutaFoto;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.TipoFotoRecepcion;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.repository.RecepcionFrutaFotoRepository;
import mx.uumbal.solutions.palm_flow.security.CentroAcopioScopeService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecepcionFrutaFotoService {

    private final RecepcionFrutaFotoRepository fotoRepository;
    private final RecepcionFrutaService recepcionFrutaService;
    private final FileStorageService fileStorageService;
    private final CentroAcopioScopeService scopeService;

    @Transactional(readOnly = true)
    public List<RecepcionFrutaFotoResponseDTO> listByRecepcion(UUID recepcionUuid) {
        RecepcionFruta recepcion = requireRecepcionAccess(recepcionUuid);
        return fotoRepository.findByRecepcionFrutaUuid(recepcion.getUuid()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RecepcionFrutaFotoResponseDTO upload(UUID recepcionUuid, TipoFotoRecepcion tipo, MultipartFile file) {
        RecepcionFruta recepcion = requireRecepcionAccess(recepcionUuid);

        fotoRepository.findByRecepcionFrutaUuidAndTipo(recepcionUuid, tipo).ifPresent(existing -> {
            fileStorageService.deleteIfExists(existing.getStoragePath());
            fotoRepository.delete(existing);
        });

        FileStorageService.StoredFile stored =
                fileStorageService.storeRecepcionFoto(recepcionUuid, tipo.name(), file);

        RecepcionFrutaFoto foto = new RecepcionFrutaFoto();
        foto.setRecepcionFruta(recepcion);
        foto.setTipo(tipo);
        foto.setContentType(stored.contentType());
        foto.setStoragePath(fileStorageService.toRelativeStoragePath(stored.absolutePath()));
        foto = fotoRepository.save(foto);

        log.info("Foto {} guardada para recepción {} (uuid foto: {})", tipo, recepcion.getFolio(), foto.getUuid());
        return toDto(foto);
    }

    @Transactional(readOnly = true)
    public Resource download(UUID recepcionUuid, UUID fotoUuid) {
        requireRecepcionAccess(recepcionUuid);
        RecepcionFrutaFoto foto = fotoRepository.findById(fotoUuid)
                .orElseThrow(() -> new NotFoundException("Foto de recepción", fotoUuid));
        if (!foto.getRecepcionFruta().getUuid().equals(recepcionUuid)) {
            throw new NotFoundException("Foto de recepción", fotoUuid);
        }
        return fileStorageService.loadAsResource(foto.getStoragePath());
    }

    @Transactional(readOnly = true)
    public String getContentType(UUID recepcionUuid, UUID fotoUuid) {
        requireRecepcionAccess(recepcionUuid);
        RecepcionFrutaFoto foto = fotoRepository.findById(fotoUuid)
                .orElseThrow(() -> new NotFoundException("Foto de recepción", fotoUuid));
        return foto.getContentType() != null ? foto.getContentType() : "image/jpeg";
    }

    private RecepcionFruta requireRecepcionAccess(UUID recepcionUuid) {
        RecepcionFruta recepcion = recepcionFrutaService.findByUuid(recepcionUuid);
        scopeService.requireCentroAcopioAccess(recepcion.getCentroAcopio().getUuid());
        return recepcion;
    }

    private RecepcionFrutaFotoResponseDTO toDto(RecepcionFrutaFoto foto) {
        return RecepcionFrutaFotoResponseDTO.builder()
                .uuid(foto.getUuid())
                .recepcionFrutaUuid(foto.getRecepcionFruta().getUuid())
                .tipo(foto.getTipo())
                .contentType(foto.getContentType())
                .createdAt(foto.getCreatedAt())
                .build();
    }
}
