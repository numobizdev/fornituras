package com.numobiz.solutions.fornituras.modules.decommissions.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.decommissions.dto.DecommissionReasonItem;
import com.numobiz.solutions.fornituras.modules.decommissions.dto.DecommissionRequest;
import com.numobiz.solutions.fornituras.modules.decommissions.dto.DecommissionSummary;
import com.numobiz.solutions.fornituras.modules.decommissions.service.DecommissionService;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API de bajas definitivas de fornituras. Dar de baja está restringido a mando
 * ({@link RolePolicy#AUTHORIZE_DECOMMISSION}: ADMIN/SUPERVISOR, acción elevada); consultar el listado y
 * el catálogo de motivos es para cualquier rol autenticado. Toda baja se audita y respeta el bloqueo por
 * asignación vigente/traslado en curso (delegado a 001).
 */
@RestController
@RequestMapping("/api/v1/decommissions")
@Tag(name = "Decommissions", description = "Bajas definitivas de fornituras")
@SecurityRequirement(name = "Bearer Authentication")
public class DecommissionController {

	private final DecommissionService service;

	public DecommissionController(DecommissionService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar bajas", description = "Paginado con filtros por fecha, tipo y motivo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<DecommissionSummary>>> list(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
			@RequestParam(required = false) Long tipoId,
			@RequestParam(required = false) Long motivoId,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(
				service.findAll(fechaDesde, fechaHasta, tipoId, motivoId, pageable)));
	}

	@GetMapping("/reasons")
	@Operation(summary = "Catálogo de motivos", description = "Motivos de baja activos para el formulario. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<DecommissionReasonItem>>> reasons() {
		return ResponseEntity.ok(ApiResponse.ok(service.findReasons()));
	}

	@PostMapping
	@Operation(summary = "Dar de baja", description = "Da de baja una fornitura por código con un motivo. Bloquea si tiene asignación vigente o traslado en curso. Solo ADMIN/SUPERVISOR.")
	@PreAuthorize(RolePolicy.AUTHORIZE_DECOMMISSION)
	public ResponseEntity<ApiResponse<DecommissionSummary>> decommission(
			@Valid @RequestBody DecommissionRequest request) {
		DecommissionSummary created = service.decommission(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Fornitura dada de baja.", created));
	}
}
