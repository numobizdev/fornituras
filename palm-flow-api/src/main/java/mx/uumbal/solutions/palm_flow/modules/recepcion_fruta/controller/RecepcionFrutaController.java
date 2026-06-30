package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service.RecepcionFrutaService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recepciones-fruta")
@RequiredArgsConstructor
@Tag(name = "Recepci?n de Fruta Fresca", description = "Registro transaccional de recepci?n de fruta en centros de acopio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class RecepcionFrutaController {

    private final RecepcionFrutaService recepcionFrutaService;

    @GetMapping
    @Operation(summary = "Listar recepciones (paginado, filtros opcionales)")
    public ResponseEntity<ApiResponse<PageResponse<RecepcionFrutaResponseDTO>>> getAll(
            @RequestParam(required = false) UUID centroAcopioUuid,
            @RequestParam(required = false) UUID productorUuid,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                recepcionFrutaService.getAll(centroAcopioUuid, productorUuid, pageable))));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Obtener recepci?n por UUID")
    public ResponseEntity<ApiResponse<RecepcionFrutaResponseDTO>> getByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(recepcionFrutaService.getByUuid(uuid)));
    }

    @PostMapping
    @Operation(summary = "Registrar recepci?n de fruta (genera folio autom?tico)")
    public ResponseEntity<ApiResponse<RecepcionFrutaResponseDTO>> create(
            @Valid @RequestBody RecepcionFrutaRequestDTO request) {
        RecepcionFrutaResponseDTO created = recepcionFrutaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recepci?n registrada exitosamente", created));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Actualizar recepci?n de fruta")
    public ResponseEntity<ApiResponse<RecepcionFrutaResponseDTO>> update(
            @PathVariable UUID uuid, @Valid @RequestBody RecepcionFrutaRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Recepci?n actualizada", recepcionFrutaService.update(uuid, request)));
    }

    @PatchMapping("/{uuid}/sync")
    @Operation(summary = "Marcar recepci?n como sincronizada (offline ÿÿÿ online)")
    public ResponseEntity<ApiResponse<RecepcionFrutaResponseDTO>> markSynced(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(
                "Recepci?n sincronizada", recepcionFrutaService.markSynced(uuid)));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Eliminar recepci?n de fruta")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID uuid) {
        recepcionFrutaService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Recepci?n eliminada")
                .build());
    }
}
