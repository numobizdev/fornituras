package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaFotoResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.TipoFotoRecepcion;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service.RecepcionFrutaFotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/recepciones-fruta/{recepcionUuid}/fotos")
@RequiredArgsConstructor
@Tag(name = "Recepción de Fruta - Fotos", description = "Fotos de báscula asociadas a recepciones")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class RecepcionFrutaFotoController {

    private final RecepcionFrutaFotoService fotoService;

    @GetMapping
    @Operation(summary = "Listar fotos de una recepción")
    public ResponseEntity<ApiResponse<List<RecepcionFrutaFotoResponseDTO>>> list(
            @PathVariable UUID recepcionUuid) {
        return ResponseEntity.ok(ApiResponse.success(fotoService.listByRecepcion(recepcionUuid)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir foto de báscula (reemplaza si ya existe para el mismo tipo)")
    public ResponseEntity<ApiResponse<RecepcionFrutaFotoResponseDTO>> upload(
            @PathVariable UUID recepcionUuid,
            @RequestParam TipoFotoRecepcion tipo,
            @RequestParam("file") MultipartFile file) {
        RecepcionFrutaFotoResponseDTO created = fotoService.upload(recepcionUuid, tipo, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Foto guardada", created));
    }

    @GetMapping("/{fotoUuid}")
    @Operation(summary = "Descargar foto de recepción")
    public ResponseEntity<Resource> download(
            @PathVariable UUID recepcionUuid,
            @PathVariable UUID fotoUuid) {
        Resource resource = fotoService.download(recepcionUuid, fotoUuid);
        String contentType = fotoService.getContentType(recepcionUuid, fotoUuid);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fotoUuid + "\"")
                .body(resource);
    }
}
