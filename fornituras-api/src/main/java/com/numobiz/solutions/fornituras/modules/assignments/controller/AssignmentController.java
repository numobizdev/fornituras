package com.numobiz.solutions.fornituras.modules.assignments.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.assignments.dto.AssignRequest;
import com.numobiz.solutions.fornituras.modules.assignments.dto.AssignmentSummary;
import com.numobiz.solutions.fornituras.modules.assignments.dto.ReassignRequest;
import com.numobiz.solutions.fornituras.modules.assignments.service.AssignmentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de asignaciones (resguardos). Listar las vigentes es para cualquier rol autenticado; asignar,
 * devolver y reasignar quedan restringidos a ADMIN/CAPTURISTA. Toda mutación se audita y respeta la
 * regla de una sola asignación vigente por fornitura.
 */
@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "Assignments", description = "Asignación de fornituras a elementos (resguardos)")
@SecurityRequirement(name = "Bearer Authentication")
public class AssignmentController {

	private static final String WRITE_ROLES = "hasAnyRole('ADMIN','CAPTURISTA')";

	private final AssignmentService service;

	public AssignmentController(AssignmentService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar asignaciones vigentes", description = "Qué fornitura tiene asignada cada elemento, paginado. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<AssignmentSummary>>> getVigentes(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findVigentes(pageable)));
	}

	@PostMapping
	@Operation(summary = "Asignar fornitura", description = "Asigna una fornitura disponible a un elemento; 409 si ya está asignada o no disponible. Solo ADMIN/CAPTURISTA.")
	@PreAuthorize(WRITE_ROLES)
	public ResponseEntity<ApiResponse<AssignmentSummary>> assign(@Valid @RequestBody AssignRequest request) {
		AssignmentSummary created = service.assign(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Fornitura asignada.", created));
	}

	@PostMapping("/{id}/return")
	@Operation(summary = "Registrar devolución", description = "Cierra una asignación vigente y libera la fornitura (vuelve a disponible). Solo ADMIN/CAPTURISTA.")
	@PreAuthorize(WRITE_ROLES)
	public ResponseEntity<ApiResponse<AssignmentSummary>> returnAssignment(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok("Devolución registrada.", service.returnAssignment(id)));
	}

	@PostMapping("/reassign")
	@Operation(summary = "Reasignar fornitura", description = "Cierra la asignación vigente y abre una nueva para otro elemento, conservando el historial. Solo ADMIN/CAPTURISTA.")
	@PreAuthorize(WRITE_ROLES)
	public ResponseEntity<ApiResponse<AssignmentSummary>> reassign(@Valid @RequestBody ReassignRequest request) {
		AssignmentSummary created = service.reassign(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Fornitura reasignada.", created));
	}
}
