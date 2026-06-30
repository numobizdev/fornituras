package mx.uumbal.solutions.palm_flow.modules.geografia.repository;

import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComunidadRepository extends JpaRepository<Comunidad, Long> {

    Page<Comunidad> findByMunicipioId(Long municipioId, Pageable pageable);

    Optional<Comunidad> findByNombreIgnoreCaseAndMunicipioId(String nombre, Long municipioId);

    Optional<Comunidad> findFirstByNombreIgnoreCase(String nombre);
}
