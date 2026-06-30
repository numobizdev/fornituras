package mx.uumbal.solutions.palm_flow.modules.auth.repository;

import mx.uumbal.solutions.palm_flow.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByCode(String code);

    void deleteByUserId(Long userId);
}
