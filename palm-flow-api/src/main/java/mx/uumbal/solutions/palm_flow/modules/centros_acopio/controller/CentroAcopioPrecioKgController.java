package mx.uumbal.solutions.palm_flow.modules.centros_acopio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mx.uumbal.solutions.palm_flow.common.dto.ApiResponse;
import mx.uumbal.solutions.palm_flow.common.dto.PageResponse;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioPrecioKgRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioPrecioKgResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.service.CentroAcopioPrecioKgService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/centros-acopio/{centroUuid}/precios-kg")
@RequiredArgsConstructor
@Tag(name = "Precios kg - Centros de Acopio", description = "Historial de precio por kg por centro de acopio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA','ENCARGADO_CENTRO_ACOPIO')")
public class CentroAcopioPrecioKgController {

    private final CentroAcopioPrecioKgService precioKgService;

    @GetMapping
    @Operation(summary = "Listar historial de precios por kg (paginado)")
    public ResponseEntity<ApiResponse<PageResponse<CentroAcopioPrecioKgResponseDTO>>> getHistory(
            @PathVariable UUID centroUuid,
            @PageableDefault(size = 20, sort = "fechaVigencia", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(precioKgService.getHistory(centroUuid, pageable))));
    }

    @GetMapping("/vigente")
    @Operation(summary = "Obtener precio vigente para una fecha (default: hoy UTC)")
    public ResponseEntity<ApiResponse<CentroAcopioPrecioKgResponseDTO>> getVigente(
            @PathVariable UUID centroUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.success(precioKgService.getVigente(centroUuid, fecha)));
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo precio por kg")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public ResponseEntity<ApiResponse<CentroAcopioPrecioKgResponseDTO>> create(
            @PathVariable UUID centroUuid,
            @Valid @RequestBody CentroAcopioPrecioKgRequestDTO request) {
        CentroAcopioPrecioKgResponseDTO created = precioKgService.create(centroUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Precio por kg registrado exitosamente", created));
    }
}
