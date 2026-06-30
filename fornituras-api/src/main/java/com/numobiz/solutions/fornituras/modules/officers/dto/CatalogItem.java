package com.numobiz.solutions.fornituras.modules.officers.dto;

/** Ítem de catálogo simple (sexo, tipo de sangre) para poblar selectores en el frontend. */
public record CatalogItem(
		Long id,
		String etiqueta
) {
}
