package mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioPrecioKg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CentroAcopioPrecioKgRepository extends JpaRepository<CentroAcopioPrecioKg, UUID> {

    Page<CentroAcopioPrecioKg> findByCentroAcopioUuid(UUID centroAcopioUuid, Pageable pageable);

    boolean existsByCentroAcopioUuidAndFechaVigencia(UUID centroAcopioUuid, LocalDate fechaVigencia);

    Optional<CentroAcopioPrecioKg> findFirstByCentroAcopioUuidAndFechaVigenciaLessThanEqualOrderByFechaVigenciaDesc(
            UUID centroAcopioUuid, LocalDate fecha);
}
