package com.numobiz.solutions.fornituras.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		String email,

		@NotBlank(message = "Password is required")
		String password
) {
}
