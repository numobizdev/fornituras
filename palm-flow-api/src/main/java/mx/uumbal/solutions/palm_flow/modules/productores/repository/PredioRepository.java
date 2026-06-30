package mx.uumbal.solutions.palm_flow.modules.productores.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredioRepository extends JpaRepository<Predio, UUID> {

    Page<Predio> findByProductorUuid(UUID productorUuid, Pageable pageable);

    Page<Predio> findByCentroAcopioUuidIn(Collection<UUID> centroAcopioUuids, Pageable pageable);

    Page<Predio> findByProductorUuidAndCentroAcopioUuidIn(
            UUID productorUuid, Collection<UUID> centroAcopioUuids, Pageable pageable);

    boolean existsByProductorUuidAndCentroAcopioUuidIn(UUID productorUuid, Collection<UUID> centroAcopioUuids);

    boolean existsByProductorUuidAndCentroAcopioUuidNotIn(UUID productorUuid, Collection<UUID> centroAcopioUuids);

    Optional<Predio> findByFiscalIdIgnoreCase(String fiscalId);

    List<Predio> findByFiscalIdIsNotNull();

    boolean existsByFiscalIdIgnoreCaseAndUuidNot(String fiscalId, UUID uuid);
}
