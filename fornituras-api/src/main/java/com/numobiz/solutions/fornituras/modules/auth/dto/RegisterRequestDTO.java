package com.numobiz.solutions.fornituras.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must not exceed 100 characters")
		String name,

		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 255, message = "Email must not exceed 255 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
		String password
) {
}
