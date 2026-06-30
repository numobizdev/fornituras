package mx.uumbal.solutions.palm_flow.modules.geografia.mapper;

import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import org.springframework.stereotype.Component;

@Component
public class MunicipioMapperImpl implements MunicipioMapper {

    @Override
    public MunicipioResponseDTO toResponseDTO(Municipio entity) {
        if (entity == null) {
            return null;
        }
        return MunicipioResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .estadoId(entity.getEstado().getId())
                .estadoNombre(entity.getEstado().getNombre())
                .build();
    }

    @Override
    public void updateEntity(Municipio entity, MunicipioRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setNombre(dto.getNombre());
    }
}
