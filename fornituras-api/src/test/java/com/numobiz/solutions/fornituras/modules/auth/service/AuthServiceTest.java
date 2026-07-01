package com.numobiz.solutions.fornituras.modules.auth.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.UnauthorizedException;
import com.numobiz.solutions.fornituras.modules.auth.dto.ActivateAccountRequest;
import com.numobiz.solutions.fornituras.modules.auth.dto.LoginRequestDTO;
import com.numobiz.solutions.fornituras.modules.auth.dto.ResetPasswordRequest;
import com.numobiz.solutions.fornituras.modules.auth.entity.PasswordResetToken;
import com.numobiz.solutions.fornituras.modules.auth.entity.VerificationToken;
import com.numobiz.solutions.fornituras.modules.auth.repository.PasswordResetTokenRepository;
import com.numobiz.solutions.fornituras.modules.auth.repository.VerificationTokenRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private VerificationTokenRepository verificationTokenRepository;
	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtService jwtService;
	@Mock
	private EmailService emailService;
	@Mock
	private com.numobiz.solutions.fornituras.common.audit.AuditWriter audit;
	@Mock
	private LoginAttemptService loginAttempt;

	@InjectMocks
	private AuthService authService;

	private User pendingUser;

	@BeforeEach
	void setUp() {
		pendingUser = new User();
		pendingUser.setId(1L);
		pendingUser.setName("Capturista");
		pendingUser.setEmail("user@example.com");
		pendingUser.setRole(Role.CAPTURISTA);
		pendingUser.setEnabled(false);
		pendingUser.setPassword("placeholder");
	}

	@Test
	void login_shouldRejectDisabledAccount() {
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(pendingUser));

		assertThrows(UnauthorizedException.class,
				() -> authService.login(new LoginRequestDTO("user@example.com", "secret")));
	}

	@Test
	void activateAccount_shouldEnableUserAndSetPassword() {
		VerificationToken token = new VerificationToken();
		token.setCode("123456");
		token.setUser(pendingUser);
		token.setExpiresAt(LocalDateTime.now().plusHours(1));

		when(verificationTokenRepository.findByCode("123456")).thenReturn(Optional.of(token));
		when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded");

		authService.activateAccount(new ActivateAccountRequest("123456", "NewPass1!"));

		assertTrue(pendingUser.isEnabled());
		assertEquals("encoded", pendingUser.getPassword());
		verify(verificationTokenRepository).delete(token);
	}

	@Test
	void sendActivationCode_shouldEmailSixDigitCode() {
		when(verificationTokenRepository.findByCode(anyString())).thenReturn(Optional.empty());

		authService.sendActivationCode(pendingUser);

		ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
		verify(emailService).sendHtmlEmail(
				eq("user@example.com"),
				eq("Activación de cuenta - Fornituras"),
				eq("email/activate-account"),
				variablesCaptor.capture());
		String code = variablesCaptor.getValue().get("code").toString();
		assertEquals(6, code.length());
		verify(verificationTokenRepository).save(any(VerificationToken.class));
	}

	@Test
	void resetPassword_shouldRejectExpiredCode() {
		PasswordResetToken token = new PasswordResetToken();
		token.setCode("654321");
		token.setUser(pendingUser);
		token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

		when(passwordResetTokenRepository.findByCode("654321")).thenReturn(Optional.of(token));

		assertThrows(BadRequestException.class,
				() -> authService.resetPassword(new ResetPasswordRequest("654321", "NewPass1!")));
	}
}
