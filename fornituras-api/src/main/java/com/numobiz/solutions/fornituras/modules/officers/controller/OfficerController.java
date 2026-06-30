package com.numobiz.solutions.fornituras.modules.officers.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerCreateRequest;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerDetail;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerSummary;
import com.numobiz.solutions.fornituras.modules.officers.service.OfficerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del padrón de elementos. <b>Maneja PII de alta sensibilidad.</b> El listado y la ficha son
 * para cualquier rol autenticado, pero la ficha enmascara CURP/RFC salvo para ADMIN y el acceso se
 * audita (lo decide el servicio, no el cliente). El alta queda restringida a ADMIN/CAPTURISTA. La
 * búsqueda usa solo blind index/placa: ninguna PII viaja en claro por la URL.
 */
@RestController
@RequestMapping("/api/v1/officers")
@Tag(name = "Officers", description = "Padrón de elementos policiales (PII de alta sensibilidad)")
@SecurityRequirement(name = "Bearer Authentication")
public class OfficerController {

	private final OfficerService service;

	public OfficerController(OfficerService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar padrón", description = "Listado paginado con búsqueda (placa o CURP/RFC exacta vía blind index) y filtros por municipio y sexo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<OfficerSummary>>> getAll(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Long municipioId,
			@RequestParam(required = false) Long sexoId,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(q, municipioId, sexoId, pageable)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Ficha de elemento", description = "Ficha del elemento; CURP/RFC se enmascaran salvo para ADMIN. El acceso queda auditado (VIEW_OFFICER).")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<OfficerDetail>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
	}

	@PostMapping
	@Operation(summary = "Alta de elemento", description = "Registra un elemento con placa única. CURP/RFC opcionales (ADR 0003). Solo ADMIN/CAPTURISTA.")
	@PreAuthorize("hasAnyRole('ADMIN','CAPTURISTA')")
	public ResponseEntity<ApiResponse<OfficerDetail>> create(
			@Valid @RequestBody OfficerCreateRequest request) {
		OfficerDetail created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Elemento registrado.", created));
	}
}
