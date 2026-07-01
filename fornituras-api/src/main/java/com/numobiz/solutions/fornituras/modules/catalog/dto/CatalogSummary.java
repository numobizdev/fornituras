package com.numobiz.solutions.fornituras.modules.catalog.dto;

/** Cabecera de catálogo para poblar la lista de catálogos administrables en el frontend. */
public record CatalogSummary(
		Long id,
		String code,
		String nombre,
		String descripcion,
		boolean system,
		boolean active
) {
}
