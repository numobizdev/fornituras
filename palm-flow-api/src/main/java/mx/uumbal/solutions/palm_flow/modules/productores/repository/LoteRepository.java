package mx.uumbal.solutions.palm_flow.modules.productores.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Lote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteRepository extends JpaRepository<Lote, UUID> {

    Page<Lote> findByPredioUuid(UUID predioUuid, Pageable pageable);

    List<Lote> findByPredioUuidIn(List<UUID> predioUuids);

    Optional<Lote> findByIdGisIgnoreCase(String idGis);

    @Query("SELECT DISTINCT LOWER(l.idGis) FROM Lote l WHERE l.idGis IS NOT NULL")
    Set<String> findDistinctIdGisLowerCase();
}
