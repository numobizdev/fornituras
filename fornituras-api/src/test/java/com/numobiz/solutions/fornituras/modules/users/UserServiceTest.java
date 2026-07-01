package com.numobiz.solutions.fornituras.modules.users;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.auth.service.AuthService;
import com.numobiz.solutions.fornituras.modules.users.dto.UserRequestDTO;
import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.mapper.UserMapper;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.modules.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio del servicio de usuarios (T025): unicidad de email en el alta y regla de "último
 * administrador activo" al desactivar o degradar.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserMapper userMapper;
	@Mock
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
	@Mock
	private AuthService authService;
	@Mock
	private AuditWriter audit;

	@InjectMocks
	private UserService service;

	@Test
	void create_rejectsDuplicateEmail() {
		when(userRepository.existsByEmail("dup@fornituras.local")).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> service.create(new UserRequestDTO("Dup", "dup@fornituras.local", Role.CAPTURISTA)));
		verify(userRepository, never()).save(any());
	}

	@Test
	void disable_lastActiveAdmin_isBlocked() {
		User admin = activeAdmin(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

		assertThrows(ConflictException.class, () -> service.setEnabled(1L, false));
		verify(userRepository, never()).save(any());
	}

	@Test
	void demote_lastActiveAdmin_isBlocked() {
		User admin = activeAdmin(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

		assertThrows(ConflictException.class, () -> service.changeRole(1L, Role.CAPTURISTA));
		verify(userRepository, never()).save(any());
	}

	@Test
	void disable_admin_allowedWhenAnotherAdminRemains() {
		User admin = activeAdmin(1L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(2L);
		lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

		service.setEnabled(1L, false);

		verify(userRepository).save(admin);
		verify(audit).record("DISABLE_USER", 1L);
	}

	private User activeAdmin(Long id) {
		User user = new User();
		user.setId(id);
		user.setName("Admin");
		user.setEmail("admin@fornituras.local");
		user.setRole(Role.ADMIN);
		user.setEnabled(true);
		return user;
	}
}
