package com.numobiz.solutions.fornituras.modules.users.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.CAPTURISTA;

	@Column(nullable = false)
	private boolean enabled = true;

	/** Intentos de login fallidos consecutivos; se reinicia al autenticar con éxito (anti-fuerza-bruta, FR-005). */
	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts = 0;

	/** Momento hasta el que la cuenta queda bloqueada por exceso de intentos; {@code null} = sin bloqueo. */
	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;
}
