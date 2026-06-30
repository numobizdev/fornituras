package mx.uumbal.solutions.palm_flow.modules.empresas.repository;

import mx.uumbal.solutions.palm_flow.modules.empresas.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNombre(String nombre);
}
