package com.numobiz.solutions.fornituras.modules.municipios.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioCreateRequest;
import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioSummary;
import com.numobiz.solutions.fornituras.modules.municipios.service.MunicipioService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de municipios. Lectura para cualquier rol autenticado (se usa como selector en
 * almacenes y elementos); alta restringida a ADMIN. El CRUD completo pertenece a la feature
 * 003-elementos-padron; aquí se expone el mínimo para desbloquear 005-almacenes.
 */
@RestController
@RequestMapping("/api/v1/municipios")
@Tag(name = "Municipios", description = "Catálogo geográfico de municipios")
@SecurityRequirement(name = "Bearer Authentication")
public class MunicipioController {

	private final MunicipioService service;

	public MunicipioController(MunicipioService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar municipios", description = "Listado paginado; filtro opcional por estado activo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<MunicipioSummary>>> getAll(
			@RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(active, pageable)));
	}

	@PostMapping
	@Operation(summary = "Crear municipio", description = "Crea un municipio con nombre único. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<MunicipioSummary>> create(
			@Valid @RequestBody MunicipioCreateRequest request) {
		MunicipioSummary created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Municipio creado.", created));
	}
}
