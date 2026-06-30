package com.numobiz.solutions.fornituras.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank(message = "Verification code is required")
		@Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
		String code,

		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
		String newPassword
) {
}
