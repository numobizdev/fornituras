package com.numobiz.solutions.fornituras.modules.catalog.dto;

/**
 * Valor de catálogo para selectores y administración. Incluye el catálogo al que pertenece y el
 * padre (jerarquía) para que el frontend pueda filtrar (p. ej. tallas por tipo).
 */
public record CatalogItemSummary(
		Long id,
		Long catalogId,
		String catalogCode,
		String code,
		String nombre,
		String descripcion,
		String fotoUrl,
		Long parentItemId,
		Integer orden,
		boolean active
) {
}
