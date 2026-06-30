package mx.uumbal.solutions.palm_flow.modules.productores.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.service.ProductorService;
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
@RequestMapping("/api/v1/productores")
@RequiredArgsConstructor
@Tag(name = "Productores", description = "Gestión de productores de palma")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class ProductorController {

    private final ProductorService productorService;

    @GetMapping
    @Operation(summary = "Listar productores (paginado, filtro opcional por centro de acopio)")
    public ResponseEntity<ApiResponse<PageResponse<ProductorResponseDTO>>> getAll(
            @RequestParam(required = false) UUID centroAcopioUuid,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(productorService.getAll(centroAcopioUuid, pageable))));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Obtener productor por UUID")
    public ResponseEntity<ApiResponse<ProductorResponseDTO>> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(productorService.getByUuid(uuid)));
    }

    @PostMapping
    @Operation(summary = "Crear productor")
    public ResponseEntity<ApiResponse<ProductorResponseDTO>> create(@Valid @RequestBody ProductorRequestDTO request) {
        ProductorResponseDTO created = productorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Productor creado exitosamente", created));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Actualizar productor")
    public ResponseEntity<ApiResponse<ProductorResponseDTO>> update(
            @PathVariable UUID uuid, @Valid @RequestBody ProductorRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Productor actualizado", productorService.update(uuid, request)));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar productor")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID uuid) {
        productorService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Productor eliminado")
                .build());
    }
}
