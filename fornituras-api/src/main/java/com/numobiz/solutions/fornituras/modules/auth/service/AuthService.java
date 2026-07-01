package com.numobiz.solutions.fornituras.modules.auth.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.UnauthorizedException;
import com.numobiz.solutions.fornituras.modules.auth.dto.ActivateAccountRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.AuthResponseDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.ChangePasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.ForgotPasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.LoginRequestDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.ResetPasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.entity.PasswordResetToken;
import com.numobiz.solutions.fornituras.modules.auth.entity.VerificationToken;
import com.numobiz.solutions.fornituras.modules.auth.repository.PasswordResetTokenRepository;
import com.numobiz.solutions.fornituras.modules.auth.repository.VerificationTokenRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final int ACTIVATION_CODE_HOURS = 24;
	private static final int RESET_CODE_HOURS = 1;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final VerificationTokenRepository verificationTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailService emailService;
	private final AuditWriter audit;
	private final LoginAttemptService loginAttempt;

	public AuthService(
			UserRepository userRepository,
			VerificationTokenRepository verificationTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			EmailService emailService,
			AuditWriter audit,
			LoginAttemptService loginAttempt) {
		this.userRepository = userRepository;
		this.verificationTokenRepository = verificationTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.emailService = emailService;
		this.audit = audit;
		this.loginAttempt = loginAttempt;
	}

	@Transactional(readOnly = true)
	public AuthResponseDTO login(LoginRequestDTO request) {
		String email = request.email().trim();
		log.info("Login attempt for email: {}", email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

		// Anti-fuerza-bruta (FR-005): rechaza si la cuenta está bloqueada por intentos fallidos.
		loginAttempt.assertNotLocked(user);

		if (!user.isEnabled()) {
			throw new UnauthorizedException(
					"La cuenta no está activada. Revise su correo para el código de activación.");
		}
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			log.warn("Login failed: password mismatch for user id {}", user.getId());
			loginAttempt.onFailedAttempt(user.getId());
			throw new UnauthorizedException("Invalid email or password");
		}
		loginAttempt.onSuccessfulLogin(user.getId());

		UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPassword())
				.authorities("ROLE_" + user.getRole().name())
				.build();

		String token = jwtService.generateToken(userDetails);
		log.info("User logged in: {}", user.getEmail());
		audit.record("LOGIN", user.getId());
		return buildAuthResponse(token, user);
	}

	@Transactional
	public void sendActivationCode(User user) {
		verificationTokenRepository.deleteByUserId(user.getId());
		verificationTokenRepository.flush();

		VerificationToken verificationToken = new VerificationToken();
		verificationToken.setCode(generateUniqueVerificationCode());
		verificationToken.setUser(user);
		verificationToken.setCreatedAt(LocalDateTime.now());
		verificationToken.setExpiresAt(LocalDateTime.now().plusHours(ACTIVATION_CODE_HOURS));
		verificationTokenRepository.save(verificationToken);

		emailService.sendHtmlEmail(
				user.getEmail(),
				"Activación de cuenta - Fornituras",
				"email/activate-account",
				Map.of("code", verificationToken.getCode(), "email", user.getEmail(), "name", user.getName()));
		log.info("Activation email sent to {}", user.getEmail());
	}

	@Transactional
	public void activateAccount(ActivateAccountRequest request) {
		VerificationToken verificationToken = verificationTokenRepository.findByCode(request.code())
				.orElseThrow(() -> new BadRequestException("Código de activación inválido o expirado"));

		if (verificationToken.isExpired()) {
			verificationTokenRepository.delete(verificationToken);
			throw new BadRequestException("El código de activación ha expirado");
		}

		User user = verificationToken.getUser();
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		user.setEnabled(true);
		userRepository.save(user);
		verificationTokenRepository.delete(verificationToken);
		log.info("Account activated: {}", user.getEmail());
	}

	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UnauthorizedException("User not found"));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BadRequestException("Current password is incorrect");
		}

		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
		log.info("Password changed for user: {}", email);
	}

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		userRepository.findByEmail(request.email().trim()).ifPresent(user -> {
			if (!user.isEnabled()) {
				return;
			}
			passwordResetTokenRepository.deleteByUserId(user.getId());
			passwordResetTokenRepository.flush();

			PasswordResetToken resetToken = new PasswordResetToken();
			resetToken.setCode(generateUniqueResetCode());
			resetToken.setUser(user);
			resetToken.setCreatedAt(LocalDateTime.now());
			resetToken.setExpiresAt(LocalDateTime.now().plusHours(RESET_CODE_HOURS));
			passwordResetTokenRepository.save(resetToken);

			emailService.sendHtmlEmail(
					user.getEmail(),
					"Recuperación de contraseña - Fornituras",
					"email/reset-password",
					Map.of("code", resetToken.getCode(), "email", user.getEmail()));
			log.info("Reset password email sent to {}", user.getEmail());
		});
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		PasswordResetToken resetToken = passwordResetTokenRepository.findByCode(request.code())
				.orElseThrow(() -> new BadRequestException("Código de verificación inválido o expirado"));

		if (resetToken.isExpired()) {
			passwordResetTokenRepository.delete(resetToken);
			throw new BadRequestException("El código de verificación ha expirado");
		}

		User user = resetToken.getUser();
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
		passwordResetTokenRepository.delete(resetToken);
		log.info("Password reset for user: {}", user.getEmail());
	}

	private AuthResponseDTO buildAuthResponse(String token, User user) {
		return new AuthResponseDTO(
				token,
				"Bearer",
				jwtService.getExpirationMs(),
				new AuthResponseDTO.UserSummaryDTO(user.getId(), user.getName(), user.getEmail(), user.getRole()));
	}

	private String generateUniqueVerificationCode() {
		return generateUniqueCode(code -> verificationTokenRepository.findByCode(code).isPresent());
	}

	private String generateUniqueResetCode() {
		return generateUniqueCode(code -> passwordResetTokenRepository.findByCode(code).isPresent());
	}

	private String generateUniqueCode(java.util.function.Predicate<String> exists) {
		for (int attempt = 0; attempt < 100; attempt++) {
			String code = generateSixDigitCode();
			if (!exists.test(code)) {
				return code;
			}
		}
		throw new BadRequestException("Unable to generate verification code. Please try again.");
	}

	private String generateSixDigitCode() {
		int code = 100_000 + RANDOM.nextInt(900_000);
		return String.valueOf(code);
	}
}
