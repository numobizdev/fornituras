package com.numobiz.solutions.fornituras.modules.auth.dto;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;

public record AuthResponseDTO(
		String token,
		String tokenType,
		long expiresIn,
		UserSummaryDTO user
) {
	public record UserSummaryDTO(
			Long id,
			String name,
			String email,
			Role role
	) {
	}
}
