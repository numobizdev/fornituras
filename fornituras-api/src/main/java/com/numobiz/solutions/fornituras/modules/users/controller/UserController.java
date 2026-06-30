package com.numobiz.solutions.fornituras.modules.users.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<ApiResponse<UserResponseDTO>> getById(@PathVariable Long id) {
		UserResponseDTO user = userService.findById(id);
		return ResponseEntity.ok(ApiResponse.ok(user));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAll() {
		List<UserResponseDTO> users = userService.findAll();
		return ResponseEntity.ok(ApiResponse.ok(users));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<UserResponseDTO>> create(@Valid @RequestBody UserRequestDTO request) {
		UserResponseDTO user = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("User created successfully", user));
	}
}
