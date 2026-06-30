package mx.uumbal.solutions.palm_flow.modules.geografia.mapper;

import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.ComunidadResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import org.springframework.stereotype.Component;

@Component
public class ComunidadMapperImpl implements ComunidadMapper {

    @Override
    public ComunidadResponseDTO toResponseDTO(Comunidad entity) {
        if (entity == null) {
            return null;
        }
        return ComunidadResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .municipioId(entity.getMunicipio().getId())
                .municipioNombre(entity.getMunicipio().getNombre())
                .estadoId(entity.getMunicipio().getEstado().getId())
                .estadoNombre(entity.getMunicipio().getEstado().getNombre())
                .build();
    }

    @Override
    public void updateEntity(Comunidad entity, ComunidadRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setNombre(dto.getNombre());
    }
}
