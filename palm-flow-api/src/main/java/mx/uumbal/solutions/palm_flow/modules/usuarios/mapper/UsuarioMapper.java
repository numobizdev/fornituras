package mx.uumbal.solutions.palm_flow.modules.usuarios.mapper;

import mx.uumbal.solutions.palm_flow.modules.usuarios.dto.UsuarioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.usuarios.entity.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponseDTO toResponseDTO(Usuario entity);
}
