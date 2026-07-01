package com.numobiz.solutions.fornituras.modules.landing.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.common.exception.TooManyRequestsException;
import com.numobiz.solutions.fornituras.common.ratelimit.RateLimiter;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionAdmin;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionCreateRequest;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionPublic;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionUpdateRequest;
import com.numobiz.solutions.fornituras.modules.landing.dto.ReorderRequest;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.service.LandingService;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * API del contenido configurable de la landing (feature 016). La cara pública es anónima y limitada por
 * tasa; el home requiere sesión; la administración (alta/edición/baja/reorden) es solo ADMIN
 * ({@link RolePolicy#MANAGE_LANDING}). Las respuestas de lectura nunca incluyen PII (ADR 0015).
 */
@RestController
@RequestMapping("/api/v1/landing")
@Tag(name = "Landing", description = "Contenido configurable de bienvenida (pública y de inicio)")
public class LandingController {

	private final LandingService service;
	private final RateLimiter rateLimiter;

	public LandingController(LandingService service, RateLimiter rateLimiter) {
		this.service = service;
		this.rateLimiter = rateLimiter;
	}

	@GetMapping("/public")
	@Operation(summary = "Landing pública", description = "Secciones públicas activas, sin autenticación y limitadas por tasa. Nunca incluye PII.")
	public ResponseEntity<ApiResponse<List<LandingSectionPublic>>> getPublic(HttpServletRequest request) {
		// Única superficie sin auth del feature: se limita por IP para frenar abuso/enumeración (ADR 0015/0010).
		if (!rateLimiter.tryConsume("landing:public:" + clientIp(request))) {
			throw new TooManyRequestsException("Demasiadas solicitudes; intente de nuevo en un momento.");
		}
		return ResponseEntity.ok(ApiResponse.ok(service.getPublic()));
	}

	@GetMapping("/home")
	@Operation(summary = "Home de inicio", description = "Secciones de inicio activas para el usuario autenticado.")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<LandingSectionPublic>>> getHome() {
		return ResponseEntity.ok(ApiResponse.ok(service.getHome()));
	}

	@GetMapping("/sections")
	@Operation(summary = "Listar secciones (editor)", description = "Todas las secciones de una cara, incluidas inactivas. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_LANDING)
	public ResponseEntity<ApiResponse<List<LandingSectionAdmin>>> list(@RequestParam LandingScope scope) {
		return ResponseEntity.ok(ApiResponse.ok(service.list(scope)));
	}

	@PostMapping("/sections")
	@Operation(summary = "Crear sección", description = "Alta de una sección de contenido. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_LANDING)
	public ResponseEntity<ApiResponse<LandingSectionAdmin>> create(
			@Valid @RequestBody LandingSectionCreateRequest request) {
		LandingSectionAdmin created = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Sección creada.", created));
	}

	@PutMapping("/sections/{id}")
	@Operation(summary = "Editar sección", description = "Edita una sección existente. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_LANDING)
	public ResponseEntity<ApiResponse<LandingSectionAdmin>> update(
			@PathVariable Long id,
			@Valid @RequestBody LandingSectionUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Sección actualizada.", service.update(id, request)));
	}

	@PatchMapping("/sections/{id}/deactivate")
	@Operation(summary = "Desactivar sección", description = "Baja lógica (idempotente). Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_LANDING)
	public ResponseEntity<ApiResponse<LandingSectionAdmin>> deactivate(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok("Sección desactivada.", service.deactivate(id)));
	}

	@PatchMapping("/sections/{id}/activate")
	@Operation(summary = "Activar sección", description = "Vuelve a mostrar una sección dada de baja (idempotente). Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_LANDING)
	public ResponseEntity<ApiResponse<LandingSectionAdmin>> activate(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok("Sección activada.", service.activate(id)));
	}

	@PatchMapping("/sections/reorder")
	@Operation(summary = "Reordenar secciones", description = "Actualiza el orden por lote. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_LANDING)
	public ResponseEntity<ApiResponse<List<LandingSectionAdmin>>> reorder(
			@Valid @RequestBody ReorderRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Orden actualizado.", service.reorder(request)));
	}

	private static String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
