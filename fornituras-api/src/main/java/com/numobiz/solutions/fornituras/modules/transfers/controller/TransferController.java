package com.numobiz.solutions.fornituras.modules.transfers.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferCreateRequest;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferDetail;
import com.numobiz.solutions.fornituras.modules.transfers.dto.TransferSummary;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;
import com.numobiz.solutions.fornituras.modules.transfers.service.TransferService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de traslados entre almacenes. Consultar es para cualquier rol autenticado; crear, recibir y
 * cancelar quedan restringidos a {@link RolePolicy#WRITE_TRANSFERS} (ADMIN/SUPERVISOR autorizan,
 * ALMACEN administra, CAPTURISTA captura). Toda mutación se audita y respeta la consistencia de estado
 * (fornituras disponibles del origen → en traslado → disponibles en destino/origen).
 */
@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Traslados de fornituras entre almacenes")
@SecurityRequirement(name = "Bearer Authentication")
public class TransferController {

	private final TransferService service;

	public TransferController(TransferService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Listar traslados", description = "Paginado con filtros por origen, destino y estado. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<TransferSummary>>> list(
			@RequestParam(required = false) Long origenId,
			@RequestParam(required = false) Long destinoId,
			@RequestParam(required = false) TransferStatus status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.findAll(origenId, destinoId, status, pageable)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Ficha de traslado", description = "Cabecera + fornituras incluidas. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<TransferDetail>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
	}

	@PostMapping
	@Operation(summary = "Crear traslado", description = "Mueve fornituras disponibles del origen; quedan en traslado y el traslado enviado. Roles de traslado (ADMIN/SUPERVISOR/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_TRANSFERS)
	public ResponseEntity<ApiResponse<TransferDetail>> create(@Valid @RequestBody TransferCreateRequest request) {
		TransferDetail created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Traslado creado.", created));
	}

	@PostMapping("/{id}/receive")
	@Operation(summary = "Recibir traslado", description = "Confirma la recepción: las fornituras quedan disponibles en el destino. Roles de traslado (ADMIN/SUPERVISOR/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_TRANSFERS)
	public ResponseEntity<ApiResponse<TransferDetail>> receive(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok("Traslado recibido.", service.receive(id)));
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancelar traslado", description = "Revierte las fornituras a disponibles en el origen. Roles de traslado (ADMIN/SUPERVISOR/ALMACEN/CAPTURISTA).")
	@PreAuthorize(RolePolicy.WRITE_TRANSFERS)
	public ResponseEntity<ApiResponse<TransferDetail>> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok("Traslado cancelado.", service.cancel(id)));
	}
}
