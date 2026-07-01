package com.numobiz.solutions.fornituras.modules.audit.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.audit.dto.AuditLogSummary;
import com.numobiz.solutions.fornituras.modules.audit.service.AuditQueryService;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de la bitácora de auditoría (012). Solo lectura, <b>restringida a auditoría</b>
 * ({@link RolePolicy#READ_AUDIT}: ADMIN y AUDITOR, ADR 0013 regla 4): un rol operativo que intente
 * consultar queda denegado (y ese intento se audita, FR-006). No existe endpoint de escritura: la
 * bitácora se alimenta internamente vía el puerto {@code AuditWriter}.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Bitácora de auditoría (ISO 27001; solo administración)")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditLogController {

	private final AuditQueryService service;

	public AuditLogController(AuditQueryService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Consultar bitácora", description = "Eventos de auditoría paginados y filtrables (actor, acción, entidad, rango de fechas). Solo ADMIN/AUDITOR.")
	@PreAuthorize(RolePolicy.READ_AUDIT)
	public ResponseEntity<ApiResponse<Page<AuditLogSummary>>> query(
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String accion,
			@RequestParam(required = false) String entidad,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
			@PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(service.query(actor, accion, entidad, desde, hasta, pageable)));
	}
}
