package mx.uumbal.solutions.palm_flow.modules.geografia.repository;

import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    Page<Municipio> findByEstadoId(Long estadoId, Pageable pageable);

    Optional<Municipio> findByNombreIgnoreCaseAndEstadoId(String nombre, Long estadoId);
}
