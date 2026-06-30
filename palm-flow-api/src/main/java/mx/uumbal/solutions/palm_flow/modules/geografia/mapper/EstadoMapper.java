package mx.uumbal.solutions.palm_flow.modules.geografia.mapper;

import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;

public interface EstadoMapper {

    Estado toEntity(EstadoRequestDTO dto);

    EstadoResponseDTO toResponseDTO(Estado entity);
}
