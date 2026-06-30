package mx.uumbal.solutions.palm_flow.modules.geografia.repository;

import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {

    Optional<Estado> findByNombreIgnoreCase(String nombre);
}
