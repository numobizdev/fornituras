package com.numobiz.solutions.fornituras.modules.users.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get user by ID", description = "Returns a user. Admins can access any user; others only their own profile.")
	@PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id)")
	public ResponseEntity<ApiResponse<UserResponseDTO>> getById(@PathVariable Long id) {
		UserResponseDTO user = userService.findById(id);
		return ResponseEntity.ok(ApiResponse.ok(user));
	}

	@GetMapping
	@Operation(summary = "List users", description = "Returns all users. Admin only.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAll() {
		List<UserResponseDTO> users = userService.findAll();
		return ResponseEntity.ok(ApiResponse.ok(users));
	}

	@PostMapping
	@Operation(summary = "Create user", description = "Creates a user and sends an activation code by email. Admin only.")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponseDTO>> create(@Valid @RequestBody UserRequestDTO request) {
		UserResponseDTO user = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Usuario creado. Se envió un código de activación al correo.", user));
	}
}
