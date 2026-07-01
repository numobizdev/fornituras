package com.numobiz.solutions.fornituras.modules.users.repository;

import com.numobiz.solutions.fornituras.modules.users.entity.Role;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	/** Administradores activos: sirve para impedir dejar el sistema sin admin (FR-007). */
	long countByRoleAndEnabledTrue(Role role);
}
