package com.numobiz.solutions.fornituras.modules.auth.repository;

import com.numobiz.solutions.fornituras.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByCode(String code);

	void deleteByUserId(Long userId);
}
