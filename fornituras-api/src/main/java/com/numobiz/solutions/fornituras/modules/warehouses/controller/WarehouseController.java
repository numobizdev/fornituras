package com.numobiz.solutions.fornituras.modules.warehouses.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseCreateRequest;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseDetail;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseSummary;
import com.numobiz.solutions.fornituras.modules.warehouses.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/**
 * API de almacenes. El listado (campos no sensibles) es para cualquier rol autenticado; el detalle
 * (incluye ubicación, responsable y contacto) y toda escritura quedan restringidos a ADMIN.
 */
@RestController
@RequestMapping("/api/v1/warehouses")
@Tag(name = "Warehouses", description = "Almacenes (ubicaciones de resguardo de fornituras)")
@SecurityRequirement(name = "Bearer Authentication")
public class WarehouseController {

	private final WarehouseService service;

	public WarehouseController(WarehouseService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar almacenes", description = "Listado paginado (campos no sensibles); filtros por estado activo y tipo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<WarehouseSummary>>> getAll(
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) Long tipoItemId,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(active, tipoItemId, pageable)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detalle de almacén", description = "Ficha completa con campos sensibles (ubicación, responsable, contacto). Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<WarehouseDetail>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
	}

	@PostMapping
	@Operation(summary = "Crear almacén", description = "Crea un almacén con clave y nombre únicos. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<WarehouseDetail>> create(
			@Valid @RequestBody WarehouseCreateRequest request) {
		WarehouseDetail created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Almacén creado.", created));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Editar almacén", description = "Edita un almacén manteniendo unicidad de clave/nombre. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<WarehouseDetail>> update(
			@PathVariable Long id,
			@Valid @RequestBody WarehouseCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Almacén actualizado.", service.update(id, request)));
	}

	@PatchMapping("/{id}/deactivate")
	@Operation(summary = "Desactivar almacén", description = "Marca el almacén como inactivo (no borrado). Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
		service.deactivate(id);
		return ResponseEntity.ok(ApiResponse.ok("Almacén desactivado."));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar almacén", description = "Borra el almacén solo si no tiene fornituras/traslados asociados; en uso se bloquea (desactívelo). Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.ok("Almacén eliminado."));
	}
}
