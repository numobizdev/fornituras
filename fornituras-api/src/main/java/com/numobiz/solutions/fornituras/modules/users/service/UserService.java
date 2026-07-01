package com.numobiz.solutions.fornituras.modules.users.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.auth.service.AuthService;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserUpdateRequest;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapper;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;
	private final AuditWriter audit;

	public UserService(
			UserRepository userRepository,
			UserMapper userMapper,
			PasswordEncoder passwordEncoder,
			AuthService authService,
			AuditWriter audit) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
		this.authService = authService;
		this.audit = audit;
	}

	public UserResponseDTO findById(Long id) {
		log.debug("Finding user by id: {}", id);
		return userMapper.toResponse(getOrThrow(id));
	}

	public Page<UserResponseDTO> findAll(Pageable pageable) {
		log.debug("Listing users (paged)");
		return userRepository.findAll(pageable).map(userMapper::toResponse);
	}

	public UserResponseDTO findByEmail(String email) {
		log.debug("Finding user by email: {}", email);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("User not found with email: {}", email);
					return new NotFoundException("User not found with email: " + email);
				});
		return userMapper.toResponse(user);
	}

	public boolean isCurrentUser(Long id) {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}
		return userRepository.findByEmail(authentication.getName())
				.map(user -> user.getId().equals(id))
				.orElse(false);
	}

	@Transactional
	public UserResponseDTO create(UserRequestDTO request) {
		log.info("Creating user with email: {}", request.email());
		if (userRepository.existsByEmail(request.email().trim())) {
			log.warn("Email already registered: {}", request.email());
			throw new ConflictException("Email already registered: " + request.email());
		}

		User user = userMapper.toEntity(request);
		user.setEmail(request.email().trim());
		user.setRole(request.role() != null ? request.role() : Role.CAPTURISTA);
		user.setEnabled(false);
		// Contraseña provisional aleatoria: el usuario define la suya al activar la cuenta con el código.
		user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID()));

		User savedUser = userRepository.save(user);
		authService.sendActivationCode(savedUser);
		audit.record("CREATE_USER", savedUser.getId());
		log.info("Pending user created with id: {}", savedUser.getId());
		return userMapper.toResponse(savedUser);
	}

	@Transactional
	public UserResponseDTO update(Long id, UserUpdateRequest request) {
		User user = getOrThrow(id);
		user.setName(request.name().trim());
		User saved = userRepository.save(user);
		audit.record("UPDATE_USER", saved.getId());
		return userMapper.toResponse(saved);
	}

	/**
	 * Activa/desactiva un usuario. Desactivar al <b>último administrador activo</b> se bloquea para no
	 * dejar el sistema sin control de acceso (FR-007).
	 */
	@Transactional
	public UserResponseDTO setEnabled(Long id, boolean enabled) {
		User user = getOrThrow(id);
		if (wouldLeaveSystemWithoutAdmin(user, user.getRole() == Role.ADMIN, enabled)) {
			throw new ConflictException("No se puede desactivar al último administrador activo del sistema.");
		}
		user.setEnabled(enabled);
		User saved = userRepository.save(user);
		audit.record(enabled ? "ENABLE_USER" : "DISABLE_USER", saved.getId());
		return userMapper.toResponse(saved);
	}

	/**
	 * Cambia el rol de un usuario. Degradar al <b>último administrador activo</b> a un rol no admin se
	 * bloquea (FR-007).
	 */
	@Transactional
	public UserResponseDTO changeRole(Long id, Role newRole) {
		User user = getOrThrow(id);
		if (wouldLeaveSystemWithoutAdmin(user, newRole == Role.ADMIN, user.isEnabled())) {
			throw new ConflictException("No se puede quitar el rol de administrador al último admin activo.");
		}
		user.setRole(newRole);
		User saved = userRepository.save(user);
		audit.record("ROLE_CHANGE_USER", saved.getId());
		return userMapper.toResponse(saved);
	}

	/**
	 * Determina si el estado resultante ({@code willBeAdmin}/{@code willBeEnabled}) dejaría al sistema sin
	 * ningún administrador activo: solo ocurre si el usuario es hoy admin activo, deja de serlo con el
	 * cambio y era el único administrador activo que quedaba.
	 */
	private boolean wouldLeaveSystemWithoutAdmin(User user, boolean willBeAdmin, boolean willBeEnabled) {
		boolean isCurrentlyActiveAdmin = user.getRole() == Role.ADMIN && user.isEnabled();
		if (!isCurrentlyActiveAdmin) {
			return false;
		}
		boolean staysActiveAdmin = willBeAdmin && willBeEnabled;
		return !staysActiveAdmin && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1;
	}

	private User getOrThrow(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("User not found with id: {}", id);
					return new NotFoundException("User not found with id: " + id);
				});
	}
}
