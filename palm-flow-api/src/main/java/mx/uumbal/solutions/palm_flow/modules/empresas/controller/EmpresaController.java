package mx.uumbal.solutions.palm_flow.modules.empresas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.service.EmpresaService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gestión de empresas (multitenant) — requiere SUPER_ADMIN")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    @Operation(summary = "Listar empresas (paginado)")
    public ResponseEntity<ApiResponse<PageResponse<EmpresaResponseDTO>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(empresaService.getAll(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener empresa por ID")
    public ResponseEntity<ApiResponse<EmpresaResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(empresaService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Obtener empresa por slug")
    public ResponseEntity<ApiResponse<EmpresaResponseDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(empresaService.getBySlug(slug)));
    }

    @PostMapping
    @Operation(summary = "Crear empresa")
    public ResponseEntity<ApiResponse<EmpresaResponseDTO>> create(@Valid @RequestBody EmpresaRequestDTO request) {
        EmpresaResponseDTO created = empresaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Empresa creada exitosamente", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar empresa")
    public ResponseEntity<ApiResponse<EmpresaResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody EmpresaRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Empresa actualizada", empresaService.update(id, request)));
    }

    @PatchMapping("/{id}/toggle-activo")
    @Operation(summary = "Activar / desactivar empresa")
    public ResponseEntity<ApiResponse<Void>> toggleActivo(@PathVariable Long id) {
        empresaService.toggleActivo(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Estado de empresa actualizado")
                .build());
    }
}
