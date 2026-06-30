package com.numobiz.solutions.fornituras.modules.users.dto;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;

import java.time.LocalDateTime;

public record UserResponseDTO(
		Long id,
		String name,
		String email,
		Role role,
		boolean enabled,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
