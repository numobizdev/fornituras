package mx.uumbal.solutions.palm_flow.modules.productores.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductorRepository extends JpaRepository<Productor, UUID> {

    @Query("""
            SELECT DISTINCT p FROM Productor p
            JOIN Predio pr ON pr.productor = p
            WHERE pr.centroAcopio.uuid IN :centroAcopioUuids
            """)
    Page<Productor> findDistinctByPredioCentroAcopioUuidIn(
            @Param("centroAcopioUuids") Collection<UUID> centroAcopioUuids, Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Productor p
            JOIN Predio pr ON pr.productor = p
            WHERE pr.centroAcopio.uuid = :centroAcopioUuid
            """)
    Page<Productor> findDistinctByPredioCentroAcopioUuid(
            @Param("centroAcopioUuid") UUID centroAcopioUuid, Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Productor p
            JOIN Predio pr ON pr.productor = p
            WHERE pr.centroAcopio.uuid = :centroAcopioUuid
              AND LOWER(p.nombre) = LOWER(:nombre)
            """)
    Optional<Productor> findByNombreIgnoreCaseAndPredioCentroAcopioUuid(
            @Param("nombre") String nombre, @Param("centroAcopioUuid") UUID centroAcopioUuid);
}
