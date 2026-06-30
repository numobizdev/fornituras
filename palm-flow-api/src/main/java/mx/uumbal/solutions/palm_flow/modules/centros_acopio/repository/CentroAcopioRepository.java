package mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository;

import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CentroAcopioRepository extends JpaRepository<CentroAcopio, UUID> {

    Page<CentroAcopio> findByRegionId(Long regionId, Pageable pageable);

    Page<CentroAcopio> findByUuidIn(Collection<UUID> uuids, Pageable pageable);

    Page<CentroAcopio> findByRegionIdAndUuidIn(Long regionId, Collection<UUID> uuids, Pageable pageable);

    Optional<CentroAcopio> findByNombreIgnoreCase(String nombre);

    Optional<CentroAcopio> findByAliasIgnoreCase(String alias);
}
