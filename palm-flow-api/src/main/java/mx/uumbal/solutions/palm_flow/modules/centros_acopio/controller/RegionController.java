package mx.uumbal.solutions.palm_flow.modules.centros_acopio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.service.RegionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regiones")
@RequiredArgsConstructor
@Tag(name = "Regiones", description = "Regiones de centros de acopio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    @Operation(summary = "Listar regiones (paginado)")
    public ResponseEntity<ApiResponse<PageResponse<RegionResponseDTO>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(regionService.getAll(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener región por ID")
    public ResponseEntity<ApiResponse<RegionResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(regionService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear región")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<RegionResponseDTO>> create(@Valid @RequestBody RegionRequestDTO request) {
        RegionResponseDTO created = regionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Región creada exitosamente", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar región")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<RegionResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody RegionRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Región actualizada", regionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar región")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        regionService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Región eliminada")
                .build());
    }
}
