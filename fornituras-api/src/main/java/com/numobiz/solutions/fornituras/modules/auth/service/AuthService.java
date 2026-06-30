package com.numobiz.solutions.fornituras.modules.auth.service;

import com.numobiz.solutions.fornituras.modules.auth.dto.AuthResponseDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.LoginRequestDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.RegisterRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.dto.UserResponseDTO;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.service.UserService;
import com.numobiz.solutions.fornituras.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserService userService;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;

	public AuthService(
			UserService userService,
			JwtService jwtService,
			AuthenticationManager authenticationManager,
			UserDetailsService userDetailsService) {
		this.userService = userService;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
	}

	public AuthResponseDTO register(RegisterRequestDTO request) {
		log.info("Registering user with email: {}", request.email());
		UserRequestDTO userRequest = new UserRequestDTO(
				request.name(),
				request.email(),
				request.password(),
				Role.CAPTURISTA);
		UserResponseDTO user = userService.create(userRequest);
		return buildAuthResponse(user);
	}

	public AuthResponseDTO login(LoginRequestDTO request) {
		log.info("Login attempt for email: {}", request.email());
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.password()));

		UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
		String token = jwtService.generateToken(userDetails);
		UserResponseDTO user = userService.findByEmail(request.email());

		return new AuthResponseDTO(
				token,
				"Bearer",
				jwtService.getExpirationMs(),
				new AuthResponseDTO.UserSummaryDTO(user.id(), user.name(), user.email(), user.role()));
	}

	private AuthResponseDTO buildAuthResponse(UserResponseDTO user) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(user.email());
		String token = jwtService.generateToken(userDetails);
		return new AuthResponseDTO(
				token,
				"Bearer",
				jwtService.getExpirationMs(),
				new AuthResponseDTO.UserSummaryDTO(user.id(), user.name(), user.email(), user.role()));
	}
}
