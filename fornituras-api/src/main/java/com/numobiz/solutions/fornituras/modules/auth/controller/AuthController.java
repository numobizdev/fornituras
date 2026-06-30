package com.numobiz.solutions.fornituras.modules.auth.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.auth.dto.AuthResponseDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.LoginRequestDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.RegisterRequestDTO;
import com.numobiz.solutions.fornituras.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
		AuthResponseDTO response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("User registered successfully", response));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
		AuthResponseDTO response = authService.login(request);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
	}
}
