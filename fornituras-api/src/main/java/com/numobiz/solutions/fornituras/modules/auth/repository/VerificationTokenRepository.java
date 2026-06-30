package com.numobiz.solutions.fornituras.modules.auth.repository;

import com.numobiz.solutions.fornituras.modules.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

	Optional<VerificationToken> findByCode(String code);

	void deleteByUserId(Long userId);
}
