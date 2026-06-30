package com.numobiz.solutions.fornituras.modules.equipmenttypes.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeDetail;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.service.EquipmentTypeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/equipment-types")
@Tag(name = "Equipment Types", description = "Catálogo de tipos de fornitura")
@SecurityRequirement(name = "Bearer Authentication")
public class EquipmentTypeController {

	private final EquipmentTypeService service;

	public EquipmentTypeController(EquipmentTypeService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar tipos", description = "Listado paginado; filtro opcional por estado activo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<EquipmentTypeSummary>>> getAll(
			@RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(active, pageable)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detalle de tipo", description = "Tipo con sus tallas activas. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<EquipmentTypeDetail>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
	}

	@PostMapping
	@Operation(summary = "Crear tipo", description = "Crea un tipo con nombre único. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<EquipmentTypeDetail>> create(
			@Valid @RequestBody EquipmentTypeCreateRequest request) {
		EquipmentTypeDetail created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Tipo de fornitura creado.", created));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Editar tipo", description = "Edita un tipo manteniendo unicidad de nombre. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<EquipmentTypeDetail>> update(
			@PathVariable Long id,
			@Valid @RequestBody EquipmentTypeCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Tipo de fornitura actualizado.", service.update(id, request)));
	}

	@PatchMapping("/{id}/deactivate")
	@Operation(summary = "Desactivar tipo", description = "Marca el tipo como inactivo (no borrado). Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
		service.deactivate(id);
		return ResponseEntity.ok(ApiResponse.ok("Tipo de fornitura desactivado."));
	}
}
