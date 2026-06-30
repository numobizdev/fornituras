package mx.uumbal.solutions.palm_flow.modules.geografia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.service.EstadoService;
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
@RequestMapping("/api/v1/estados")
@RequiredArgsConstructor
@Tag(name = "Estados", description = "Catálogo geográfico de estados")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class EstadoController {

    private final EstadoService estadoService;

    @GetMapping
    @Operation(summary = "Listar estados (paginado)")
    public ResponseEntity<ApiResponse<PageResponse<EstadoResponseDTO>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(estadoService.getAll(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estado por ID")
    public ResponseEntity<ApiResponse<EstadoResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(estadoService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<EstadoResponseDTO>> create(@Valid @RequestBody EstadoRequestDTO request) {
        EstadoResponseDTO created = estadoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Estado creado exitosamente", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<EstadoResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody EstadoRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Estado actualizado", estadoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        estadoService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Estado eliminado")
                .build());
    }
}
