package mx.uumbal.solutions.palm_flow.modules.geografia.mapper;

import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;

public interface ComunidadMapper {

    ComunidadResponseDTO toResponseDTO(Comunidad entity);

    void updateEntity(Comunidad entity, ComunidadRequestDTO dto);
}
