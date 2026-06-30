package mx.uumbal.solutions.palm_flow.modules.productores.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.service.PredioService;
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
@RequestMapping("/api/v1/predios")
@RequiredArgsConstructor
@Tag(name = "Predios", description = "Gestión de predios asociados a productores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class PredioController {

    private final PredioService predioService;

    @GetMapping
    @Operation(summary = "Listar predios (paginado, filtro opcional por productor)")
    public ResponseEntity<ApiResponse<PageResponse<PredioResponseDTO>>> getAll(
            @RequestParam(required = false) UUID productorUuid,
            @RequestParam(defaultValue = "false") boolean includeLotes,
            @PageableDefault(size = 20, sort = {"productor.nombre", "nombre"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(predioService.getAll(productorUuid, includeLotes, pageable))));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Obtener predio por UUID")
    public ResponseEntity<ApiResponse<PredioResponseDTO>> getByUuid(
            @PathVariable UUID uuid,
            @RequestParam(defaultValue = "false") boolean includeLotes) {
        return ResponseEntity.ok(ApiResponse.success(predioService.getByUuid(uuid, includeLotes)));
    }

    @PostMapping
    @Operation(summary = "Crear predio")
    public ResponseEntity<ApiResponse<PredioResponseDTO>> create(@Valid @RequestBody PredioRequestDTO request) {
        PredioResponseDTO created = predioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Predio creado exitosamente", created));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Actualizar predio")
    public ResponseEntity<ApiResponse<PredioResponseDTO>> update(
            @PathVariable UUID uuid, @Valid @RequestBody PredioRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Predio actualizado", predioService.update(uuid, request)));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar predio")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID uuid) {
        predioService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Predio eliminado")
                .build());
    }
}
