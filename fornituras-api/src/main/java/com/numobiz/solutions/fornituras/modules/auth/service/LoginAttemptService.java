package com.numobiz.solutions.fornituras.modules.auth.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.TooManyRequestsException;
import com.numobiz.solutions.fornituras.config.LoginLockProperties;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protección anti-fuerza-bruta por cuenta (FR-005): cuenta intentos fallidos consecutivos y bloquea
 * temporalmente la cuenta al superar el umbral. El contador y el bloqueo se <b>persisten</b> en
 * {@code users} para sobrevivir a reinicios. Las escrituras van en transacción propia
 * ({@code REQUIRES_NEW}) porque el login corre en una transacción de solo lectura y, además, la
 * penalización por fallo debe registrarse aunque el flujo de autenticación termine lanzando error.
 */
@Service
public class LoginAttemptService {

	private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

	private final UserRepository userRepository;
	private final AuditWriter audit;
	private final int maxAttempts;
	private final int lockMinutes;

	public LoginAttemptService(
			UserRepository userRepository, AuditWriter audit, LoginLockProperties properties) {
		this.userRepository = userRepository;
		this.audit = audit;
		this.maxAttempts = properties.maxAttempts();
		this.lockMinutes = properties.lockMinutes();
	}

	/** Rechaza el intento si la cuenta está bloqueada. El mensaje no revela intentos ni existencia. */
	public void assertNotLocked(User user) {
		if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
			throw new TooManyRequestsException(
					"Cuenta temporalmente bloqueada por varios intentos fallidos. Intente más tarde.");
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onFailedAttempt(Long userId) {
		userRepository.findById(userId).ifPresent(user -> {
			int attempts = user.getFailedAttempts() + 1;
			user.setFailedAttempts(attempts);
			if (attempts >= maxAttempts) {
				user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
				audit.record("LOGIN_LOCKED", user.getId());
				log.warn("Account locked after {} failed attempts, user id {}", attempts, user.getId());
			}
			userRepository.save(user);
		});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onSuccessfulLogin(Long userId) {
		userRepository.findById(userId).ifPresent(user -> {
			if (user.getFailedAttempts() != 0 || user.getLockedUntil() != null) {
				user.setFailedAttempts(0);
				user.setLockedUntil(null);
				userRepository.save(user);
			}
		});
	}
}
