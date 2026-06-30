package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFruta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecepcionFrutaRepository extends JpaRepository<RecepcionFruta, UUID> {

    Optional<RecepcionFruta> findTopByFolioStartingWithOrderByFolioDesc(String folioPrefix);

    Page<RecepcionFruta> findByCentroAcopioUuid(UUID centroAcopioUuid, Pageable pageable);

    Page<RecepcionFruta> findByProductorUuid(UUID productorUuid, Pageable pageable);

    Page<RecepcionFruta> findByCentroAcopioUuidIn(Collection<UUID> centroAcopioUuids, Pageable pageable);

    Page<RecepcionFruta> findByCentroAcopioUuidInAndProductorUuid(
            Collection<UUID> centroAcopioUuids, UUID productorUuid, Pageable pageable);
}
