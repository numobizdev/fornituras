package com.numobiz.solutions.fornituras.modules.users.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Activación/desactivación de un usuario. Desactivar al último administrador activo se bloquea en el
 * servicio (FR-007).
 */
public record EnabledUpdateRequest(
		@NotNull(message = "Enabled flag is required")
		Boolean enabled
) {
}
