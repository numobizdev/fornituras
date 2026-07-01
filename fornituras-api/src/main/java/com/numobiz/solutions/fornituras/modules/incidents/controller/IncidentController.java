package com.numobiz.solutions.fornituras.modules.incidents.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentCreateRequest;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentSummary;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentUpdateRequest;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.service.IncidentService;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de incidencias sobre fornituras. Consultar es para cualquier rol autenticado; reportar y
 * actualizar quedan restringidos a {@link RolePolicy#WRITE_OPERATIONS} (ADMIN/SUPERVISOR/CAPTURISTA).
 * Toda mutación se audita y respeta la consistencia de estado de la fornitura (retiro al reportar,
 * retorno al resolver).
 */
@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Incidencias y mantenimiento de fornituras")
@SecurityRequirement(name = "Bearer Authentication")
public class IncidentController {

	private final IncidentService service;

	public IncidentController(IncidentService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar incidencias", description = "Paginado con filtro opcional por estado. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<IncidentSummary>>> list(
			@RequestParam(required = false) IncidentStatus estado,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(estado, pageable)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Ficha de incidencia", description = "Una incidencia por id. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<IncidentSummary>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
	}

	@PostMapping
	@Operation(summary = "Reportar incidencia", description = "Crea una incidencia abierta y retira la fornitura si aplica. Roles de operación (ADMIN/SUPERVISOR/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_OPERATIONS)
	public ResponseEntity<ApiResponse<IncidentSummary>> report(@Valid @RequestBody IncidentCreateRequest request) {
		IncidentSummary created = service.report(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Incidencia reportada.", created));
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Actualizar incidencia", description = "Cambia el estado; al resolver/cerrar devuelve la fornitura a disponible si procede. Roles de operación (ADMIN/SUPERVISOR/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_OPERATIONS)
	public ResponseEntity<ApiResponse<IncidentSummary>> update(
			@PathVariable Long id,
			@Valid @RequestBody IncidentUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Incidencia actualizada.", service.update(id, request)));
	}
}
