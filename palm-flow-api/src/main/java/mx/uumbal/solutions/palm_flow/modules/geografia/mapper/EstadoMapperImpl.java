package mx.uumbal.solutions.palm_flow.modules.geografia.mapper;

import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.EstadoResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import org.springframework.stereotype.Component;

@Component
public class EstadoMapperImpl implements EstadoMapper {

    @Override
    public Estado toEntity(EstadoRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Estado estado = new Estado();
        estado.setNombre(dto.getNombre());
        return estado;
    }

    @Override
    public EstadoResponseDTO toResponseDTO(Estado entity) {
        if (entity == null) {
            return null;
        }
        return EstadoResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .build();
    }
}
