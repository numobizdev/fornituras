package mx.uumbal.solutions.palm_flow.modules.auth.repository;

import mx.uumbal.solutions.palm_flow.modules.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    void deleteByUserId(Long userId);
}
