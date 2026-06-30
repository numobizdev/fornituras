package mx.uumbal.solutions.palm_flow.modules.centros_acopio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.service.CentroAcopioService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/centros-acopio")
@RequiredArgsConstructor
@Tag(name = "Centros de Acopio", description = "Gestión de centros de acopio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class CentroAcopioController {

    private final CentroAcopioService centroAcopioService;

    @GetMapping
    @Operation(summary = "Listar centros de acopio (paginado, filtro opcional por región)")
    public ResponseEntity<ApiResponse<PageResponse<CentroAcopioResponseDTO>>> getAll(
            @RequestParam(required = false) Long regionId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(centroAcopioService.getAll(regionId, pageable))));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Obtener centro de acopio por UUID")
    public ResponseEntity<ApiResponse<CentroAcopioResponseDTO>> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(centroAcopioService.getByUuid(uuid)));
    }

    @PostMapping
    @Operation(summary = "Crear centro de acopio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<CentroAcopioResponseDTO>> create(
            @Valid @RequestBody CentroAcopioRequestDTO request) {
        CentroAcopioResponseDTO created = centroAcopioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Centro de acopio creado exitosamente", created));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Actualizar centro de acopio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<CentroAcopioResponseDTO>> update(
            @PathVariable UUID uuid, @Valid @RequestBody CentroAcopioRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Centro de acopio actualizado", centroAcopioService.update(uuid, request)));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar centro de acopio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID uuid) {
        centroAcopioService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Centro de acopio eliminado")
                .build());
    }
}
