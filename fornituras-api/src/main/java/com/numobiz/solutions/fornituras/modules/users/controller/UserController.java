package com.numobiz.solutions.fornituras.modules.users.controller;

import com.numobiz.solutions.fornituras.common.dto.ApiResponse;
import com.numobiz.solutions.fornituras.modules.users.dto.EnabledUpdateRequest;
import com.numobiz.solutions.fornituras.modules.users.dto.RoleUpdateRequest;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserUpdateRequest;
import com.numobiz.solutions.fornituras.modules.users.service.UserService;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de administración de usuarios (feature 013). El listado y toda escritura (alta, edición, cambio de
 * rol, activar/desactivar) están restringidos a ADMIN (rechazo por defecto, mínimo privilegio); cada
 * usuario puede consultar únicamente su propia ficha. Nunca se expone la contraseña (hash) ni el token.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Administración de usuarios del sistema y sus roles")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get user by ID", description = "Returns a user. Admins can access any user; others only their own profile.")
	@PreAuthorize(RolePolicy.MANAGE_USERS + " or @userService.isCurrentUser(#id)")
	public ResponseEntity<ApiResponse<UserResponseDTO>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
	}

	@GetMapping
	@Operation(summary = "List users", description = "Listado paginado de usuarios. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_USERS)
	public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> getAll(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.ok(userService.findAll(pageable)));
	}

	@PostMapping
	@Operation(summary = "Create user", description = "Creates a user and sends an activation code by email. Admin only.")
	@PreAuthorize(RolePolicy.MANAGE_USERS)
	public ResponseEntity<ApiResponse<UserResponseDTO>> create(@Valid @RequestBody UserRequestDTO request) {
		UserResponseDTO user = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Usuario creado. Se envió un código de activación al correo.", user));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Edit user", description = "Edita los datos básicos del usuario (el email es inmutable). Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_USERS)
	public ResponseEntity<ApiResponse<UserResponseDTO>> update(
			@PathVariable Long id,
			@Valid @RequestBody UserUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Usuario actualizado.", userService.update(id, request)));
	}

	@PatchMapping("/{id}/enabled")
	@Operation(summary = "Enable/disable user", description = "Activa o desactiva el usuario; no permite dejar el sistema sin admin. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_USERS)
	public ResponseEntity<ApiResponse<UserResponseDTO>> setEnabled(
			@PathVariable Long id,
			@Valid @RequestBody EnabledUpdateRequest request) {
		UserResponseDTO user = userService.setEnabled(id, request.enabled());
		return ResponseEntity.ok(ApiResponse.ok(
				request.enabled() ? "Usuario activado." : "Usuario desactivado.", user));
	}

	@PatchMapping("/{id}/role")
	@Operation(summary = "Change user role", description = "Asigna el rol del usuario; no permite degradar al último admin. Solo ADMIN.")
	@PreAuthorize(RolePolicy.MANAGE_USERS)
	public ResponseEntity<ApiResponse<UserResponseDTO>> changeRole(
			@PathVariable Long id,
			@Valid @RequestBody RoleUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("Rol actualizado.", userService.changeRole(id, request.role())));
	}
}
