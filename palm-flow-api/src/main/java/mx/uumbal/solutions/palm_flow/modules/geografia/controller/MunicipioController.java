package mx.uumbal.solutions.palm_flow.modules.geografia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.MunicipioService;
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
@RequestMapping("/api/v1/municipios")
@RequiredArgsConstructor
@Tag(name = "Municipios", description = "Catálogo geográfico de municipios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class MunicipioController {

    private final MunicipioService municipioService;

    @GetMapping
    @Operation(summary = "Listar municipios (paginado, filtro opcional por estado)")
    public ResponseEntity<ApiResponse<PageResponse<MunicipioResponseDTO>>> getAll(
            @RequestParam(required = false) Long estadoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(municipioService.getAll(estadoId, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener municipio por ID")
    public ResponseEntity<ApiResponse<MunicipioResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(municipioService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear municipio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<MunicipioResponseDTO>> create(@Valid @RequestBody MunicipioRequestDTO request) {
        MunicipioResponseDTO created = municipioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Municipio creado exitosamente", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar municipio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<MunicipioResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody MunicipioRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Municipio actualizado", municipioService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar municipio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        municipioService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Municipio eliminado")
                .build());
    }
}
