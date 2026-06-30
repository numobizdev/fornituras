package com.numobiz.solutions.fornituras.modules.officers.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.officers.dto.CatalogItem;
import com.numobiz.solutions.fornituras.modules.officers.repository.SexoRepository;
import com.numobiz.solutions.fornituras.modules.officers.repository.TipoSangreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogos de apoyo del padrón (sexo, tipo de sangre) para poblar los selectores del alta. Datos
 * de referencia no sensibles; lectura para cualquier rol autenticado.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Officer catalogs", description = "Catálogos del padrón (sexo, tipo de sangre)")
@SecurityRequirement(name = "Bearer Authentication")
public class OfficerCatalogController {

	private final SexoRepository sexoRepository;
	private final TipoSangreRepository tipoSangreRepository;

	public OfficerCatalogController(SexoRepository sexoRepository, TipoSangreRepository tipoSangreRepository) {
		this.sexoRepository = sexoRepository;
		this.tipoSangreRepository = tipoSangreRepository;
	}

	@GetMapping("/sexos")
	@Operation(summary = "Listar sexos")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<CatalogItem>>> sexos() {
		List<CatalogItem> items = sexoRepository.findByActiveTrue().stream()
				.map(s -> new CatalogItem(s.getId(), s.getNombre()))
				.toList();
		return ResponseEntity.ok(ApiResponse.ok(items));
	}

	@GetMapping("/tipos-sangre")
	@Operation(summary = "Listar tipos de sangre")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<CatalogItem>>> tiposSangre() {
		List<CatalogItem> items = tipoSangreRepository.findByActiveTrue().stream()
				.map(t -> new CatalogItem(t.getId(), t.getEtiqueta()))
				.toList();
		return ResponseEntity.ok(ApiResponse.ok(items));
	}
}
