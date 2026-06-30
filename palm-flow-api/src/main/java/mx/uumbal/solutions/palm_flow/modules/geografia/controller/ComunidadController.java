package mx.uumbal.solutions.palm_flow.modules.geografia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.ComunidadService;
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
@RequestMapping("/api/v1/comunidades")
@RequiredArgsConstructor
@Tag(name = "Comunidades", description = "Catálogo geográfico de comunidades")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class ComunidadController {

    private final ComunidadService comunidadService;

    @GetMapping
    @Operation(summary = "Listar comunidades (paginado, filtro opcional por municipio)")
    public ResponseEntity<ApiResponse<PageResponse<ComunidadResponseDTO>>> getAll(
            @RequestParam(required = false) Long municipioId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(comunidadService.getAll(municipioId, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener comunidad por ID")
    public ResponseEntity<ApiResponse<ComunidadResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(comunidadService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear comunidad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<ComunidadResponseDTO>> create(@Valid @RequestBody ComunidadRequestDTO request) {
        ComunidadResponseDTO created = comunidadService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comunidad creada exitosamente", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar comunidad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<ComunidadResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody ComunidadRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Comunidad actualizada", comunidadService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar comunidad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        comunidadService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Comunidad eliminada")
                .build());
    }
}
