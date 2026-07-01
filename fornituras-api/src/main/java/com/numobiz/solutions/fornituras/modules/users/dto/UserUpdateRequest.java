package com.numobiz.solutions.fornituras.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edición de datos básicos de un usuario. El <b>email es identidad de login</b> y no se edita aquí
 * (se mantiene inmutable); el rol y el estado activo se cambian por sus endpoints dedicados
 * ({@code PATCH /role}, {@code PATCH /enabled}).
 */
public record UserUpdateRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must not exceed 100 characters")
		String name
) {
}
