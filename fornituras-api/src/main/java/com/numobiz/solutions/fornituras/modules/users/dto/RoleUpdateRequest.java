package com.numobiz.solutions.fornituras.modules.users.dto;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio de rol de un usuario. El valor debe pertenecer al enum {@link Role} vigente; un valor fuera
 * del enum falla en la deserialización (400). La regla de "no dejar el sistema sin admin" se aplica en
 * el servicio (FR-007).
 */
public record RoleUpdateRequest(
		@NotNull(message = "Role is required")
		Role role
) {
}
