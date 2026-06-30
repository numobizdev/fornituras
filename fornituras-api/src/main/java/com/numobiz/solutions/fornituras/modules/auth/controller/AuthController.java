package com.numobiz.solutions.fornituras.modules.auth.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.auth.dto.ActivateAccountRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.AuthResponseDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.ChangePasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.ForgotPasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.LoginRequestDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.ResetPasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and account management")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate with email and password")
	public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
		AuthResponseDTO response = authService.login(request);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
	}

	@PostMapping("/activate")
	@Operation(summary = "Activate account", description = "Activate account using the 6-digit code from email and set password")
	public ResponseEntity<ApiResponse<Void>> activate(@Valid @RequestBody ActivateAccountRequest request) {
		authService.activateAccount(request);
		return ResponseEntity.ok(ApiResponse.ok("Cuenta activada correctamente. Ya puede iniciar sesión.", null));
	}

	@PostMapping("/change-password")
	@SecurityRequirement(name = "Bearer Authentication")
	@Operation(summary = "Change password", description = "Change password for the authenticated user")
	public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(request);
		return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
	}

	@PostMapping("/forgot-password")
	@Operation(summary = "Forgot password", description = "Request a password reset code by email")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return ResponseEntity.ok(ApiResponse.ok("Si el correo existe, se ha enviado un código de recuperación.", null));
	}

	@PostMapping("/reset-password")
	@Operation(summary = "Reset password", description = "Reset password using the 6-digit code from email")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.ok(ApiResponse.ok("Contraseña restablecida exitosamente. Ahora puede iniciar sesión.", null));
	}
}
