package com.numobiz.solutions.fornituras.modules.incidents.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.incidents.dto.AlertItem;
import com.numobiz.solutions.fornituras.modules.incidents.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alertas derivadas de la operación. Hoy expone las de vigencia (próximas a vencer / caducadas),
 * calculadas al vuelo desde {@code equipment.fecha_vencimiento}. Consulta para cualquier rol
 * autenticado; el color semántico lo aplica el frontend según {@code expiryStatus}.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alertas derivadas (vigencia y mantenimiento)")
@SecurityRequirement(name = "Bearer Authentication")
public class AlertController {

	private final AlertService service;

	public AlertController(AlertService service) {
		this.service = service;
	}

	@GetMapping("/vigencia")
	@Operation(summary = "Alertas de vigencia", description = "Fornituras próximas a vencer (≤ 90 días) o caducadas. Cualquier rol autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<AlertItem>>> vigencia() {
		return ResponseEntity.ok(ApiResponse.ok(service.vigenciaAlerts()));
	}
}
