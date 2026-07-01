package com.numobiz.solutions.fornituras.modules.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta/edición de un valor de catálogo. El catálogo destino se indica por su {@code code} en la
 * ruta (no en el cuerpo). La unicidad de nombre (dentro del catálogo) se valida en el servicio.
 */
public record CatalogItemCreateRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 120, message = "El nombre no debe exceder 120 caracteres")
		String nombre,

		@Size(max = 40, message = "El código no debe exceder 40 caracteres")
		String code,

		@Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
		String descripcion,

		@Size(max = 500, message = "La URL de foto no debe exceder 500 caracteres")
		// Bloquea esquemas peligrosos (javascript:, data:, vbscript:…) para evitar XSS almacenado al
		// renderar la foto; admite http(s) o cualquier ruta relativa sin esquema.
		@Pattern(
				regexp = "^\\s*$|^https?://[^\\s]*$|^(?![a-zA-Z][a-zA-Z0-9+.-]*:)[^\\s]*$",
				message = "La foto debe ser una URL http(s) o una ruta relativa")
		String fotoUrl,

		/** NULL = valor global; si se indica, cuelga de ese valor padre (jerarquía item→item). */
		Long parentItemId,

		Integer orden
) {
}
