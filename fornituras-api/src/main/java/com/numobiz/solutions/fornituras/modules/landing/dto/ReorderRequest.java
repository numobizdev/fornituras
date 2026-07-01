package com.numobiz.solutions.fornituras.modules.landing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Reordenamiento por lote de secciones: cada elemento fija el nuevo {@code orden} de una sección por id.
 */
public record ReorderRequest(
		@NotEmpty(message = "Debe indicar al menos un elemento a reordenar")
		List<@Valid Item> items
) {

	public record Item(
			@NotNull(message = "El id es obligatorio")
			Long id,

			@NotNull(message = "El orden es obligatorio")
			Integer orden
	) {
	}
}
