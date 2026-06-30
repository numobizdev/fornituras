package com.numobiz.solutions.fornituras.modules.equipmenttypes.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.service.SizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/sizes")
@Tag(name = "Sizes", description = "Catálogo de tallas")
@SecurityRequirement(name = "Bearer Authentication")
public class SizeController {

	private final SizeService service;

	public SizeController(SizeService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar tallas", description = "Tallas activas; filtro opcional por tipo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<SizeSummary>>> getAll(
			@RequestParam(required = false) Long equipmentTypeId) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(equipmentTypeId)));
	}

	@PostMapping
	@Operation(summary = "Crear talla", description = "Crea una talla, opcionalmente asociada a un tipo. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<SizeSummary>> create(@Valid @RequestBody SizeCreateRequest request) {
		SizeSummary created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Talla creada.", created));
	}

	@PatchMapping("/{id}/deactivate")
	@Operation(summary = "Desactivar talla", description = "Marca la talla como inactiva (no borrado). Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
		service.deactivate(id);
		return ResponseEntity.ok(ApiResponse.ok("Talla desactivada."));
	}
}
