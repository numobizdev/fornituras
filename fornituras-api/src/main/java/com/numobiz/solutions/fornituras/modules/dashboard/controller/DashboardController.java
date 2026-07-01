package com.numobiz.solutions.fornituras.modules.dashboard.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.dashboard.dto.DashboardSummary;
import com.numobiz.solutions.fornituras.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tablero de control (feature 010): indicadores agregados del inventario en una sola respuesta.
 * Requiere autenticación (cualquier rol; el RBAC fino se define en 013) y devuelve únicamente
 * contadores numéricos, sin PII ni registros individuales. El color semántico lo aplica el frontend
 * según {@code docs/05-ui-ux.md}.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Tablero de control (indicadores agregados del inventario)")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

	private final DashboardService service;

	public DashboardController(DashboardService service) {
		this.service = service;
	}

	@GetMapping("/summary")
	@Operation(summary = "Resumen de indicadores", description = "Contadores agregados (total, disponibles, asignadas, próximas a vencer, caducadas, en mantenimiento). Cualquier rol autenticado; sin PII.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<DashboardSummary>> summary() {
		return ResponseEntity.ok(ApiResponse.ok(service.summary()));
	}
}
