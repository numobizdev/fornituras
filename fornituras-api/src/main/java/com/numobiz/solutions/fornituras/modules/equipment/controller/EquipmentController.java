package com.numobiz.solutions.fornituras.modules.equipment.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.common.exception.TooManyRequestsException;
import com.numobiz.solutions.fornituras.common.ratelimit.RateLimiter;
import com.numobiz.solutions.fornituras.modules.equipment.dto.BatchCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentDetail;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentSummary;
import com.numobiz.solutions.fornituras.modules.equipment.dto.StatusChangeRequest;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API del inventario de fornituras. La consulta (sin PII del elemento) es para cualquier rol
 * autenticado; el alta, edición y cambio de estado se restringen a los roles que capturan inventario
 * ({@link RolePolicy#WRITE_INVENTORY}: ADMIN, ALMACEN y CAPTURISTA), según el ADR 0013.
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment", description = "Inventario de fornituras (equipos de blindaje y dotación)")
@SecurityRequirement(name = "Bearer Authentication")
public class EquipmentController {

	private final EquipmentService service;
	private final RateLimiter rateLimiter;

	public EquipmentController(EquipmentService service, RateLimiter rateLimiter) {
		this.service = service;
		this.rateLimiter = rateLimiter;
	}

	@GetMapping
	@Operation(summary = "Listar fornituras", description = "Listado paginado con filtros (texto código/descripción, estado, tipo, talla, almacén). Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<EquipmentSummary>>> getAll(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) EquipmentStatus status,
			@RequestParam(required = false) Long equipmentTypeId,
			@RequestParam(required = false) Long sizeId,
			@RequestParam(required = false) Long warehouseId,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(
				service.findAll(q, status, equipmentTypeId, sizeId, warehouseId, pageable)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detalle de fornitura", description = "Ficha completa (sin datos del elemento asignado). Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<EquipmentDetail>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
	}

	@GetMapping("/by-codigo/{codigo}")
	@Operation(summary = "Resolver fornitura por código", description = "Resolución server-side del código QR/serie (consumible por asignación, traslados y bajas).")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<EquipmentDetail>> getByCodigo(@PathVariable String codigo) {
		// Limita la enumeración de códigos opacos (ADR 0005/0010): tope por actor y ventana.
		if (!rateLimiter.tryConsume("equipment:by-codigo:" + currentActor())) {
			throw new TooManyRequestsException(
					"Demasiadas consultas por código; intente de nuevo en un momento.");
		}
		return ResponseEntity.ok(ApiResponse.ok(service.findByCodigo(codigo)));
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication == null) ? "anonymous" : authentication.getName();
	}

	@PostMapping
	@Operation(summary = "Alta de fornitura", description = "Crea una fornitura con código único. Roles de escritura de inventario (ADMIN/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_INVENTORY)
	public ResponseEntity<ApiResponse<EquipmentDetail>> create(
			@Valid @RequestBody EquipmentCreateRequest request) {
		EquipmentDetail created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Fornitura registrada.", created));
	}

	@PostMapping("/batch")
	@Operation(summary = "Alta por lote", description = "Crea N fornituras con datos generales comunes y códigos distintos, de forma atómica. Roles de escritura de inventario (ADMIN/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_INVENTORY)
	public ResponseEntity<ApiResponse<List<EquipmentDetail>>> createBatch(
			@Valid @RequestBody BatchCreateRequest request) {
		List<EquipmentDetail> created = service.createBatch(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Lote registrado: " + created.size() + " fornituras.", created));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Editar fornitura", description = "Edita atributos no identitarios (el código es inmutable). Roles de escritura de inventario (ADMIN/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_INVENTORY)
	public ResponseEntity<ApiResponse<EquipmentDetail>> update(
			@PathVariable Long id,
			@Valid @RequestBody EquipmentCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Fornitura actualizada.", service.update(id, request)));
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Cambiar estado", description = "Cambia el estado operativo; bloquea baja/traslado con asignación vigente. Roles de escritura de inventario (ADMIN/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_INVENTORY)
	public ResponseEntity<ApiResponse<EquipmentDetail>> changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody StatusChangeRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Estado actualizado.", service.changeStatus(id, request.status())));
	}
}
