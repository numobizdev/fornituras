package mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper;

import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.Region;
import org.springframework.stereotype.Component;

@Component
public class RegionMapperImpl implements RegionMapper {

    @Override
    public Region toEntity(RegionRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Region region = new Region();
        region.setNombre(dto.getNombre());
        return region;
    }

    @Override
    public RegionResponseDTO toResponseDTO(Region entity) {
        if (entity == null) {
            return null;
        }
        return RegionResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .build();
    }
}
