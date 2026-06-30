package mx.uumbal.solutions.palm_flow.modules.usuarios.repository;

import mx.uumbal.solutions.palm_flow.modules.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
