package com.numobiz.solutions.fornituras.modules.catalog.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemCreateRequest;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemSummary;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogSummary;
import com.numobiz.solutions.fornituras.modules.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

/**
 * CRUD genérico de catálogos (ADR 0007). El mismo endpoint sirve a todos los catálogos: el catálogo
 * se identifica por su {@code code} en la ruta. Lectura para cualquier rol autenticado; escritura
 * solo ADMIN.
 */
@RestController
@RequestMapping("/api/v1/catalogs")
@Tag(name = "Catalogs", description = "Catálogos genéricos (tipos de fornitura, tallas, tipo de almacén…)")
@SecurityRequirement(name = "Bearer Authentication")
public class CatalogController {

	private final CatalogService service;

	public CatalogController(CatalogService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar catálogos", description = "Catálogos activos administrables. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<CatalogSummary>>> getCatalogs() {
		return ResponseEntity.ok(ApiResponse.ok(service.findCatalogs()));
	}

	@GetMapping("/{code}/items")
	@Operation(summary = "Listar valores (paginado)", description = "Valores de un catálogo; filtro opcional por estado activo. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<CatalogItemSummary>>> getItems(
			@PathVariable String code,
			@RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findItems(code, active, pageable)));
	}

	@GetMapping("/items/{itemId}")
	@Operation(summary = "Detalle de valor", description = "Un valor de catálogo por id. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<CatalogItemSummary>> getItem(@PathVariable Long itemId) {
		return ResponseEntity.ok(ApiResponse.ok(service.findItem(itemId)));
	}

	@GetMapping("/{code}/items/active")
	@Operation(summary = "Valores activos (selector)", description = "Valores activos para selectores; filtro opcional por valor padre (jerarquía). Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<CatalogItemSummary>>> getActiveItems(
			@PathVariable String code,
			@RequestParam(required = false) Long parentItemId) {
		return ResponseEntity.ok(ApiResponse.ok(service.findActiveItems(code, parentItemId)));
	}

	@PostMapping("/{code}/items")
	@Operation(summary = "Crear valor", description = "Crea un valor con nombre único dentro del catálogo. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<CatalogItemSummary>> createItem(
			@PathVariable String code,
			@Valid @RequestBody CatalogItemCreateRequest request) {
		CatalogItemSummary created = service.createItem(code, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Valor de catálogo creado.", created));
	}

	@PutMapping("/items/{itemId}")
	@Operation(summary = "Editar valor", description = "Edita un valor manteniendo unicidad de nombre en su catálogo. Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<CatalogItemSummary>> updateItem(
			@PathVariable Long itemId,
			@Valid @RequestBody CatalogItemCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Valor de catálogo actualizado.", service.updateItem(itemId, request)));
	}

	@PatchMapping("/items/{itemId}/deactivate")
	@Operation(summary = "Desactivar valor", description = "Marca el valor como inactivo (no borrado). Solo ADMIN.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deactivateItem(@PathVariable Long itemId) {
		service.deactivateItem(itemId);
		return ResponseEntity.ok(ApiResponse.ok("Valor de catálogo desactivado."));
	}
}
