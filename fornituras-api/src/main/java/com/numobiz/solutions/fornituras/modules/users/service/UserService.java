package com.numobiz.solutions.fornituras.modules.users.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapper;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	public UserResponseDTO findById(Long id) {
		log.debug("Finding user by id: {}", id);
		User user = userRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("User not found with id: {}", id);
					return new NotFoundException("User not found with id: " + id);
				});
		return userMapper.toResponse(user);
	}

	public List<UserResponseDTO> findAll() {
		log.debug("Finding all users");
		return userMapper.toResponseList(userRepository.findAll());
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

	@Transactional
	public UserResponseDTO create(UserRequestDTO request) {
		log.info("Creating user with email: {}", request.email());
		if (userRepository.existsByEmail(request.email())) {
			log.warn("Email already registered: {}", request.email());
			throw new BadRequestException("Email already registered: " + request.email());
		}

		User user = userMapper.toEntity(request);
		user.setPassword(passwordEncoder.encode(request.password()));
		if (request.role() != null) {
			user.setRole(request.role());
		} else {
			user.setRole(Role.CAPTURISTA);
		}

		User savedUser = userRepository.save(user);
		log.info("User created with id: {}", savedUser.getId());
		return userMapper.toResponse(savedUser);
	}
}
