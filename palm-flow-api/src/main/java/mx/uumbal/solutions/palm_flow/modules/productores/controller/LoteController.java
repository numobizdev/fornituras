package mx.uumbal.solutions.palm_flow.modules.productores.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.service.LoteService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
@Tag(name = "Lotes", description = "Gestión de lotes asociados a predios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    @Operation(summary = "Listar lotes (paginado, filtro por predio requerido)")
    public ResponseEntity<ApiResponse<PageResponse<LoteResponseDTO>>> getAll(
            @RequestParam UUID predioUuid,
            @PageableDefault(size = 50, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(loteService.getAll(predioUuid, pageable))));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Obtener lote por UUID")
    public ResponseEntity<ApiResponse<LoteResponseDTO>> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(loteService.getByUuid(uuid)));
    }

    @PostMapping
    @Operation(summary = "Crear lote")
    public ResponseEntity<ApiResponse<LoteResponseDTO>> create(@Valid @RequestBody LoteRequestDTO request) {
        LoteResponseDTO created = loteService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lote creado exitosamente", created));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Actualizar lote")
    public ResponseEntity<ApiResponse<LoteResponseDTO>> update(
            @PathVariable UUID uuid, @Valid @RequestBody LoteRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Lote actualizado", loteService.update(uuid, request)));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar lote")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID uuid) {
        loteService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Lote eliminado")
                .build());
    }
}
